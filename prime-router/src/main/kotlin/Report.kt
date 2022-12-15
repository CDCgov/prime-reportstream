package gov.cdc.prime.router

import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ElrResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.DateUtilities.toLocalDate
import gov.cdc.prime.router.common.DateUtilities.toOffsetDateTime
import gov.cdc.prime.router.common.DateUtilities.toYears
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.metadata.ElementAndValue
import gov.cdc.prime.router.metadata.Mappers
import org.apache.logging.log4j.kotlin.Logging
import tech.tablesaw.api.Row
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.columns.Column
import tech.tablesaw.selection.Selection
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.xml.bind.DatatypeConverter
import kotlin.random.Random

/**
 * Report id
 */
typealias ReportId = UUID

/**
 * Default values for elements to use when creating reports
 */
typealias DefaultValues = Map<String, String>

// the threshold for count of rows inside the report that we don't want
// to shuffle at. If there are less than this number of rows in the table
// then we just want to fake the data instead to prevent the leakage of PII
const val SHUFFLE_THRESHOLD = 25

// Basic size limitations on incoming reports
// Experiments show 10k HL7 Items is ~41Meg. So allow 50Meg
const val PAYLOAD_MAX_BYTES: Long = (50 * 1000 * 1000).toLong()
const val REPORT_MAX_ITEMS = 10000
const val REPORT_MAX_ITEM_COLUMNS = 2000
const val REPORT_MAX_ERRORS = 100

// constants used for parsing and processing a report message
const val ROUTE_TO_SEPARATOR = ","
const val DEFAULT_SEPARATOR = ":"

// options are used to process and route the report
enum class Options {
    None,
    ValidatePayload,
    CheckConnections,
    SkipSend,
    SendImmediately,

    @OptionDeprecated
    SkipInvalidItems;

    class InvalidOptionException(message: String) : Exception(message)

    /**
     * Checks to see if the enum constant has an @OptionDeprecated annotation.
     * If the annotation is present, the constant is no longer in use.
     */

    val isDeprecated = this.declaringClass.getField(this.name)
        .getAnnotation(OptionDeprecated::class.java) != null

    companion object {
        /**
         * ActiveValues is a list of the non-deprecated options that can be used when submitting a report
         */

        val activeValues = mutableListOf<Options>()

        init {
            Options.values().forEach {
                if (!it.isDeprecated) activeValues.add(it)
            }
        }

        /**
         * Handles invalid values, which are technically not allowed in an enum. In this case if the [input]
         *  is not one that is supported, it will be set to None.
         */
        fun valueOfOrNone(input: String): Options {
            return try {
                valueOf(input)
            } catch (ex: IllegalArgumentException) {
                val msg = "$input is not a valid Option. Valid options: ${Options.activeValues.joinToString()}"
                throw InvalidOptionException(msg)
            }
        }
    }
}

annotation class OptionDeprecated()

/**
 * ReportStreamFilterResult records useful information about rows filtered by one filter call.  One filter
 * might filter many rows. ReportStreamFilterResult entries are only created when filter logging is on.  This is to
 * prevent tons of junk logging of jurisdictionalFilters - the vast majority of which typically filter out everything.
 *
 * @property receiverName Then intended receiver for the report
 * @property originalCount The original number of items in the report
 * @property filterName The name of the filter function that removed the rows
 * @property filterArgs The arguments used in the filter function
 * @property filteredTrackingElement The trackingElement value of the rows removed.
 * Note that we can't guarantee the Sender is sending good unique trackingElement values.
 * Note that we are not tracking the index (aka rownum).  That's because the row numbers we get here are
 * not the ones in the data the user submitted -- because quality filtering is done after juris filtering,
 * which creates a new report with fewer rows.
 */
data class ReportStreamFilterResult(
    val receiverName: String,
    val originalCount: Int,
    val filterName: String,
    val filterArgs: List<String>,
    val filteredTrackingElement: String,
    val filterType: ReportStreamFilterType?
) : ActionLogDetail {
    override val scope = ActionLogScope.translation
    override val errorCode = ""

    companion object {
        // Use this value in logs and user-facing messages if the trackingElement is missing.
        const val DEFAULT_TRACKING_VALUE = "MissingID"
    }

    override val message = "For $receiverName, filter $filterName$filterArgs" +
        " filtered out item $filteredTrackingElement"

    // Used for deserializing to a JSON response
    override fun toString(): String {
        return message
    }
}

/**
 * The report represents the report from one agent-organization, and which is
 * translated and sent to another agent-organization. Each report has a schema,
 * unique id and name as well as list of sources for the creation of the report.
 */
class Report : Logging {
    enum class Format(
        val ext: String,
        val mimeType: String,
        val isSingleItemFormat: Boolean = false
    ) {
        INTERNAL("internal.csv", "text/csv"), // A format that serializes all elements of a Report.kt (in CSV)
        CSV("csv", "text/csv"), // A CSV format the follows the csvFields
        CSV_SINGLE("csv", "text/csv", true),
        HL7("hl7", "application/hl7-v2", true), // HL7 with one result per file
        HL7_BATCH("hl7", "application/hl7-v2"), // HL7 with BHS and FHS headers
        FHIR("fhir", "application/fhir+json");

        companion object {
            // Default to CSV if weird or unknown
            fun safeValueOf(formatStr: String?): Format {
                return try {
                    valueOf(formatStr ?: "CSV")
                } catch (e: IllegalArgumentException) {
                    CSV
                }
            }

            /**
             * Returns a Format based on the [ext] provided, ignoring case.
             */
            fun valueOfFromExt(ext: String): Format {
                return when (ext.lowercase()) {
                    HL7.ext.lowercase() -> HL7
                    FHIR.ext.lowercase() -> FHIR
                    CSV.ext.lowercase() -> CSV
                    else -> throw IllegalArgumentException("Unexpected extension $ext.")
                }
            }
        }
    }

    /**
     * the UUID for the report
     */
    val id: ReportId

    /**
     * The schema of the data in the report
     */
    val schema: Schema

    /**
     * The sources that generated this service
     * todo this is no longer being stored in the db. Its not clear what its useful for.
     */
    val sources: List<Source>

    /**
     * The intended destination service for this report
     */
    val destination: Receiver?

    /**
     * The list of info about data *removed* from this report by filtering.
     * The list has one entry per filter applied, *not* one entry per row removed.
     */
    val filteringResults: MutableList<ReportStreamFilterResult> = mutableListOf()

    /**
     * The time when the report was created
     */
    val createdDateTime: OffsetDateTime

    /**
     * The number of items in the report
     */
    val itemCount: Int

    /**
     * The number of items that passed the jurisdictionalFilter for this report, prior to
     * other filtering.  This is purely informational.   It is >= the actual number of items
     * in the report.  This is only useful for reports created by the routing step.
     * In all other cases, this value can be confusing, for example after batching has occurred.
     * So in all other cases, we set it to null.
     *
     * Example usage: if during filtering 10 items passed the juris filter for Kentucky, but then only 3 passed
     * the qualityFilter, the report created by routing (prior to batching)
     * would have [itemCountBeforeQualFilter] = 10 and [itemCount] = 3.
     */
    var itemCountBeforeQualFilter: Int? = null

    /**
     * The set of parent -> child lineage items associated with this report.
     * The items in *this* report are the *child* items.
     * There should be `itemCount` items in this List, or it should be null.
     * Implicit in that assumption is that each Item
     * within this report has only a single parent item.  If this assumption changes, we'll
     * need to make this into a more complex data structure.
     */
    var itemLineages: List<ItemLineage>? = null

    /**
     * A range of item index for this report
     */
    val itemIndices: IntRange get() = 0 until this.table.rowCount()

    /**
     * A standard name for this report that take schema, id, and destination into account
     */
    val name: String
        get() = formFilename(
            id,
            schema.baseName,
            bodyFormat,
            createdDateTime,
            translationConfig = destination?.translation,
            metadata = this.metadata
        )

    /**
     * A format for the body or use the destination format
     */
    val bodyFormat: Format

    /**
     * A pointer to where the Report is stored.
     */
    var bodyURL: String = ""

    /**
     * An indicator of what the nextAction on a report is, defaults to 'none'
     */
    var nextAction: TaskAction = TaskAction.none

    // The use of a TableSaw is an implementation detail hidden by this class
    // The TableSaw table is mutable, while this class is has immutable semantics
    //
    // Dev Note: TableSaw is not multi-platform, so it could be switched out in the future.
    // Don't let the TableSaw abstraction leak.
    //
    private val table: Table

    private val metadata: Metadata

    /**
     * Allows us to specify a synthesize strategy when converting a report from live data
     * into synthetic data that cannot be tied back to any real persons
     */
    enum class SynthesizeStrategy {
        BLANK,
        SHUFFLE,
        PASSTHROUGH,
        FAKE
    }

    // Generic
    constructor(
        schema: Schema,
        values: List<List<String>>,
        sources: List<Source>,
        destination: Receiver? = null,
        bodyFormat: Format? = null,
        itemLineage: List<ItemLineage>? = null,
        id: ReportId? = null, // If constructing from blob storage, must pass in its UUID here.  Otherwise, null.
        metadata: Metadata,
        itemCountBeforeQualFilter: Int? = null
    ) {
        this.id = id ?: UUID.randomUUID()
        this.schema = schema
        this.sources = sources
        this.createdDateTime = OffsetDateTime.now()
        this.destination = destination
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.itemLineages = itemLineage
        this.table = createTable(schema, values)
        this.itemCount = this.table.rowCount()
        this.metadata = metadata
        this.itemCountBeforeQualFilter = itemCountBeforeQualFilter
    }

    // Test source
    constructor(
        schema: Schema,
        values: List<List<String>>,
        source: TestSource,
        destination: Receiver? = null,
        bodyFormat: Format? = null,
        itemLineage: List<ItemLineage>? = null,
        metadata: Metadata? = null,
        itemCountBeforeQualFilter: Int? = null
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = listOf(source)
        this.destination = destination
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.itemLineages = itemLineage
        this.createdDateTime = OffsetDateTime.now()
        this.table = createTable(schema, values)
        this.itemCount = this.table.rowCount()
        this.metadata = metadata ?: Metadata.getInstance()
        this.itemCountBeforeQualFilter = itemCountBeforeQualFilter
    }

    constructor(
        schema: Schema,
        values: Map<String, List<String>>,
        source: Source,
        destination: Receiver? = null,
        bodyFormat: Format? = null,
        itemLineage: List<ItemLineage>? = null,
        metadata: Metadata,
        itemCountBeforeQualFilter: Int? = null
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = listOf(source)
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.destination = destination
        this.createdDateTime = OffsetDateTime.now()
        this.itemLineages = itemLineage
        this.table = createTable(values)
        this.itemCount = this.table.rowCount()
        this.metadata = metadata
        this.itemCountBeforeQualFilter = itemCountBeforeQualFilter
    }

    /**
     * Full ELR Report constructor for ingest
     * [bodyFormat] is the format for this report. Should be HL7
     * [sources] is the ClientSource or TestSource, where this data came from
     * [numberOfMessages] how many incoming messages does this Report represent
     * [metadata] is the metadata to use, mocked meta is passed in for testing
     * [itemLineage] itemlineages for this report to track parent/child reports
     */
    constructor(
        bodyFormat: Format,
        sources: List<Source>,
        numberOfMessages: Int,
        metadata: Metadata? = null,
        itemLineage: List<ItemLineage>? = null,
        destination: Receiver? = null,
        nextAction: TaskAction = TaskAction.process
    ) {
        this.id = UUID.randomUUID()
        // ELR submissions do not need a schema, but it is required by the database to maintain legacy functionality
        this.schema = Schema("None", Topic.FULL_ELR)
        this.sources = sources
        this.bodyFormat = bodyFormat
        this.destination = destination
        this.createdDateTime = OffsetDateTime.now()
        this.itemLineages = itemLineage
        // we do not need the 'table' representation in this instance
        this.table = createTable(emptyMap<String, List<String>>())
        this.itemCount = numberOfMessages
        this.metadata = metadata ?: Metadata.getInstance()
        this.itemCountBeforeQualFilter = numberOfMessages
        this.nextAction = nextAction
    }

    private constructor(
        schema: Schema,
        table: Table,
        sources: List<Source>,
        destination: Receiver? = null,
        bodyFormat: Format? = null,
        itemLineage: List<ItemLineage>? = null,
        metadata: Metadata? = null,
        itemCountBeforeQualFilter: Int? = null
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.table = table
        this.itemCount = this.table.rowCount()
        this.sources = sources
        this.destination = destination
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.itemLineages = itemLineage
        this.createdDateTime = OffsetDateTime.now()
        this.metadata = metadata ?: Metadata.getInstance()
        this.itemCountBeforeQualFilter = itemCountBeforeQualFilter
    }

    @Suppress("Destructure")
    private fun createTable(schema: Schema, values: List<List<String>>): Table {
        fun valuesToColumns(schema: Schema, values: List<List<String>>): List<Column<*>> {
            return schema.elements.mapIndexed { index, element ->
                StringColumn.create(element.name, values.map { it[index] })
            }
        }

        return Table.create("prime", valuesToColumns(schema, values))
    }

    private fun createTable(values: Map<String, List<String>>): Table {
        fun valuesToColumns(values: Map<String, List<String>>): List<Column<*>> {
            return values.keys.map {
                StringColumn.create(it, values[it])
            }
        }
        return Table.create("prime", valuesToColumns(values))
    }

    // todo remove this when we remove ReportSource
    private fun fromThisReport(action: String) = listOf(ReportSource(this.id, action))

    /**
     * Does a shallow copy of this report. Will have a new id and create date.
     * Copies the itemLineages and filteredItems as well.
     */
    fun copy(destination: Receiver? = null, bodyFormat: Format? = null): Report {
        // Dev Note: table is immutable, so no need to duplicate it
        val copy = Report(
            this.schema,
            this.table,
            fromThisReport("copy"),
            destination ?: this.destination,
            bodyFormat ?: this.bodyFormat,
            metadata = this.metadata,
            itemCountBeforeQualFilter = this.itemCountBeforeQualFilter
        )
        copy.itemLineages = createOneToOneItemLineages(this, copy)
        copy.filteringResults.addAll(this.filteringResults)
        return copy
    }

    /** Checks to see if the report is empty or not */
    fun isEmpty(): Boolean {
        return table.rowCount() == 0
    }

    /** Given a report object, returns the assigned time zone or the default */
    fun getTimeZoneForReport(): ZoneId {
        val hl7Config = this.destination?.translation as? Hl7Configuration
        return if (
            hl7Config?.convertDateTimesToReceiverLocalTime == true && this.destination?.timeZone != null
        ) {
            ZoneId.of(this.destination.timeZone.zoneId)
        } else {
            // default to UTC
            ZoneId.of("UTC")
        }
    }

    fun getString(row: Int, column: Int, maxLength: Int? = null): String? {
        return table.getString(row, column).let {
            if (maxLength == null || maxLength > it.length) {
                it
            } else {
                it.substring(0, maxLength)
            }
        }
    }

    fun getString(row: Int, colName: String, maxLength: Int? = null): String? {
        val column = schema.findElementColumn(colName) ?: return null
        return table.getString(row, column).let {
            if (maxLength == null || maxLength > it.length) {
                it
            } else {
                it.substring(0, maxLength)
            }
        }
    }

    fun getStringByHl7Field(row: Int, hl7Field: String, maxLength: Int? = null): String? {
        val column = schema.elements.firstOrNull { it.hl7Field.equals(hl7Field, ignoreCase = true) } ?: return null
        val index = schema.findElementColumn(column.name) ?: return null
        return table.getString(row, index).let {
            if (maxLength == null || maxLength > it.length) {
                it
            } else {
                it.substring(0, maxLength)
            }
        }
    }

    fun getRow(row: Int): List<String> {
        return schema.elements.map {
            val column = schema.findElementColumn(it.name)
                ?: error("Internal Error: column for '${it.name}' is not found")
            table.getString(row, column) ?: ""
        }
    }

    fun filter(
        filterFunctions: List<Pair<ReportStreamFilterDefinition, List<String>>>,
        receiver: Receiver,
        doLogging: Boolean,
        trackingElement: String?,
        reverseTheFilter: Boolean = false,
        reportStreamFilterType: ReportStreamFilterType
    ): Report {
        val filteredRows = mutableListOf<ReportStreamFilterResult>()
        val combinedSelection = Selection.withRange(0, table.rowCount())
        filterFunctions.forEach { (filterFn, fnArgs) ->
            val filterFnSelection = filterFn.getSelection(fnArgs, table, receiver, doLogging)
            // NOTE: It's odd that we have to do logic after the fact
            //       to figure out what the previous function did
            if (doLogging && filterFnSelection.size() < table.rowCount()) {
                val before = Selection.withRange(0, table.rowCount())
                val filteredRowList = before.andNot(filterFnSelection).toList()
                val rowsFiltered = getValuesInRows(
                    trackingElement,
                    filteredRowList,
                    ReportStreamFilterResult.DEFAULT_TRACKING_VALUE
                )
                rowsFiltered.forEach { trackingId ->
                    filteredRows.add(
                        ReportStreamFilterResult(
                            receiver.fullName,
                            table.rowCount(),
                            filterFn.name,
                            fnArgs,
                            trackingId,
                            reportStreamFilterType
                        )
                    )
                }
            }
            combinedSelection.and(filterFnSelection)
        }
        val finalCombinedSelection = if (reverseTheFilter) {
            Selection.withRange(0, table.rowCount()).andNot(combinedSelection)
        } else {
            combinedSelection
        }
        val filteredTable = table.where(finalCombinedSelection)
        val filteredReport = Report(
            this.schema,
            filteredTable,
            fromThisReport("filter: $filterFunctions"),
            metadata = this.metadata,
            // copy from previous filter; avoid losing info during filtering steps subsequent to quality filter.
            itemCountBeforeQualFilter = this.itemCountBeforeQualFilter
        )
        // Write same info to our logs that goes in the json response obj
        if (doLogging) {
            filteredRows.forEach { filterResult -> logger.info(filterResult.toString()) }
        }
        filteredReport.filteringResults.addAll(this.filteringResults) // copy ReportStreamFilterResults from prev
        filteredReport.filteringResults.addAll(filteredRows) // and add any new ReportStreamFilterResults just created.
        filteredReport.itemLineages = createItemLineages(finalCombinedSelection, this, filteredReport)
        return filteredReport
    }

    /**
     * Return the values in column [columnName] for these [rows].  If a [default] is specified,
     * that value is used for all cases where the value is empty/null/missing for that row in the table.
     * If [default] is null, this may return fewer rows than in [rows].
     *
     * @return an emptyList if the columnName is null (not an error)
     *
     */
    fun getValuesInRows(columnName: String?, rows: List<Int>, default: String? = null): List<String> {
        if (columnName.isNullOrEmpty()) return emptyList()
        val columnIndex = this.table.columnIndex(columnName)
        return rows.mapNotNull { row ->
            val value = this.table.getString(row, columnIndex)
            if (value.isNullOrEmpty()) {
                default // might be null
            } else {
                value
            }
        }
    }

    /**
     * Return Report with PII columns transformed to [replacementValue] when a value is sent. Blank values should
     * remain unchanged.
     */
    fun deidentify(replacementValue: String): Report {
        val columns = schema.elements.map {
            when {
                it.name == patient_zip_column_name -> buildRestrictedZipCode(it.name)
                it.name == patient_age_column_name -> buildDeidentifiedPatientAgeColumn(replacementValue)
                it.name == patient_dob_column_name -> buildDeidentifiedPatientDobColumn(replacementValue)
                it.pii == true -> {
                    table.column(it.name)
                        .asStringColumn()
                        .set(
                            table.column(it.name)
                                .isNotMissing,
                            replacementValue
                        )
                }

                else -> table.column(it.name).copy()
            }
        }
        return Report(
            schema,
            Table.create(columns),
            fromThisReport("deidentify"),
            itemLineage = this.itemLineages,
            metadata = this.metadata,
            itemCountBeforeQualFilter = this.itemCountBeforeQualFilter
        )
    }

    /**
     * Writes the [value] for the [columnName] for the [row].  If a [columnName] is not in the schema,
     * an error is thrown.
     * Any data in the field will be overwritten.
     */
    fun setString(row: Int, columnName: String, value: String) {
        val column = schema.findElementColumn(columnName) ?: error("Internal Error: '$columnName' is not found")
        table.stringColumn(column).set(row, value)
    }

    // takes the data in the existing report and synthesizes different data from it
    // the goal is to allow us to take real data in, move it around and scramble it, so it's
    // not able to point back to the actual records
    fun synthesizeData(
        synthesizeStrategies: Map<String, SynthesizeStrategy> = emptyMap(),
        targetState: String? = null,
        targetCounty: String? = null,
        metadata: Metadata
    ): Report {
        fun safeSetStringInRow(row: Row, columnName: String, value: String) {
            if (row.columnNames().contains(columnName)) {
                row.setString(columnName, value)
            }
        }

        val columns = schema.elements.map {
            val synthesizedColumn = synthesizeStrategies[it.name]?.let { strategy ->
                // we want to guard against the possibility that there are too few records
                // to reliably shuffle against. because shuffling is pseudo-random, it's possible that
                // with something below a threshold we could end up leaking PII, therefore
                // ignore the call to shuffle and just fake it
                val synthesizeStrategy = if (itemCount < SHUFFLE_THRESHOLD && strategy == SynthesizeStrategy.SHUFFLE) {
                    SynthesizeStrategy.FAKE
                } else {
                    strategy
                }
                // look in the mapping parameter passed in for the current element
                when (synthesizeStrategy) {
                    // examine the synthesizeStrategy for the field
                    // can be one of three values right now:
                    // empty column, shuffle column, pass through column untouched
                    SynthesizeStrategy.SHUFFLE -> {
                        // if the field is date of birth, then we can break it apart and make
                        // a pseudo-random date
                        val shuffledValues = if (it.name == "patient_dob") {
                            // shuffle all the DOBs
                            val dobs = table.column(it.name).asStringColumn().shuffled().map { dob ->
                                // parse the date
                                val parsedDate = LocalDate.parse(
                                    dob.ifEmpty {
                                        LocalDate.now().format(
                                            DateTimeFormatter.ofPattern(DateUtilities.datePattern)
                                        )
                                    },
                                    DateTimeFormatter.ofPattern(DateUtilities.datePattern)
                                )
                                // get the year and date
                                val year = parsedDate.year
                                // fake a month
                                val month = Random.nextInt(1, 12)
                                val day = Random.nextInt(1, 28)
                                // return with a different month and day
                                DateUtilities.dateFormatter.format(LocalDate.of(year, month, day))
                            }
                            // return our list of days
                            dobs
                        } else {
                            // return the string column shuffled
                            table.column(it.name).asStringColumn().shuffled()
                        }
                        StringColumn.create(it.name, shuffledValues)
                    }

                    SynthesizeStrategy.FAKE -> {
                        // generate random faked data for the column passed in
                        buildFakedColumn(it.name, it, targetState, targetCounty, metadata)
                    }

                    SynthesizeStrategy.BLANK -> buildEmptyColumn(it.name)
                    SynthesizeStrategy.PASSTHROUGH -> table.column(it.name).copy()
                }
            }
            // if the element name is not mapping, it is handled as a pass through
            synthesizedColumn ?: table.column(it.name).copy()
        }
        val table = Table.create(columns)
        // unfortunate fact for how we do faking of rows, the four columns below
        // would never match because the row context was new on each write of the
        // column. because we synthesize the data here, we need to actually overwrite the
        // values in each row because quality synthetic data matters
        table.forEach {
            val context = FakeReport.RowContext(
                metadata,
                targetState,
                schema.name,
                targetCounty
            )
            safeSetStringInRow(it, "patient_county", context.county)
            safeSetStringInRow(it, "patient_city", context.city)
            safeSetStringInRow(it, "patient_state", context.state)
            safeSetStringInRow(it, "patient_zip_code", context.zipCode)
        }
        // return the new copy of the report here
        return Report(schema, table, fromThisReport("synthesizeData"), metadata = this.metadata)
    }

    /**
     * Create a separate report for each item in the report
     */
    fun split(): List<Report> {
        return itemIndices.map {
            val row = getRow(it)
            val oneItemReport = Report(
                schema = schema,
                values = listOf(row),
                sources = fromThisReport("split"),
                destination = destination,
                bodyFormat = bodyFormat,
                metadata = this.metadata
            )
            oneItemReport.itemLineages =
                listOf(createItemLineageForRow(this, it, oneItemReport, 0))
            oneItemReport
        }
    }

    /**
     * Here 'mapping' means to transform data from the current schema to a new schema per the rules in the [mapping].
     * Not to be confused with our lower level [Mapper] concept.
     */
    fun applyMapping(mapping: Translator.Mapping): Report {
        val pass1Columns = mapping.toSchema.elements.map { element -> buildColumnPass1(mapping, element) }
        val pass2Columns = mapping.toSchema.elements.map { element -> buildColumnPass2(mapping, element, pass1Columns) }
        val newTable = Table.create(pass2Columns)
        return Report(
            mapping.toSchema,
            newTable,
            fromThisReport("mapping"),
            itemLineage = itemLineages,
            metadata = this.metadata,
            itemCountBeforeQualFilter = this.itemCountBeforeQualFilter
        )
    }

    /**
     * This method takes the contents of a report and maps them a [ElrResultMetadata] object that is ready
     * to be persisted to the database. This is not PII nor PHI, so it is safe to collect and build trend
     * analysis off of.
     */
    fun getDeidentifiedResultMetaData(): List<ElrResultMetadata> {
        return try {
            table.mapIndexed { idx, row ->
                ElrResultMetadata().also {
                    it.messageId = row.getStringOrNull("message_id")
                    it.previousMessageId = row.getStringOrNull("previous_message_id")
                    it.topic = row.getStringOrNull("topic")
                    it.reportId = this.id
                    // switched to 1-based index on items in Feb 2022
                    it.reportIndex = idx + 1
                    // For sender ID, use first the provided ID and if not use the client ID.
                    it.senderId = row.getStringOrNull("sender_id")
                    if (it.senderId.isNullOrBlank()) {
                        val clientSource = sources.firstOrNull { source -> source is ClientSource } as ClientSource?
                        if (clientSource != null) it.senderId = clientSource.name.trimToNull()
                    }
                    it.organizationName = row.getStringOrNull("organization_name")

                    it.sendingApplicationId = row.getStringOrNull("sending_application_namespace_id")
                    it.sendingApplicationName = row.getStringOrNull("sending_application_universal_id")

                    it.orderingProviderName =
                        row.getStringOrNull("ordering_provider_first_name") +
                        " " + row.getStringOrNull("ordering_provider_last_name")
                    it.orderingProviderId = row.getStringOrNull("ordering_provider_id")
                    it.orderingProviderCity = row.getStringOrNull("ordering_provider_city")
                    it.orderingProviderState = row.getStringOrNull("ordering_provider_state")
                    it.orderingProviderPostalCode = row.getStringOrNull("ordering_provider_zip_code")
                    it.orderingProviderCounty = row.getStringOrNull("ordering_provider_county")

                    it.orderingFacilityId = row.getStringOrNull("ordering_facility_id")
                    it.orderingFacilityCity = row.getStringOrNull("ordering_facility_city")
                    it.orderingFacilityCounty = row.getStringOrNull("ordering_facility_county")
                    it.orderingFacilityName = row.getStringOrNull("ordering_facility_name")
                    it.orderingFacilityPostalCode = row.getStringOrNull("ordering_facility_zip_code")
                    it.orderingFacilityState = row.getStringOrNull("ordering_facility_state")

                    it.testingFacilityCity = row.getStringOrNull("testing_lab_city")
                    it.testingFacilityId = row.getStringOrNull("testing_lab_id")
                    it.testingFacilityCounty = row.getStringOrNull("testing_lab_county")
                    it.testingFacilityName = row.getStringOrNull("testing_lab_name")
                    it.testingFacilityPostalCode = row.getStringOrNull("testing_lab_zip_code")
                    it.testingFacilityState = row.getStringOrNull("testing_lab_state")

                    it.patientCounty = row.getStringOrNull("patient_county")
                    it.patientCountry = row.getStringOrNull("patient_country")
                    it.patientEthnicityCode = row.getStringOrNull("patient_ethnicity")
                    it.patientEthnicity = if (it.patientEthnicityCode != null) {
                        metadata.findValueSet("hl70189")?.toDisplayFromCode(it.patientEthnicityCode)
                    } else {
                        null
                    }
                    it.patientGenderCode = row.getStringOrNull("patient_gender")
                    it.patientGender = if (it.patientGenderCode != null) {
                        metadata.findValueSet("hl70001")?.toDisplayFromCode(it.patientGenderCode)
                    } else {
                        null
                    }
                    it.patientPostalCode = row.getStringOrNull("patient_zip_code")
                    it.patientRaceCode = row.getStringOrNull("patient_race")
                    it.patientRace = if (it.patientRaceCode != null) {
                        metadata.findValueSet("hl70005")?.toDisplayFromCode(it.patientRaceCode)
                    } else {
                        null
                    }
                    it.patientState = row.getStringOrNull("patient_state")
                    it.patientTribalCitizenship = row.getStringOrNull("patient_tribal_citizenship")
                    it.patientTribalCitizenshipCode = row.getStringOrNull("patient_tribal_citizenship_code")
                    it.patientPreferredLanguage = row.getStringOrNull("patient_preferred_language")
                    it.patientNationality = row.getStringOrNull("patient_nationality")
                    it.patientSpecies = row.getStringOrNull("patient_species")
                    it.patientSpeciesCode = row.getStringOrNull("patient_species_code")

                    it.reasonForStudy = row.getStringOrNull("reason_for_study_text")
                    it.reasonForStudyCode = row.getStringOrNull("reason_for_study_id")

                    it.testResultCode = row.getStringOrNull("test_result_id")
                    it.testResult = row.getStringOrNull("test_result_text")
                    it.testResultNormalized = if (it.testResultCode != null) {
                        metadata.findValueSet("monkeypox/test_result")?.toDisplayFromCode(it.testResultCode)
                    } else {
                        null
                    }
                    it.equipmentModel = row.getStringOrNull("equipment_model_name")
                    it.specimenCollectionDateTime = row.getStringOrNull("specimen_collection_date_time").let { dt ->
                        if (!dt.isNullOrEmpty()) {
                            try {
                                DateUtilities.parseDate(dt).toOffsetDateTime()
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    it.patientAge = getAge(
                        row.getStringOrNull("patient_age"),
                        row.getStringOrNull("patient_dob"),
                        it.specimenCollectionDateTime
                    )
                    it.specimenReceivedDateTime = row.getStringOrNull(
                        "testing_lab_specimen_received_datetime"
                    ).let { dt ->
                        if (!dt.isNullOrEmpty()) {
                            try {
                                DateUtilities.parseDate(dt).toOffsetDateTime()
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    it.specimenCollectionMethod = row.getStringOrNull("specimen_collection_method_text")
                    it.specimenCollectionMethodCode = row.getStringOrNull("specimen_collection_method_code")
                    it.specimenCollectionSite = row.getStringOrNull("specimen_collection_site_text")
                    it.specimenCollectionSiteCode = row.getStringOrNull("specimen_collection_site_code")
                    it.specimenType = row.getStringOrNull("specimen_type_name")
                    it.specimenTypeCode = row.getStringOrNull("specimen_type_code")
                    it.specimenTypeNormalized = if (it.specimenTypeCode != null) {
                        metadata.findValueSet("monkeypox/specimen_type")?.toDisplayFromCode(it.specimenTypeCode)
                    } else {
                        null
                    }
                    it.specimenSourceSite = row.getStringOrNull("specimen_source_site_text")
                    it.specimenSourceSiteCode = row.getStringOrNull("specimen_source_site_code")

                    it.siteOfCare = row.getStringOrNull("site_of_care")
                    it.testKitNameId = row.getStringOrNull("test_kit_name_id")
                    it.testPerformedCode = row.getStringOrNull("test_performed_code")
                    it.testPerformed = row.getStringOrNull("test_performed_name")
                    it.testPerformedNormalized = if (it.testPerformedCode != null) {
                        metadata.findValueSet("monkeypox/test_code")?.toDisplayFromCode(it.testPerformedCode)
                    } else {
                        null
                    }
                    it.testPerformedLongName = if (it.testPerformedCode != null) {
                        metadata.findValueSet("monkeypox/test_long_name")?.toDisplayFromCode(it.testPerformedCode)
                    } else {
                        null
                    }
                    it.testOrdered = row.getStringOrNull("ordered_test_name")
                    it.testOrderedCode = row.getStringOrNull("ordered_test_code")
                    it.testOrderedNormalized = if (it.testOrderedCode != null) {
                        metadata.findValueSet("monkeypox/test_code")?.toDisplayFromCode(it.testOrderedCode)
                    } else {
                        null
                    }
                    it.testOrderedLongName = if (it.testOrderedCode != null) {
                        metadata.findValueSet("monkeypox/test_long_name")?.toDisplayFromCode(it.testOrderedCode)
                    } else {
                        null
                    }
                    // trap the processing mode code as well
                    it.processingModeCode = row.getStringOrNull("processing_mode_code")
                }
            }
        } catch (e: Exception) {
            logger.error(e)
            emptyList()
        }
    }

    fun getDeidentifiedCovidResults(): List<CovidResultMetadata> {
        return try {
            table.mapIndexed() { idx, row ->
                CovidResultMetadata().also {
                    it.messageId = row.getStringOrNull("message_id")
                    it.previousMessageId = row.getStringOrNull("previous_message_id")
                    it.orderingProviderName =
                        row.getStringOrNull("ordering_provider_first_name") +
                        " " + row.getStringOrNull("ordering_provider_last_name")
                    it.orderingProviderId = row.getStringOrNull("ordering_provider_id")
                    it.orderingProviderState = row.getStringOrNull("ordering_provider_state")
                    it.orderingProviderPostalCode = row.getStringOrNull("ordering_provider_zip_code")
                    it.orderingProviderCounty = row.getStringOrNull("ordering_provider_county")
                    it.orderingFacilityCity = row.getStringOrNull("ordering_facility_city")
                    it.orderingFacilityCounty = row.getStringOrNull("ordering_facility_county")
                    it.orderingFacilityName = row.getStringOrNull("ordering_facility_name")
                    it.orderingFacilityPostalCode = row.getStringOrNull("ordering_facility_zip_code")
                    it.orderingFacilityState = row.getStringOrNull("ordering_facility_state")
                    it.testingLabCity = row.getStringOrNull("testing_lab_city")
                    it.testingLabClia = row.getStringOrNull("testing_lab_clia")
                    it.testingLabCounty = row.getStringOrNull("testing_lab_county")
                    it.testingLabName = row.getStringOrNull("testing_lab_name")
                    it.testingLabPostalCode = row.getStringOrNull("testing_lab_zip_code")
                    it.testingLabState = row.getStringOrNull("testing_lab_state")
                    it.patientCounty = row.getStringOrNull("patient_county")
                    it.patientCountry = row.getStringOrNull("patient_country")
                    it.patientEthnicityCode = row.getStringOrNull("patient_ethnicity")
                    it.patientEthnicity = if (it.patientEthnicityCode != null) {
                        metadata.findValueSet("hl70189")?.toDisplayFromCode(it.patientEthnicityCode)
                    } else {
                        null
                    }
                    it.patientGenderCode = row.getStringOrNull("patient_gender")
                    it.patientGender = if (it.patientGenderCode != null) {
                        metadata.findValueSet("hl70001")?.toDisplayFromCode(it.patientGenderCode)
                    } else {
                        null
                    }
                    it.patientPostalCode = row.getStringOrNull("patient_zip_code")
                    it.patientRaceCode = row.getStringOrNull("patient_race")
                    it.patientRace = if (it.patientRaceCode != null) {
                        metadata.findValueSet("hl70005")?.toDisplayFromCode(it.patientRaceCode)
                    } else {
                        null
                    }
                    it.patientState = row.getStringOrNull("patient_state")
                    it.testResultCode = row.getStringOrNull("test_result")
                    it.testResult = if (it.testResultCode != null) {
                        metadata.findValueSet("covid-19/test_result")?.toDisplayFromCode(it.testResultCode)
                    } else {
                        null
                    }
                    it.equipmentModel = row.getStringOrNull("equipment_model_name")
                    it.specimenCollectionDateTime = row.getStringOrNull("specimen_collection_date_time").let { dt ->
                        if (!dt.isNullOrEmpty()) {
                            try {
                                LocalDate.from(DateUtilities.parseDate(dt))
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    it.patientAge = getAge(
                        row.getStringOrNull("patient_age"),
                        row.getStringOrNull("patient_dob"),
                        it.specimenCollectionDateTime?.toOffsetDateTime()
                    )
                    it.siteOfCare = row.getStringOrNull("site_of_care")
                    it.reportId = this.id
                    // switched to 1-based index on items in Feb 2022
                    it.reportIndex = idx + 1
                    // For sender ID, use first the provided ID and if not use the client ID.
                    it.senderId = row.getStringOrNull("sender_id")
                    if (it.senderId.isNullOrBlank()) {
                        val clientSource = sources.firstOrNull { source -> source is ClientSource } as ClientSource?
                        if (clientSource != null) it.senderId = clientSource.name.trimToNull()
                    }
                    it.testKitNameId = row.getStringOrNull("test_kit_name_id")
                    it.testPerformedLoincCode = row.getStringOrNull("test_performed_code")
                    it.organizationName = row.getStringOrNull("organization_name")
                    // trap the processing mode code from submissions as well
                    it.processingModeCode = row.getStringOrNull("processing_mode_code")
                }
            }
        } catch (e: Exception) {
            logger.error(e)
            emptyList()
        }
    }

    /**
     * getAge - calculate the age of the patient according to the criteria below:
     *      if patient_age is given then
     *          - validate it is not null, it is valid digit number, and not lesser than zero
     *      else
     *          - the patient will be calculated using period between patient date of birth and
     *          the specimen collection date.
     *  @param patientAge - input patient's age.
     *  @param patientDob - input patient date of birth.
     *  @param specimenCollectionDate - input date of when specimen was collected.
     *  @return age - result of patient's age.
     */
    private fun getAge(patientAge: String?, patientDob: String?, specimenCollectionDate: OffsetDateTime?): String? {
        return if (
            (!patientAge.isNullOrBlank()) &&
            patientAge.all { Character.isDigit(it) } &&
            (patientAge.toInt() > 0)
        ) {
            patientAge
        } else {
            //
            // Here, we got invalid or blank patient_age given to us.  Therefore, we will use patient date
            // of birth and date of specimen collected to calculate the patient's age.
            //
            try {
                if (patientDob == null || specimenCollectionDate == null) return null
                val d = DateUtilities.parseDate(patientDob).toOffsetDateTime()
                if (d.isBefore(specimenCollectionDate)) {
                    Duration.between(d, specimenCollectionDate).toYears().toString()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Builds the column in a first pass based on the translator mapping
     * @param mapping - the mapping for the translation
     * @param toElement - the element to write to
     * @return a [StringColumn] based on the mapping
     */
    private fun buildColumnPass1(mapping: Translator.Mapping, toElement: Element): StringColumn? {
        return when (toElement.name) {
            in mapping.useDirectly -> {
                table.stringColumn(mapping.useDirectly[toElement.name]).copy().setName(toElement.name)
            }

            in mapping.useMapper -> {
                null
            }

            in mapping.useDefault -> {
                val defaultValue = mapping.useDefault[toElement.name]
                val defaultValues = Array(table.rowCount()) { defaultValue }
                StringColumn.create(toElement.name, defaultValues.asList())
            }

            else -> {
                buildEmptyColumn(toElement.name)
            }
        }
    }

    /**
     * The second pass runs any mappers as needed for the given [toElement] and the [pass1Columns] data from the first
     * pass.  Mappers will use data from either schema as inputs.
     * @param mapping the mapping
     * @return the mapped column
     */
    private fun buildColumnPass2(
        mapping: Translator.Mapping,
        toElement: Element,
        pass1Columns: List<StringColumn?>
    ): StringColumn {
        val toSchema = mapping.toSchema
        val fromSchema = mapping.fromSchema
        val elementIndex = mapping.toSchema.findElementColumn(toElement.name)
        val values = Array(table.rowCount()) { row ->
            var elementValue = elementIndex?.let { pass1Columns[elementIndex]?.get(row) ?: "" } ?: ""
            if (toElement.useMapper(elementValue)) {
                val mapper = mapping.useMapper[toElement.name]!!
                val (_, args) = Mappers.parseMapperField(
                    toElement.mapper
                        ?: error("'${toElement.mapper}' mapper is missing")
                )
                // Mapper input values can come from either schema
                val inputValues = mapper.valueNames(toElement, args).mapNotNull { argName ->
                    val element = toSchema.findElement(argName)
                        ?: fromSchema.findElement(argName)
                        ?: return@mapNotNull null
                    var value = toSchema.findElementColumn(argName)?.let {
                        val column = pass1Columns[it] ?: return@let null
                        column.get(row)
                    }
                    if (value == null && fromSchema.containsElement(argName)) {
                        value = table.getString(row, argName)
                    }
                    if (value == null || value.isBlank()) return@mapNotNull null
                    ElementAndValue(element, value)
                }
                elementValue = mapper.apply(toElement, args, inputValues).value ?: ""
            }
            if (toElement.useDefault(elementValue)) elementValue = mapping.useDefault[toElement.name] ?: ""
            elementValue
        }
        return StringColumn.create(toElement.name, values.asList())
    }

    private fun buildEmptyColumn(name: String): StringColumn {
        return StringColumn.create(name, List(itemCount) { "" })
    }

    /**
     * Given a column name, this function walks through each value and if the value in that
     * column matches a restricted postal code, it will replace it with the appropriate value
     * per the HIPAA Safe Harbor rules
     * @param name The name of the column to examine
     */
    private fun buildRestrictedZipCode(name: String): StringColumn {
        val restrictedZip = metadata.findLookupTable("restricted_zip_code")

        table.column(name).forEachIndexed { idx, columnValue ->
            // Assuming zip format is xxxxx-yyyy
            val zipCode = columnValue.toString().split("-")
            val value = zipCode[0].dropLast(2)
            if (restrictedZip?.dataRows?.contains(listOf(value)) == true) {
                setString(idx, name, "00000")
            } else {
                setString(idx, name, (value + "00"))
            }
        }
        return table.column(name).copy() as StringColumn
    }

    /**
     * Walks the table rows and compares the patient age and if it is greater than or
     * equal to the comparison value, it will zero it out, otherwise it will pass it through
     * unchanged.
     */
    private fun buildDeidentifiedPatientAgeColumn(nullValuePlaceholder: String = ""): StringColumn {
        // loop through the table rows
        table.forEachIndexed { idx, row ->
            // get the specimen collection date
            val specimenCollectionDateTime = row
                // get the specimen collection date time
                .getStringOrNull(specimen_collection_date_column_name)
                .let {
                    when (it) {
                        // if the value is not null, parse it to a date value, otherwise, use current
                        // date time value to compare DOB against
                        null -> DateUtilities.nowAtZone(DateUtilities.utcZone).toOffsetDateTime(DateUtilities.utcZone)
                        else -> DateUtilities.parseDate(it).toOffsetDateTime(DateUtilities.utcZone)
                    }
                }
            // get the patient age
            val patientAge = getAge(
                row.getStringOrNull(patient_age_column_name),
                row.getStringOrNull(patient_dob_column_name),
                specimenCollectionDateTime
            )?.toIntOrNull().let {
                // if the patient age is greater than or equal to 89 years old, set it to zero
                if (it != null && it >= SAFE_HARBOR_AGE_CUTOFF) {
                    0
                } else {
                    it
                }
            }
            // set the patient age value
            setString(idx, patient_age_column_name, patientAge?.toString() ?: nullValuePlaceholder)
        }

        return table.column(patient_age_column_name).copy() as StringColumn
    }

    /**
     * Walks through the patient DOB looking at each year and doing a comparison against
     * [SAFE_HARBOR_DOB_YEAR_REPLACEMENT]. If the age is greater than or equal to that cutoff
     * value then it replaces it with [SAFE_HARBOR_DOB_YEAR_REPLACEMENT], otherwise, the DOB
     * is deidentified by replacing it with the birth year, so someone born 12/1/2000 would have
     * their DOB replaced with 2000, while someone born in 1927 would have their DOB replaced
     * with 0000.
     * @param nullValuePlaceholder - The value to replace null with
     * @returns a [StringColumn] of the deidentified values
     */
    private fun buildDeidentifiedPatientDobColumn(nullValuePlaceholder: String = ""): StringColumn {
        table.forEachIndexed() { idx, row ->
            val patientDob = row.getStringOrNull(patient_dob_column_name)
            if (patientDob == null) {
                setString(idx, patient_dob_column_name, nullValuePlaceholder)
            } else {
                val patientDobYear = DateUtilities.parseDate(patientDob).toLocalDate().year
                if (patientDobYear <= SAFE_HARBOR_CUTOFF_YEAR) {
                    setString(idx, patient_dob_column_name, SAFE_HARBOR_DOB_YEAR_REPLACEMENT)
                } else {
                    setString(idx, patient_dob_column_name, patientDobYear.toString())
                }
            }
        }
        return table.column(patient_dob_column_name).copy() as StringColumn
    }

    private fun buildFakedColumn(
        name: String,
        element: Element,
        targetState: String?,
        targetCounty: String?,
        metadata: Metadata
    ): StringColumn {
        val fakeDataService = FakeDataService()
        return StringColumn.create(
            name,
            List(itemCount) {
                val context = FakeReport.RowContext(metadata, targetState, schema.name, targetCounty)
                fakeDataService.getFakeValueForElement(element, context)
            }
        )
    }

    /**
     * Gets the item hash for a the [rowNum] of the report.
     * @return the ByteArray hash.
     */
    fun getItemHashForRow(rowNum: Int): String {
        // calculate and store item hash for deduplication purposes for the generated item
        val row = this.table.row(rowNum)
        var rawStr = ""
        for (colNum in 0 until row.columnCount()) {
            rawStr += row.getString(colNum)
        }

        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(rawStr.toByteArray())

        return DatatypeConverter.printHexBinary(digest).uppercase()
    }

    /**
     * Static functions for use in modifying and manipulating reports.
     */
    companion object {
        private const val patient_dob_column_name = "patient_dob"
        private const val patient_age_column_name = "patient_age"
        private const val patient_zip_column_name = "patient_zip_code"
        private const val specimen_collection_date_column_name = "specimen_collection_date_time"
        private const val SAFE_HARBOR_CUTOFF_YEAR = 1933
        private const val SAFE_HARBOR_DOB_YEAR_REPLACEMENT = "0000"
        private const val SAFE_HARBOR_AGE_CUTOFF = 89

        fun merge(inputs: List<Report>): Report {
            if (inputs.isEmpty()) {
                error("Cannot merge an empty report list")
            }
            if (inputs.size == 1) {
                return inputs[0]
            }
            if (!inputs.all { it.destination == inputs[0].destination }) {
                error("Cannot merge reports with different destinations")
            }
            if (!inputs.all { it.bodyFormat == inputs[0].bodyFormat }) {
                error("Cannot merge reports with different bodyFormats")
            }

            val head = inputs[0]
            val tail = inputs.subList(1, inputs.size)

            // Check schema
            val schema = head.schema
            tail.find { it.schema != schema }?.let { error("${it.schema.name} does not match the rest of the merge") }

            // Build table
            val newTable = head.table.copy()
            tail.forEach {
                newTable.append(it.table)
            }

            // Build sources
            val sources = inputs.map { ReportSource(it.id, "merge") }
            val mergedReport =
                Report(
                    schema,
                    newTable,
                    sources,
                    destination = head.destination,
                    bodyFormat = head.bodyFormat,
                    metadata = head.metadata
                )
            mergedReport.itemLineages = createItemLineages(inputs, mergedReport)
            return mergedReport
        }

        private fun createItemLineages(parentReports: List<Report>, childReport: Report): List<ItemLineage> {
            var childRowNum = 0
            val itemLineages = mutableListOf<ItemLineage>()
            parentReports.forEach { parentReport ->
                parentReport.itemIndices.forEach {
                    itemLineages.add(createItemLineageForRow(parentReport, it, childReport, childRowNum))
                    childRowNum++
                }
            }
            return itemLineages
        }

        /**
         * Use a tablesaw Selection bitmap to create a mapping from this report items to newReport items.
         * Note: A tablesaw Selection is just an array of the row indexes in the oldReport that meet the filter criteria
         */
        fun createItemLineages(selection: Selection, parentReport: Report, childReport: Report): List<ItemLineage> {
            return selection.mapIndexed { childRowNum, parentRowNum ->
                createItemLineageForRow(parentReport, parentRowNum, childReport, childRowNum)
            }.toList()
        }

        fun createOneToOneItemLineages(parentReport: Report, childReport: Report): List<ItemLineage> {
            if (parentReport.itemCount != childReport.itemCount) {
                error("Reports must have same number of items: ${parentReport.id}, ${childReport.id}")
            }
            if (parentReport.itemLineages != null && parentReport.itemLineages!!.size != parentReport.itemCount) {
                // good place for a simple sanity check.  OK to have no itemLineage, but if you do have it,
                // it must be complete.
                error(
                    "Report ${parentReport.id} should have ${parentReport.itemCount} lineage items" +
                        " but instead has ${parentReport.itemLineages!!.size} lineage items"
                )
            }
            return parentReport.itemIndices.map { i ->
                createItemLineageForRow(parentReport, i, childReport, i)
            }.toList()
        }

        /**
         * This is designed to survive any complicated dicing and slicing of Items that Rick can come up with.
         */
        fun createItemLineageForRow(
            parentReport: Report,
            parentRowNum: Int,
            childReport: Report,
            childRowNum: Int
        ): ItemLineage {
            // get the item hash to store for deduplication purposes. If a hash has already been generated
            //  for a row, use that hash to represent the row itself, since translations will result in different
            //  hash values
            val itemHash = if (parentReport.itemLineages != null && parentReport.itemLineages!!.isNotEmpty()) {
                parentReport.itemLineages!![parentRowNum].itemHash
            } else {
                parentReport.getItemHashForRow(parentRowNum)
            }

            // Row numbers start at 0, but index need to start at 1
            val childIndex = childRowNum + 1
            val parentIndex = parentRowNum + 1

            // ok if this is null.
            if (parentReport.itemLineages != null) {
                // Avoid losing history.
                // If the parent report already had lineage, then pass its sins down to the next generation.
                val grandParentReportId = parentReport.itemLineages!![parentRowNum].parentReportId
                val grandParentIndex = parentReport.itemLineages!![parentRowNum].parentIndex
                val grandParentTrackingValue = parentReport.itemLineages!![parentRowNum].trackingId
                return ItemLineage(
                    null,
                    grandParentReportId,
                    grandParentIndex,
                    childReport.id,
                    childIndex,
                    grandParentTrackingValue,
                    null,
                    null,
                    itemHash
                )
            } else {
                val trackingElementValue =
                    parentReport.getString(parentRowNum, parentReport.schema.trackingElement ?: "")
                return ItemLineage(
                    null,
                    parentReport.id,
                    parentIndex,
                    childReport.id,
                    childIndex,
                    trackingElementValue,
                    null,
                    null,
                    itemHash
                )
            }
        }

        /**
         * Create a 1:1 parent_child lineage mapping, using data taken from the
         * grandparent:parent lineage pulled from the database.
         * This is needed in cases where an Action doesn't have the actual Report data in mem. (examples: Send,Download)
         * In those cases, to populate the lineage, we can grab needed fields from previous lineage rows.
         */
        fun createItemLineagesFromDb(
            prevHeader: WorkflowEngine.Header,
            newChildReportId: ReportId
        ): List<ItemLineage>? {
            if (prevHeader.itemLineages == null) return null
            val newLineages = mutableMapOf<Int, ItemLineage>()
            prevHeader.itemLineages.forEach {
                if (it.childReportId != prevHeader.reportFile.reportId) {
                    return@forEach
                }
                newLineages[it.childIndex] =
                    ItemLineage(
                        null,
                        it.childReportId, // the prev child is the new parent
                        it.childIndex,
                        newChildReportId,
                        it.childIndex, // one-to-one mapping
                        it.trackingId,
                        it.transportResult,
                        null,
                        it.itemHash
                    )
            }
            val retval = mutableListOf<ItemLineage>()
            // Note indices start at 1
            for (index in 1..prevHeader.reportFile.itemCount) {
                retval.add(
                    newLineages[index] ?: error(
                        "Unable to create parent->child lineage " +
                            "${prevHeader.reportFile.reportId} -> $newChildReportId: missing lineage $index"
                    )
                )
            }
            return retval
        }

        fun decorateItemLineagesWithTransportResults(itemLineages: List<ItemLineage>, transportResults: List<String>) {
            if (itemLineages.size != transportResults.size) {
                error(
                    "To include transport_results in item_lineages, must have 1:1." +
                        "  Instead have ${transportResults.size} and  ${itemLineages.size} resp."
                )
            }
            itemLineages.forEachIndexed { index, itemLineage ->
                itemLineage.transportResult = transportResults[index]
            }
        }

        fun formFilename(
            id: ReportId,
            schemaName: String,
            fileFormat: Format?,
            createdDateTime: OffsetDateTime,
            translationConfig: TranslatorConfiguration? = null,
            metadata: Metadata
        ): String {
            return formFilename(
                id,
                schemaName,
                fileFormat,
                createdDateTime,
                translationConfig?.nameFormat ?: "standard",
                translationConfig,
                metadata = metadata
            )
        }

        fun formFilename(
            id: ReportId,
            schemaName: String,
            fileFormat: Format?,
            createdDateTime: OffsetDateTime,
            nameFormat: String = "standard",
            translationConfig: TranslatorConfiguration? = null,
            metadata: Metadata
        ): String {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val nameSuffix = fileFormat?.ext ?: Format.CSV.ext
            val fileName = if (fileFormat == Format.INTERNAL || translationConfig == null) {
                // This file-naming format is used for all INTERNAL files, and whenever there is no custom format.
                "${Schema.formBaseName(schemaName)}-$id-${formatter.format(createdDateTime)}"
            } else {
                metadata.fileNameTemplates[nameFormat.lowercase()].run {
                    this?.getFileName(translationConfig, id)
                        ?: "${Schema.formBaseName(schemaName)}-$id-${formatter.format(createdDateTime)}"
                }
            }
            return "$fileName.$nameSuffix"
        }

        /**
         * Try to extract an existing filename from report metadata [header].  If it does not exist or is malformed,
         * create a new filename.
         * @param metadata optional metadata instance used for dependency injection
         */
        fun formExternalFilename(
            header: WorkflowEngine.Header,
            metadata: Metadata? = null
        ): String {
            // extract the filename from the blob url.
            val filename = if (header.reportFile.bodyUrl != null) {
                BlobAccess.BlobInfo.getBlobFilename(header.reportFile.bodyUrl)
            } else ""
            return if (filename.isNotEmpty()) {
                filename
            } else {
                // todo: extend this to use the APHL naming convention
                formFilename(
                    header.reportFile.reportId,
                    header.reportFile.schemaName,
                    header.receiver?.format ?: error("Internal Error: ${header.receiver?.name} does not have a format"),
                    header.reportFile.createdAt,
                    metadata = metadata ?: Metadata.getInstance()
                )
            }
        }

        /**
         * Form external filename for a given [bodyUrl], [reportId], [schemaName], [format] and [createdAt].
         * @param metadata optional metadata instance used for dependency injection
         */
        fun formExternalFilename(
            bodyUrl: String?,
            reportId: ReportId,
            schemaName: String,
            format: Format,
            createdAt: OffsetDateTime,
            metadata: Metadata? = null
        ): String {
            // extract the filename from the blob url.
            val filename = if (bodyUrl != null) {
                BlobAccess.BlobInfo.getBlobFilename(bodyUrl)
            } else ""
            return filename.ifEmpty {
                // todo: extend this to use the APHL naming convention
                formFilename(
                    reportId,
                    schemaName,
                    format,
                    createdAt,
                    metadata = metadata ?: Metadata.getInstance()
                )
            }
        }

        /**
         * Tries to get a value from the underlying row and if there is an error, returns the default provided
         * @param columnName the name of the column to try and query from in this row
         * @param default the default value to return if there's an error getting a value. Defaults to null.
         */
        private fun Row.getStringOrDefault(columnName: String, default: String? = null): String? {
            return try {
                this.getString(columnName)
            } catch (_: Exception) {
                default
            }
        }

        /**
         * Tries to get a value in the underlying row for the column name, and if it doesn't exist, returns null
         */
        private fun Row.getStringOrNull(columnName: String): String? {
            // don't remove the call to `trimToNull` from here. Calls to this method
            // depend on the value being trimmed, potentially down to null, and removing
            // it would potentially change behavior for things like writes to the DB
            return this.getStringOrDefault(columnName, null).trimToNull()
        }

        /**
         * Gets a file format of a blob located at a [blobURL]
         *
         * @return a Report.Format representing the appropriate format
         */
        fun getFormatFromBlobURL(blobURL: String): Format {
            val extension = BlobAccess.BlobInfo.getBlobFileExtension(blobURL)
            return Format.valueOfFromExt(extension)
        }
    }
}