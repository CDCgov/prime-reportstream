package gov.cdc.prime.router

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
 * The report represents the report from one agent-organization, and which is
 * translated and sent to another agent-organization. Each report has a schema,
 * unique id and name as well as list of sources for the creation of the report.
 */
class Report {
    val id: ReportId
    val schema: Schema
    val sources: List<Source>
    val destination: OrganizationService?
    val createdDateTime: OffsetDateTime

    val itemCount: Int get() = this.table.rowCount()
    val rowIndices: IntRange get() = 0 until this.table.rowCount()
    val name: String get() = formFileName(id, schema.baseName, destination?.format, createdDateTime)

    // The use of a TableSaw is an implementation detail hidden by this class
    // The TableSaw table is mutable, while this class is has immutable semantics
    //
    // Dev Note: TableSaw is not multi-platform, so it could be switched out in the future.
    // Don't let the TableSaw abstraction leak.
    //
    private val table: Table

    // Generic
    constructor(
        schema: Schema,
        values: List<List<String>>,
        sources: List<Source>,
        destination: OrganizationService? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = sources
        this.createdDateTime = OffsetDateTime.now()
        this.destination = destination
        this.table = createTable(schema, values)
    }

    // Test source
    constructor(
        schema: Schema,
        values: List<List<String>>,
        source: TestSource,
        destination: OrganizationService? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = listOf(source)
        this.destination = destination
        this.createdDateTime = OffsetDateTime.now()
        this.table = createTable(schema, values)
    }

    // Client source
    constructor(
        schema: Schema,
        values: List<List<String>>,
        source: OrganizationClient,
        destination: OrganizationService? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.sources = listOf(ClientSource(source.organization.name, source.name))
        this.destination = destination
        this.createdDateTime = OffsetDateTime.now()
        this.table = createTable(schema, values)
    }

    private constructor(
        schema: Schema,
        table: Table,
        sources: List<Source>,
        destination: OrganizationService? = null,
    ) {
        this.id = UUID.randomUUID()
        this.schema = schema
        this.table = table
        this.sources = sources
        this.destination = destination
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
    fun copy(destination: OrganizationService? = null): Report {
        // Dev Note: table is immutable, so no need to duplicate it
        return Report(this.schema, this.table, fromThisReport("copy"), destination)
    }

    fun isEmpty(): Boolean {
        return table.rowCount() == 0
    }

    fun getString(row: Int, column: Int): String? {
        return table.getString(row, column)
    }

    fun getString(row: Int, colName: String): String? {
        return table.getString(row, colName)
    }

    fun getStringWithDefault(row: Int, colName: String): String {
        return if (table.columnNames().contains(colName)) {
            table.getString(row, colName)
        } else {
            schema.findElement(colName)?.default ?: ""
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
                createDefaultColumn(it)
            } else {
                table.column(it.name).copy()
            }
        }
        return Report(schema, Table.create(columns), fromThisReport("deidentify"))
    }

    fun applyMapping(mapping: Translator.Mapping): Report {
        val columns = mapping.toSchema.elements.map { buildColumn(mapping, it) }
        val newTable = Table.create(columns)
        return Report(mapping.toSchema, newTable, fromThisReport("mapping"))
    }

    private fun buildColumn(mapping: Translator.Mapping, toElement: Element): StringColumn {
        return when (toElement.name) {
            in mapping.useDirectly -> {
                table.stringColumn(mapping.useDirectly[toElement.name]).copy().setName(toElement.name)
            }
            in mapping.useMapper -> {
                createMappedColumn(toElement, mapping.useMapper.getValue(toElement.name))
            }
            in mapping.useDefault -> {
                createDefaultColumn(toElement)
            }
            else -> error("missing mapping for element: ${toElement.name}")
        }
    }

    private fun createDefaultColumn(element: Element): StringColumn {
        val defaultValues = Array(table.rowCount()) { element.default ?: "" }
        return StringColumn.create(element.name, defaultValues.asList())
    }

    private fun createMappedColumn(toElement: Element, mapper: Mapper): StringColumn {
        val args = Mappers.parseMapperField(toElement.mapper ?: error("mapper is missing")).second
        val values = Array(table.rowCount()) { row ->
            val inputValues = mapper.valueNames(toElement, args).mapNotNull { elementName ->
                val value = table.getString(row, elementName)
                if (value.isBlank()) return@mapNotNull null
                val element = schema.findElement(elementName) ?: return@mapNotNull null
                ElementAndValue(element, value)
            }
            mapper.apply(toElement, args, inputValues) ?: toElement.default ?: ""
        }
        return StringColumn.create(toElement.name, values.asList())
    }

    companion object {
        fun merge(inputs: List<Report>): Report {
            if (inputs.isEmpty()) error("Cannot merge an empty report list")
            if (inputs.size == 1) return inputs[0]
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

            return Report(schema, newTable, sources)
        }

        fun formFileName(
            id: ReportId,
            schemaName: String,
            fileFormat: OrganizationService.Format?,
            createdDateTime: OffsetDateTime
        ): String {
            val formatter = DateTimeFormatter.ofPattern("YYYYMMddHHmmss")
            val namePrefix = "${Schema.formBaseName(schemaName)}-$id-${formatter.format(createdDateTime)}"
            val nameSuffix = fileFormat?.toExt() ?: OrganizationService.Format.CSV.toExt()
            return "$namePrefix.$nameSuffix"
        }
    }
}