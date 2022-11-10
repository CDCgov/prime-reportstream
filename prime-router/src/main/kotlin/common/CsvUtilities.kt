package gov.cdc.prime.router.common

import com.github.doyaaaaaken.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import org.apache.commons.io.output.ByteArrayOutputStream

class CsvUtilities {
    companion object {
        /**
         * Cut takes a [csvTable] with a header row and returns
         * another csvTable that only has rows in [indices].
         * Indices are zero based with the row after the header being the first.
         */
        fun cut(csvTable: String, indices: List<Int>): String {
            if (csvTable.isBlank()) {
                if (indices.isEmpty()) return "" else error("Blank content with non-empty indices")
            }
            val inputRows = stringToTable(csvTable)
            val outputRows = listOf(inputRows[0]) + indices.map {
                if (it >= inputRows.size + 1) error("Index $it is out of bounds")
                inputRows[it + 1]
            }
            return tableToString(outputRows)
        }

        /**
         * Given a list of CSV tables [inputCsvList], merge them together into one table.
         * All tables must have the same header row.
         */
        fun merge(inputCsvList: List<String>): String {
            if (inputCsvList.isEmpty()) return ""
            val inputTables = inputCsvList.map { stringToTable(it) }
            // Start with header of the first table
            val mergedTable = mutableListOf<List<String>>(inputTables[0][0])
            inputTables.forEach {
                if (mergedTable[0] != it[0])
                    error("One of the tables does not match the other in a merge")
                mergedTable.addAll(it.takeLast(it.size - 1))
            }
            return tableToString(mergedTable)
        }

        private fun stringToTable(csvTable: String): List<List<String>> {
            return csvReader {
                quoteChar = '"'
                delimiter = ','
                skipEmptyLine = false
                insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.ERROR
                excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.ERROR
            }.readAll(csvTable)
        }

        private fun tableToString(table: List<List<String>>): String {
            val outputStream = ByteArrayOutputStream()
            csvWriter {
                delimiter = ','
                lineTerminator = "\n"
            }.writeAll(table, outputStream)
            return String(outputStream.toByteArray())
        }
    }
}