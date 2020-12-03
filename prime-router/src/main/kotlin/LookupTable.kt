package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream

class LookupTable(
    val table: List<Map<String, String>>
) {
    fun hasColumn(column: String): Boolean {
        return table.first().containsKey(column)
    }

    fun lookupValue(indexColumn: String, indexValue: String, lookupColumn: String): String? {
        return table
            .find { it[indexColumn].equals(indexValue, ignoreCase = true) }
            ?.get(lookupColumn)
    }

    fun lookupValues(values: Map<String, String>, lookupColumn: String): String? {
        return table
            .find { row ->
                values.forEach { (name, value) ->
                    if (!row[name].equals(value, ignoreCase = true))
                        return@find false
                }
                true
            }
            ?.get(lookupColumn)
    }

    companion object {
        fun read(inputStream: InputStream): LookupTable {
            return LookupTable(csvReader().readAllWithHeader(inputStream))
        }
    }
}