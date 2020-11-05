@file:Suppress("Destructure")

package gov.cdc.prime.router

import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.columns.Column
import tech.tablesaw.selection.Selection
import java.io.InputStream
import java.io.OutputStream

class MappableTable {
    val name: String
    val schema: Schema
    val rowCount: Int get() = this.table.rowCount()
    val rowIndices: IntRange get() = 0 until this.table.rowCount()

    // The use of a TableSaw is an implementation detail hidden by this class
    // The TableSaw table is mutable, while this class is has immutable semantics
    //
    // Dev Note: TableSaw is not multi-platform, so it could be switched out in the future.
    // Don't let the TableSaw abstraction leak.
    //
    private val table: Table

    constructor(name: String, schema: Schema, values: List<List<String>> = emptyList()) {
        this.name = name
        this.schema = schema
        this.table = createTable(name, schema, values)
    }

    private constructor(name: String, schema: Schema, table: Table) {
        this.schema = schema
        this.name = name
        this.table = table
    }

    private fun createTable(name: String, schema: Schema, values: List<List<String>>): Table {
        fun valuesToColumns(schema: Schema, values: List<List<String>>): List<Column<*>> {
            return schema.elements.mapIndexed { index, element ->
                StringColumn.create(element.name, values.map { it[index] })
            }
        }

        return Table.create(name, valuesToColumns(schema, values))
    }

    fun copy(name: String = this.name): MappableTable {
        return MappableTable(name, this.schema, this.table.copy())
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

    fun concat(name: String, appendTable: MappableTable): MappableTable {
        if (appendTable.schema != this.schema) error("concat a table with a different schema")
        val newTable = this.table.copy().append(appendTable.table)
        return MappableTable(name, this.schema, newTable)
    }

    fun filter(name: String, patterns: Map<String, String>): MappableTable {
        val combinedSelection = Selection.withRange(0, table.rowCount())
        patterns.forEach { (col, pattern) ->
            val columnSelection = table.stringColumn(col).matchesRegex(pattern)
            combinedSelection.and(columnSelection)
        }
        val filteredTable = table.where(combinedSelection)
        return MappableTable(name, this.schema, filteredTable)
    }

    @Deprecated("I'd like to remove receivers from this class abstraction")
    fun routeByReceiver(receivers: List<Receiver>): List<MappableTable> {
        return receivers.filter {
            it.topic == schema.topic
        }.map { receiver: Receiver ->
            val outputName = "${receiver.name}-${name}"
            val input: MappableTable = if (receiver.schema != schema.name) {
                val toSchema =
                    Metadata.findSchema(receiver.schema) ?: error("${receiver.schema} schema is missing from catalog")
                val mapping = schema.buildMapping(toSchema)
                this.applyMapping(outputName, mapping)
            } else {
                this
            }
            val filtered = input.filter(name = outputName, patterns = receiver.patterns)
            var transformed = filtered
            receiver.transforms.forEach { (transform, transformValue) ->
                when (transform) {
                    "deidentify" -> if (transformValue == "true") {
                        transformed = transformed.deidentify()
                    }
                }
            }
            transformed
        }
    }

    fun deidentify(): MappableTable {
        val columns = schema.elements.map {
            if (it.pii == true) {
                createDefaultColumn(it)
            } else {
                table.column(it.name).copy()
            }
        }
        return MappableTable(name, schema, Table.create(columns))
    }

    fun applyMapping(name: String, mapping: Schema.Mapping): MappableTable {
        val columns = mapping.toSchema.elements.map { buildColumn(mapping, it) }
        val newTable = Table.create(columns)
        return MappableTable(name, mapping.toSchema, newTable)
    }



    private fun buildColumn(mapping: Schema.Mapping, toElement: Element): StringColumn {
        return when (toElement.name) {
            in mapping.useDirectly -> {
                table.stringColumn(mapping.useDirectly[toElement.name]).copy().setName(toElement.name)
            }
            in mapping.useValueSet -> {
                val valueSetName = mapping.useValueSet.getValue(toElement.name)
                val valueSet = Metadata.findValueSet(valueSetName) ?: error("$valueSetName is not found")
                createValueSetTranslatedColumn(toElement, valueSet)
            }
            in mapping.useTranslator -> {
                createTranslatedColumn(toElement, mapping.useTranslator.getValue(toElement.name))
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

    private fun createTranslatedColumn(toElement: Element, translator: Translator): StringColumn {
        val values = Array(table.rowCount()) { row ->
            val inputValues = translator.fromElements.map { columnName ->
                table.getString(row, columnName)
            }
            translator.apply(inputValues) ?: toElement.default ?: ""
        }
        return StringColumn.create(toElement.name, values.asList())
    }

    private fun createValueSetTranslatedColumn(toElement: Element, valueSet: ValueSet): StringColumn {
        val values = when {
            toElement.isCodeText -> {
                Array(table.rowCount()) { row ->
                    val fromCode = table.getString(row, toElement.nameAsCode)
                    valueSet.toDisplay(fromCode) ?: toElement.default ?: ""
                }
            }
            toElement.isCodeSystem -> {
                Array(table.rowCount()) { valueSet.systemCode }
            }
            toElement.isCode -> {
                Array(table.rowCount()) { row ->
                    val fromDisplay = table.getString(row, toElement.nameAsCodeText)
                    valueSet.toCode(fromDisplay) ?: toElement.default ?: ""
                }
            }
            else -> error("Cannot convert ${toElement.name} using value set")
        }
        return StringColumn.create(toElement.name, values.asList())
    }
}