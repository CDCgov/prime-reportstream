package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.io.InputStream

/**
 * Represents a table of metadata that we use to perform lookups on, for example the LIVD table, or FIPS values
 * @constructor creates a new instance from a List of Lists of String.
 */
class LookupTable(
    private val table: List<List<String>>
) {
    private val headerRow: List<String> = table[0].map { it.lowercase() }
    private val headerIndex: Map<String, Int> = headerRow.mapIndexed { index, header -> header to index }.toMap()
    private val columnIndex: MutableMap<String, Map<String, Int>> = mutableMapOf()
    private val indexDelimiter = "|"

    val rowCount: Int get() = table.size - 1

    fun hasColumn(column: String): Boolean {
        return headerIndex.containsKey(column.lowercase())
    }

    /**
     * Performs a search of the table by looking through a column for a value
     * and returning the result for the specified row and column.
     *
     * By default the search is case-insensitive, though you can pass in the flag
     * to make it respect case.
     * @param indexColumn The column to index looking for your value
     * @param indexValue The value to look for in the index column
     * @param lookupColumn The column to pull your value from based on the index
     * @param ignoreCase Whether or not to perform a case insensitive search
     * @return The value you're looking for in the table base on the index look up value
     */
    fun lookupValue(
        indexColumn: String,
        indexValue: String,
        lookupColumn: String,
        ignoreCase: Boolean = true
    ): String? {
        val lcIndexColumn = indexColumn.lowercase()
        val lcLookupColumn = lookupColumn.lowercase()
        val colNumber = headerIndex[lcLookupColumn] ?: return null
        val index = getIndex(listOf(lcIndexColumn), ignoreCase)
        // if the search is case-insensitive we will cast everything to lower case
        // note, this probably only works for English, and US locales. Some languages,
        // may not work correctly if we lower case without a locale
        val indexLookupValue = if (ignoreCase)
            indexValue.lowercase()
        else
            indexValue
        val rowNumber = index[indexLookupValue] ?: return null // Ok if the index value is not found
        return table[rowNumber][colNumber]
    }

    /**
     * Filters a table down based on a predicate, and then invokes the standard lookupValue above
     * on that filtered table below
     * By default the search is case-insensitive, though you can pass in the flag
     * to make it respect case.
     *
     * @param indexColumn The column to index looking for your value
     * @param indexValue The value to look for in the index column
     * @param lookupColumn The column to pull your value from based on the index
     * @param ignoreCase Whether or not to perform a case insensitive search
     * @param filters a Map of columnName and String which will filter the LookupTable down before doing its lookup
     * @return The value you're looking for in the table base on the index look up value
     */
    fun lookupValue(
        indexColumn: String,
        indexValue: String,
        lookupColumn: String,
        filters: Map<String, String>,
        ignoreCase: Boolean = true
    ): String? {
        val filteredTable = filter(filters, ignoreCase)
        return filteredTable.lookupValue(indexColumn, indexValue, lookupColumn, ignoreCase)
    }

    /**
     * Performs a search of the table by looking the indexColumn for value that starts with indexValue
     * and returning the result for the matched row at the lookupColumn.
     *
     * @param indexColumn The column to index looking for your value
     * @param indexValue The value to look for in the index column
     * @param lookupColumn The column to pull your value from based on the index
     * @param ignoreCase Whether or not to perform a case insensitive search
     * @return The value you're looking for in the table base on the index look up value
     */
    fun lookupPrefixValue(
        indexColumn: String,
        indexValue: String,
        lookupColumn: String,
        ignoreCase: Boolean = true
    ): String? {
        val indexColIndex = headerIndex[indexColumn.lowercase()] ?: return null
        val lookupColIndex = headerIndex[lookupColumn.lowercase()] ?: return null
        val findValue = if (ignoreCase) indexValue.lowercase() else indexValue
        for (row in table) {
            val rowsValue = if (ignoreCase) row[indexColIndex].lowercase() else row[indexColIndex]
            if (rowsValue.startsWith(findValue)) return row[lookupColIndex]
        }
        return null
    }

    /**
     * Filters a table down based on a predicate, and then invokes the standard lookupPrefixValue above
     * on that filtered table below
     *
     * @param indexColumn The column to index looking for your value
     * @param indexValue The value to look for in the index column
     * @param lookupColumn The column to pull your value from based on the index
     * @param ignoreCase Whether or not to perform a case insensitive search
     * @param filters a Map of columnName and String which will filter the LookupTable down before doing its lookup
     * @return The value you're looking for in the table base on the index look up value
     */
    fun lookupPrefixValue(
        indexColumn: String,
        indexValue: String,
        lookupColumn: String,
        filters: Map<String, String>,
        ignoreCase: Boolean = true
    ): String? {
        val filteredTable = filter(filters, ignoreCase)
        return filteredTable.lookupPrefixValue(indexColumn, indexValue, lookupColumn, ignoreCase)
    }

    /**
     * Performs a search of the table by looking through a set of column for values
     * and returning the result for the specified row and column.
     *
     * By default the search is case-insensitive, though you can pass in the flag
     * to make it respect case.
     * @param indexValues The values to look for in the index column
     * @param lookupColumn The column to pull your value from based on the index
     * @param ignoreCase Whether or not to perform a case insensitive search
     * @return The value you're looking for in the table base on the index look up value
     */
    fun lookupValues(
        indexValues: List<Pair<String, String>>,
        lookupColumn: String,
        ignoreCase: Boolean = true
    ): String? {
        return when (indexValues.size) {
            1 -> lookupValue(indexValues[0].first, indexValues[0].second, lookupColumn)
            2 -> {
                val lcIndexFirstColumn = indexValues[0].first.lowercase()
                val lcIndexSecondColumn = indexValues[1].first.lowercase()
                val lcLookupColumn = lookupColumn.lowercase()
                val colNumber = headerIndex[lcLookupColumn] ?: return null
                val index = getIndex(listOf(lcIndexFirstColumn, lcIndexSecondColumn), ignoreCase)
                val indexValue = indexValues[0].second.replace(indexDelimiter, "") +
                    indexDelimiter +
                    indexValues[1].second.replace(indexDelimiter, "")
                // if the search is case-insensitive we will cast everything to lower case
                // note, this probably only works for English, and US locales. Some languages,
                // may not work correctly if we lower case without a locale
                val indexLookupValue = if (ignoreCase)
                    indexValue.lowercase()
                else
                    indexValue
                val rowNumber = index[indexLookupValue] ?: return null // Ok if the index value is not found
                return table[rowNumber][colNumber]
            }
            else -> null
        }
    }

    /**
     * Get the set of distinct values in a column in the table.
     */
    fun getDistinctValuesInColumn(selectColumn: String): Set<String> {
        val selectColumnNumber = headerIndex[selectColumn.lowercase()] ?: return emptySet()
        return table.drop(1)
            .map { row -> row[selectColumnNumber] }
            .toSet()
    }

    /**
     * Takes the contents of the table and filters down the rows to match the filter value provided
     */
    fun filter(
        filterColumn: String,
        filterValue: String,
        selectColumn: String,
        ignoreCase: Boolean = true
    ): List<String> {
        val filterColumnNumber = headerIndex[filterColumn.lowercase()] ?: return emptyList()
        val selectColumnNumber = headerIndex[selectColumn.lowercase()] ?: return emptyList()
        return table
            .filter { row -> row[filterColumnNumber].equals(filterValue, ignoreCase) }
            .map { row -> row[selectColumnNumber] }
    }

    /**
     * Takes the contents of the table and filters down the rows where all the filters match.
     * This performs an implicit AND because all of the filters must match
     */
    fun filter(
        selectColumn: String,
        filters: Map<String, String>,
        ignoreCase: Boolean = true
    ): List<String> {
        val selectColumnNumber = headerIndex[selectColumn.lowercase()] ?: return emptyList()
        return table
            .filter { row ->
                filters.all { (k, v) ->
                    val filterColumnNumber = headerIndex[k.lowercase()] ?: error("$k doesn't exist lookup table")
                    row[filterColumnNumber].equals(v, ignoreCase)
                }
            }
            .map { row -> row[selectColumnNumber] }
    }

    /**
     * This filters a LookupTable by comparing values in the table columns specified by the values in the Map
     *
     * <p>
     *     In certain instances we need to be able to filter out some of the records we consider when we do
     *     a lookup in the lookup table. For example, the Sofia 2 test in the LIVD table checks for not only
     *     COVID-19, but also for Influenza A+B, and the only thing that distinguishes between the three possible
     *     result codes are looking for is the test_performed_code (LOINC), and the description for the LOINC code.
     * </p>
     * <p>
     *     This means it is possible if we match on the test_ordered_code, or the device name, we could potentially
     *     return the influenza result code instead of the COVID result code, or vice versa.
     * </p>
     * <p>
     *     This fix allows us to pass in an arbitrary number of indexing columns and create a new table that
     *     we then operate off of.
     * </p>
     * <p>
     *     The goal by returning a new LookupTable is to leverage existing code written for the class while having
     *     additional filtering options
     * </p>
     *
     * @param filters       the map of column name and value to search for in that column
     * @param ignoreCase    whether or not to perform a case insensitive search
     * @return The filtered LookupTable
     */
    fun filter(
        filters: Map<String, String>,
        ignoreCase: Boolean = true
    ): LookupTable {
        if (filters.isEmpty()) return this
        val filteredRows = table
            .filter { row ->
                filters.all { (k, v) ->
                    val filterColumnNumber = headerIndex[k.lowercase()] ?: error("$k doesn't exist in lookup table")
                    row[filterColumnNumber].equals(v, ignoreCase)
                }
            }
        val headerAndFilteredRows = listOf(headerRow) + filteredRows
        return LookupTable(headerAndFilteredRows)
    }

    /**
     * Get the best match for the passed in [searchValue] in the table's [searchColumn]. If a match is found,
     * return the value in the [lookupColumn]. Optionally, filter the table before matching using [filterColumn] and
     * [filterValue]. Similar to lookupValue, but with a different heuristic match algorithm.
     *
     * The best match algorithm is a simplified weighted word match algorithm of [searchValue] to the table's column.
     * To make this algorithm work, the caller must first canonicalize the strings for comparison by passing in a
     * [canonicalize] function. Typically, the [canonicalize] functions should remove punctuations, case,
     * and empty search value words. Empty search value words depends on the domain, but single letter words
     * are typically removed.
     *
     * Next, the caller should pass in a list of [commonWords]. Common words are a list of
     * words which have search value, but which are frequently used in the table. The algorithm will use common words
     * as a tie-breaker, but will never return a match on just common words.
     */
    fun lookupBestMatch(
        searchColumn: String,
        searchValue: String,
        lookupColumn: String,
        canonicalize: (String) -> String,
        commonWords: List<String> = emptyList(),
        filterColumn: String? = null,
        filterValue: String? = null,
    ): String? {
        fun filterRows(): List<List<String>> {
            return if(filterColumn != null && filterValue != null) {
                val filterColumnIndex = headerIndex[filterColumn.lowercase()] ?: error("Invalid filter column name")
                table.filterIndexed { index, row ->
                    index > 0 && row[filterColumnIndex].equals(filterValue, ignoreCase = true)
                }
            } else {
                table.takeLast(table.size - 1)
            }
        }

        // Split into words
        fun wordsFromRaw(input: String): List<String> {
            val canonicalWords = canonicalize(input)
            return canonicalWords
                .trim()
                .replace("\\s+".toRegex(), " ")
                .split(" ")
        }

        // Scoring is based on simplified implementation of a weighted word match algorithm.
        // Common words are given a low weight, others are given a high weight.
        fun scoreRows(rows: List<List<String>>): List<Pair<Double, Int>> {
            // Score based on the search words that are passed in
            val searchWords = wordsFromRaw(searchValue)
            val uncommonSearchWords = searchWords.filter { !commonWords.contains(it) }
            val commonSearchWords = searchWords.filter { commonWords.contains(it) }
            val uncommonFactor = uncommonSearchWords.size + 1
            // +1 means that a full match on common words is less than an uncommon word match

            val searchColumnIndex = headerIndex[searchColumn.lowercase()] ?: error("Invalid index column name")
            return rows.mapIndexed { rowIndex, rawRow ->
                // match uncommon search words
                val rowWords = wordsFromRaw(rawRow[searchColumnIndex])
                val uncommonCount = uncommonSearchWords.fold(0) { count, word ->
                    if (rowWords.contains(word)) count + 1 else count
                }

                // if uncommon words don't match, the score zero
                if (uncommonCount == 0) return@mapIndexed Pair(0.0, rowIndex)

                // count the common matches for tie breaks
                val commonCount = commonSearchWords.fold(0) { count, word ->
                    if (rowWords.contains(word)) count + 1 else count
                }

                // normalize against the possible where all common words are never worth more than an uncommon word
                val score =
                    (uncommonCount * uncommonFactor + commonCount).toDouble() /
                    (uncommonSearchWords.size * uncommonFactor + commonSearchWords.size).toDouble()
                Pair(score, rowIndex)
            }
        }

        val filteredRows = filterRows()
        val rowScores = scoreRows(filteredRows)
        val maxRow = rowScores.maxByOrNull { it.first }
        return if (maxRow != null && maxRow.first > 0.0) {
            // If a match, do a lookup
            val lookupColumnIndex = headerIndex[lookupColumn.lowercase()] ?: error("Invalid lookup column name")
            filteredRows[maxRow.second][lookupColumnIndex]
        } else null
    }

    /**
     * Given a list of column names, walks through each column name and maps out each value to
     * its row in the column to speed lookup for another value in the same row but in a different column.
     */
    @Synchronized
    private fun getIndex(columnNames: List<String>, ignoreCase: Boolean = true): Map<String, Int> {
        val indexName = columnNames.joinToString(indexDelimiter)
        return if (columnIndex.containsKey(indexName)) {
            columnIndex[indexName]!!
        } else {
            val index = table.slice(1 until table.size).mapIndexed { index, row ->
                val values = columnNames.map { columnName ->
                    val column = headerIndex[columnName] ?: error("Internal Error: Lookup logic error")
                    // when we create the values to index map, we need to take the case into consideration
                    // given the fact that we allow for user-submitted data that might not be in the case we
                    // expect, it is better to default all of the keys to lower case instead of depending
                    // on them casing according to our expectations.
                    // For the cases where we use tables, there's no difference between a device name
                    // purely on the basis of case, or a difference between counties on the basis of
                    // case (santa clara and Santa Clara both refer to the same county for example)
                    // That said, we do allow for passing through the flag just in case case matters
                    val preppedValue = if (ignoreCase)
                        row[column].replace(indexDelimiter, "").lowercase()
                    else
                        row[column].replace(indexDelimiter, "")

                    preppedValue
                }
                values.joinToString(indexDelimiter) to index + 1
            }.toMap()
            columnIndex[indexName] = index
            index
        }
    }

    companion object {
        fun read(fileName: String): LookupTable {
            val file = File(fileName)
            return read(file.inputStream(), isTsv = file.extension == "tsv")
        }

        fun read(inputStream: InputStream, isTsv: Boolean = false): LookupTable {
            val reader = csvReader {
                delimiter = if (isTsv) '\t' else ','
            }
            val table = reader.readAll(inputStream)
            return LookupTable(table)
        }
    }
}