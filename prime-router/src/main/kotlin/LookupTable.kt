package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream

class LookupTable(
    private val table: List<List<String>>
) {
    private val headerRow: List<String>
    private val headerIndex: Map<String, Int>
    private val columnIndex: MutableMap<String, Map<String, Int>>
    private val indexDelimiter = "|"

    val rowCount: Int get() = table.size - 1

    init {
        headerRow = table[0].map { it.toLowerCase() }
        headerIndex = headerRow.mapIndexed { index, header -> header to index }.toMap()
        columnIndex = mutableMapOf()
    }

    fun hasColumn(column: String): Boolean {
        return headerIndex.containsKey(column.toLowerCase())
    }

    fun lookupValue(indexColumn: String, indexValue: String, lookupColumn: String): String? {
        val lcIndexColumn = indexColumn.toLowerCase()
        val lcLookupColumn = lookupColumn.toLowerCase()
        val colNumber = headerIndex[lcLookupColumn] ?: return null
        val index = getIndex(listOf(lcIndexColumn))
        val rowNumber = index[indexValue] ?: return null // Ok if the index value is not found
        return table[rowNumber][colNumber]
    }

    fun lookupValues(
        indexValues: List<Pair<String, String>>,
        lookupColumn: String
    ): String? {
        return when (indexValues.size) {
            1 -> lookupValue(indexValues[0].first, indexValues[0].second, lookupColumn)
            2 -> {
                val lcIndexFirstColumn = indexValues[0].first.toLowerCase()
                val lcIndexSecondColumn = indexValues[1].first.toLowerCase()
                val lcLookupColumn = lookupColumn.toLowerCase()
                val colNumber = headerIndex[lcLookupColumn] ?: return null
                val index = getIndex(listOf(lcIndexFirstColumn, lcIndexSecondColumn))
                val indexValue = indexValues[0].second.replace(indexDelimiter, "") +
                    indexDelimiter +
                    indexValues[1].second.replace(indexDelimiter, "")
                val rowNumber = index[indexValue] ?: return null // Ok if the index value is not found
                return table[rowNumber][colNumber]
            }
            else -> null
        }
    }

    fun filter(
        filterColumn: String,
        filterValue: String,
        selectColumn: String,
        ignoreCase: Boolean = true
    ): List<String> {
        val filterColumnNumber = headerIndex[filterColumn.toLowerCase()] ?: return emptyList()
        val selectColumnNumber = headerIndex[selectColumn.toLowerCase()] ?: return emptyList()
        return table
            .filter { row -> row[filterColumnNumber].equals(filterValue, ignoreCase) }
            .map { row -> row[selectColumnNumber] }
    }

    fun filter(
        selectColumn: String,
        filters: Map<String, String>,
        ignoreCase: Boolean = true
    ): List<String> {
        val selectColumnNumber = headerIndex[selectColumn.toLowerCase()] ?: return emptyList()
        return table
            .filter { row ->
                filters.all { (k, v) ->
                    val filterColumnNumber = headerIndex[k.toLowerCase()] ?: error("$k doesn't exist lookup table")
                    row[filterColumnNumber].equals(v, ignoreCase)
                }
            }
            .map { row -> row[selectColumnNumber] }
    }

    @Synchronized
    private fun getIndex(columnNames: List<String>): Map<String, Int> {
        val indexName = columnNames.joinToString(indexDelimiter)
        return if (columnIndex.containsKey(indexName)) {
            columnIndex[indexName]!!
        } else {
            val index = table.slice(1 until table.size).mapIndexed { index, row ->
                val values = columnNames.map { columnName ->
                    val column = headerIndex[columnName] ?: error("Internal Error: Lookup logic error")
                    row[column].replace(indexDelimiter, "")
                }
                values.joinToString(indexDelimiter) to index + 1
            }.toMap()
            columnIndex[indexName] = index
            index
        }
    }

    companion object {
        fun read(inputStream: InputStream): LookupTable {
            val table = csvReader().readAll(inputStream)
            return LookupTable(table)
        }
    }
}