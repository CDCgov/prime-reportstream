package gov.cdc.prime.router

import gov.cdc.prime.router.azure.DatabaseAccess
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.columns.Column
import tech.tablesaw.selection.Selection
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

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

/**
 * The report represents the report from one agent-organization, and which is
 * translated and sent to another agent-organization. Each report has a schema,
 * unique id and name as well as list of sources for the creation of the report.
 */
class Report {
    enum class Format {
        INTERNAL, // A format that serializes all elements of a report (A CSV today)
        CSV, // A CSV format the follows the csvFields
        HL7, // HL7 with one result per file
        HL7_BATCH, // HL7 with BHS and FHS headers
        REDOX; // Redox format
        // FHIR

        fun toExt(): String {
            return when (this) {
                INTERNAL -> "internal"
                CSV -> "csv"
                HL7 -> "hl7"
                HL7_BATCH -> "hl7"
                REDOX -> "redox"
            }
        }

        fun isSingleItemFormat(): Boolean {
            return when (this) {
                REDOX -> true
                HL7 -> true
                else -> false
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
     * todo this is now redundant with ActionHistory.reportLineages.
     */
    val sources: List<Source>

    /**
     * The intended destination service for this report
     */
    val destination: OrganizationService?

    /**
     * The time when the report was created
     */
    val createdDateTime: OffsetDateTime

    /**
     * The number of items in the report
     */
    val itemCount: Int get() = this.table.rowCount()

    /**
     * A range of item index for this report
     */
    val itemIndices: IntRange get() = 0 until this.table.rowCount()

    /**
     * A standard name for this report that take schema, id, and destination into account
     */
    val name: String get() = formFilename(id, schema.baseName, bodyFormat, createdDateTime)

    /**
     * A format for the body or use the destination format
     */
    val bodyFormat: Format

    /**
     * A pointer to where the Report is stored.
     */
    var bodyURL: String = ""

    // The use of a TableSaw is an implementation detail hidden by this class
    // The TableSaw table is mutable, while this class is has immutable semantics
    //
    // Dev Note: TableSaw is not multi-platform, so it could be switched out in the future.
    // Don't let the TableSaw abstraction leak.
    //
    private val table: Table

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
        destination: OrganizationService? = null,
        bodyFormat: Format? = null
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = sources
        this.createdDateTime = OffsetDateTime.now()
        this.destination = destination
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.table = createTable(schema, values)
    }

    // Test source
    constructor(
        schema: Schema,
        values: List<List<String>>,
        source: TestSource,
        destination: OrganizationService? = null,
        bodyFormat: Format? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = listOf(source)
        this.destination = destination
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.createdDateTime = OffsetDateTime.now()
        this.table = createTable(schema, values)
    }

    // Client source
    constructor(
        schema: Schema,
        values: List<List<String>>,
        source: OrganizationClient,
        destination: OrganizationService? = null,
        bodyFormat: Format? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = listOf(ClientSource(source.organization.name, source.name))
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.destination = destination
        this.createdDateTime = OffsetDateTime.now()
        this.table = createTable(schema, values)
    }

    private constructor(
        schema: Schema,
        table: Table,
        sources: List<Source>,
        destination: OrganizationService? = null,
        bodyFormat: Format? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.table = table
        this.sources = sources
        this.destination = destination
        this.bodyFormat = bodyFormat ?: destination?.format ?: Format.INTERNAL
        this.createdDateTime = OffsetDateTime.now()
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

    private fun fromThisReport(action: String) = listOf(ReportSource(this.id, action))

    /**
     * Does a shallow copy of this report. Will have a new id and create date.
     */
    fun copy(destination: OrganizationService? = null, bodyFormat: Format? = null): Report {
        // Dev Note: table is immutable, so no need to duplicate it
        return Report(
            this.schema,
            this.table,
            fromThisReport("copy"),
            destination ?: this.destination,
            bodyFormat ?: this.bodyFormat
        )
    }

    fun isEmpty(): Boolean {
        return table.rowCount() == 0
    }

    fun getString(row: Int, column: Int): String? {
        return table.getString(row, column)
    }

    fun getString(row: Int, colName: String): String? {
        val column = schema.findElementColumn(colName) ?: return null
        return table.getString(row, column)
    }

    fun getRow(row: Int): List<String> {
        return schema.elements.map {
            val column = schema.findElementColumn(it.name)
                ?: error("Internal Error: column for '${it.name}' is not found")
            table.getString(row, column) ?: ""
        }
    }

    fun filter(filterFunctions: List<Pair<JurisdictionalFilter, List<String>>>): Report {
        val combinedSelection = Selection.withRange(0, table.rowCount())
        filterFunctions.forEach { (filterFn, fnArgs) ->
            val filterFnSelection = filterFn.getSelection(fnArgs, table)
            combinedSelection.and(filterFnSelection)
        }
        val filteredTable = table.where(combinedSelection)
        return Report(this.schema, filteredTable, fromThisReport("filter: $filterFunctions"))
    }

    fun deidentify(): Report {
        val columns = schema.elements.map {
            if (it.pii == true) {
                buildEmptyColumn(it.name)
            } else {
                table.column(it.name).copy()
            }
        }
        return Report(schema, Table.create(columns), fromThisReport("deidentify"))
    }

    // takes the data in the existing report and synthesizes different data from it
    // the goal is to allow us to take real data in, move it around and scramble it so it's
    // not able to point back to the actual records
    fun synthesizeData(
        synthesizeStrategies: Map<String, SynthesizeStrategy> = emptyMap(),
        targetState: String? = null,
        targetCounty: String? = null
    ): Report {
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
                        val shuffledValues = table.column(it.name).asStringColumn().shuffled()
                        StringColumn.create(it.name, shuffledValues)
                    }
                    SynthesizeStrategy.FAKE -> {
                        // generate random faked data for the column passed in
                        buildFakedColumn(it.name, it, targetState, targetCounty)
                    }
                    SynthesizeStrategy.BLANK -> buildEmptyColumn(it.name)
                    SynthesizeStrategy.PASSTHROUGH -> table.column(it.name).copy()
                }
            }
            // if the element name is not mapping, it is handled as a pass through
            synthesizedColumn ?: table.column(it.name).copy()
        }
        // return the new copy of the report here
        return Report(schema, Table.create(columns), fromThisReport("synthesizeData"))
    }

    /**
     * Create a separate report for each item in the report
     */
    fun split(): List<Report> {
        return itemIndices.map {
            val row = getRow(it)
            Report(
                schema = schema,
                values = listOf(row),
                sources = fromThisReport("split"),
                destination = destination,
                bodyFormat = bodyFormat
            )
        }
    }

    fun applyMapping(mapping: Translator.Mapping): Report {
        val pass1Columns = mapping.toSchema.elements.map { element -> buildColumnPass1(mapping, element) }
        val pass2Columns = mapping.toSchema.elements.map { element -> buildColumnPass2(mapping, element, pass1Columns) }
        val newTable = Table.create(pass2Columns)
        return Report(mapping.toSchema, newTable, fromThisReport("mapping"))
    }

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

    private fun buildColumnPass2(
        mapping: Translator.Mapping,
        toElement: Element,
        pass1Columns: List<StringColumn?>
    ): StringColumn {
        val toSchema = mapping.toSchema
        val fromSchema = mapping.fromSchema
        val index = mapping.toSchema.findElementColumn(toElement.name)
            ?: error("Schema Error: buildColumnPass2")
        // pass1 put a null column for columns that should use a mapper
        return if (pass1Columns[index] != null) {
            pass1Columns[index]!!
        } else {
            val mapper = mapping.useMapper[toElement.name]!!
            val (_, args) = Mappers.parseMapperField(
                toElement.mapper
                    ?: error("'${toElement.mapper}' mapper is missing")
            )
            val values = Array(table.rowCount()) { row ->
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
                mapper.apply(toElement, args, inputValues) ?: mapping.useDefault[toElement.name] ?: ""
            }
            return StringColumn.create(toElement.name, values.asList())
        }
    }

    private fun buildEmptyColumn(name: String): StringColumn {
        return StringColumn.create(name, List(itemCount) { "" })
    }

    private fun buildFakedColumn(
        name: String,
        element: Element,
        targetState: String?,
        targetCounty: String?
    ): StringColumn {
        val context = FakeReport.RowContext({ null }, targetState, schema.name, targetCounty)
        val fakeDataService = FakeDataService()
        return StringColumn.create(name, List(itemCount) { fakeDataService.getFakeValueForElement(element, context) })
    }

    companion object {
        fun merge(inputs: List<Report>): Report {
            if (inputs.isEmpty())
                error("Cannot merge an empty report list")
            if (inputs.size == 1)
                return inputs[0]
            if (!inputs.all { it.destination == inputs[0].destination })
                error("Cannot merge reports with different destinations")
            if (!inputs.all { it.bodyFormat == inputs[0].bodyFormat })
                error("Cannot merge reports with different bodyFormats")

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
            return Report(schema, newTable, sources, destination = head.destination, bodyFormat = head.bodyFormat)
        }

        fun formFilename(
            id: ReportId,
            schemaName: String,
            fileFormat: Format?,
            createdDateTime: OffsetDateTime
        ): String {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val namePrefix = "${Schema.formBaseName(schemaName)}-$id-${formatter.format(createdDateTime)}"
            val nameSuffix = fileFormat?.toExt() ?: Format.CSV.toExt()
            return "$namePrefix.$nameSuffix"
        }

        /**
         * Try to extract an existing filename from report metadata.  If it does not exist or is malformed,
         * create a new filename.
         */
        fun formExternalFilename(header: DatabaseAccess.Header): String {
            // extract the filename from the blob url.
            val filename = if (header.reportFile.bodyUrl != null)
                header.reportFile.bodyUrl.split("/").last()
            else ""
            return if (filename.isNotEmpty())
                filename
            else {
                formFilename(
                    header.reportFile.reportId,
                    header.reportFile.schemaName,
                    header.orgSvc?.format ?: error("Internal Error: ${header.orgSvc?.name} does not have a format"),
                    header.reportFile.createdAt
                )
            }
        }
    }
}