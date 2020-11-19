package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.InputStream
import java.io.OutputStream

/**
 * A converter differs from a serialization in that
 */
object CsvConverter {
    fun read(schema: Schema, input: InputStream, source: Source): Report {
        // Read in the file
        val rows: List<List<String>> = csvReader().readAll(input)
        if (rows.isEmpty()) error("Empty input stream")

        val header = rows[0]
        if (header.size != schema.elements.size) error("Mismatch of elements and header size")

        fun isElementMissing(element: Element, value: String) = value != element.csvField && value != element.name
        schema.elements.zip(header).find { isElementMissing(it.first, it.second) }?.let {
            error("Element ${it.first.csvField} is not found in the input stream header")
        }

        return Report(schema, rows.subList(1, rows.size), listOf(source))
    }

    fun write(report: Report, output: OutputStream) {
        val schema = report.schema
        
        fun buildHeader() = schema.elements.map { it.csvField ?: it.name }
        
        fun buildRows() = report.rowIndices.map { row ->
            schema.elements.indices.map { column ->
                report.getString(row, column) ?: ""
            }
        }
        
        val allRows = listOf(buildHeader()).plus(buildRows())
        csvWriter {
            lineTerminator = "\n"
            outputLastLineTerminator = true
        }.writeAll(allRows, output)
    }
}