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

        // Check column names
        schema.elements.forEachIndexed { index, element ->
            val header = rows[0]
            if (index >= header.size ||
                (header[index] != element.csvField && header[index] != element.name)
            ) {
                error("Element ${element.csvField} is not found in the input stream header")
            }
        }
        return MappableTable(name, schema, rows.subList(1, rows.size))
    }

    fun write(table: MappableTable, output: OutputStream) {
        val schema = table.schema
        val valueRows =
            table.rowIndices.map { row ->
                schema.elements.indices.map { column ->
                    table.getString(row, column) ?: ""
                }
            }
        val allRows = mutableListOf(schema.elements.map { it.csvField ?: it.name })
        allRows.addAll(valueRows)
        csvWriter {
            lineTerminator = "\n"
            outputLastLineTerminator = true
        }.writeAll(allRows, output)
    }
}