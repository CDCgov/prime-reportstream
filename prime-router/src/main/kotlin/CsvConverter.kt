package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.InputStream
import java.io.OutputStream

object CsvConverter {
    fun read(name: String, schema: Schema, input: InputStream): MappableTable {
        // Read in the file
        val rows: List<List<String>> = csvReader().readAll(input)
        if (rows.isEmpty()) error("Empty input stream")

        val header = rows[0]
        if (header.size != schema.elements.size) error("Mismatch of elements and header size")

        fun isElementMissing(element: Element, value: String) = value != element.csvField && value != element.name
        schema.elements.zip(header).find { isElementMissing(it.first, it.second) }?.let {
            error("Element ${it.first.csvField} is not found in the input stream header")
        }

        return MappableTable(name, schema, rows.subList(1, rows.size))
    }

    fun write(table: MappableTable, output: OutputStream) {
        val schema = table.schema
        
        fun buildHeader() = schema.elements.map { it.csvField ?: it.name }
        
        fun buildRows() = table.rowIndices.map { row ->
            schema.elements.indices.map { column ->
                table.getString(row, column) ?: ""
            }
        }
        
        val allRows = listOf(buildHeader()).plus(buildRows())
        csvWriter {
            lineTerminator = "\n"
            outputLastLineTerminator = true
        }.writeAll(allRows, output)
    }
}