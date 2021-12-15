package gov.cdc.prime.router.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Preconditions
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.io.csv.CsvReadOptions
import tech.tablesaw.selection.Selection
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.function.BiPredicate

/**
 * Represents a table of metadata that we use to perform lookups on, for example the LIVD table, or FIPS values
 * @constructor creates a new instance from a List of Lists of String.
 */
open class LookupTable : Logging {
    /**
     * The name of the table.
     */
    var name: String
        private set

    /**
     * The current version of this table.
     */
    var version: Int = 0
        private set

    /**
     * The table data.
     */
    private var table: Table = Table.create()

    /**
     * The database access instance.
     */
    private var tableDbAccess: DatabaseLookupTableAccess

    /**
     * True if the table source is the database.
     */
    var isSourceDatabase = false
        private set

    /**
     * Create table named [name] based on raw [table] data.  The optional [dbAccess] is for dependency injection.
     */
    constructor(
        name: String = "",
        table: List<List<String>> = emptyList(),
        dbAccess: DatabaseLookupTableAccess? = null
    ) {
        this.name = name
        setTableData(table)
        this.tableDbAccess = dbAccess ?: DatabaseLookupTableAccess()
    }

    /**
     * Create table named [name] based on a TableSaw [table] data.  The optional [dbAccess] is for dependency injection.
     */
    constructor(
        name: String = "",
        table: Table = Table.create(),
        dbAccess: DatabaseLookupTableAccess? = null
    ) {
        this.name = name
        this.table = table
        this.tableDbAccess = dbAccess ?: DatabaseLookupTableAccess()
    }
    /**
     * Set the table's data with [tableData].
     */
    private fun setTableData(tableData: List<List<String>>) {
        if (tableData.isEmpty()) table = Table.create()
        else {
            // convert to columns
            val colNames = mutableListOf<String>()
            val tableByCols = MutableList<MutableList<String>>(tableData[0].size) { mutableListOf() }
            tableData.forEachIndexed { rowIndex, tableRow ->
                tableRow.forEachIndexed { colIndex, value ->
                    if (rowIndex == 0) colNames.add(colIndex, value)
                    else {
                        tableByCols[colIndex].add(value)
                    }
                }
            }

            val table = Table.create()
            tableByCols.forEachIndexed { colIndex, colData ->
                table.addColumns(StringColumn.create(colNames[colIndex], colData))
            }
            this.table = table
        }
    }

    /**
     * Clear the contents of the table.
     */
    fun clear() {
        setTableData(emptyList())
    }

    /**
     * The number of rows in the table.
     */
    val rowCount: Int get() = table.rowCount()

    /**
     * Get the raw table data with no headers.
     * @return the raw table data with no headers
     */
    val dataRows: List<List<String>>
        get() {
            val data = mutableListOf<List<String>>()
            if (!table.isEmpty) {
                val colNames = table.columnNames()
                table.forEach { row ->
                    val rowValues = mutableListOf<String>()
                    colNames.forEach { colName ->
                        rowValues.add(row.getString(colName))
                    }
                    data.add(rowValues)
                }
            }
            return data
        }

    /**
     * Test if a [column] name exists in the table.  The search is case-insensitive.
     * @return true if the column exists, false otherwise
     */
    fun hasColumn(column: String): Boolean {
        return table.containsColumn(column)
    }

    /**
     * Generates an exact match selector based on the [exactMatches] columns and value pairs.  The new selector
     * is added to an exiting selector if [selector] is provided.  The matches are case sensitive if [ignoreCase]
     * is set to false.
     * @return the selector
     */
    private fun getExatchMatchSelector(
        exactMatches: Map<String, String>,
        ignoreCase: Boolean = true,
        selector: Selection? = null
    ): Selection? {
        var newSelector = selector
        exactMatches.forEach { (colName, searchValue) ->
            val col = table.stringColumn(colName)
            val stringSelector = if (ignoreCase) col.equalsIgnoreCase(searchValue) else col.isEqualTo(searchValue)
            newSelector = if (selector == null) stringSelector else selector.and(stringSelector)
        }
        return newSelector
    }

    /**
     * Generates a start with selector based on the [prefixMatches] columns and value pairs.  The new selector
     * is added to an exiting selector if [selector] is provided.  The matches are case sensitive if [ignoreCase]
     * is set to false.
     * @return the selector
     */
    private fun getStartsWithSelector(
        prefixMatches: Map<String, String>,
        ignoreCase: Boolean = true,
        selector: Selection? = null
    ): Selection? {
        class StartsWithIgnoreCasePredicate : BiPredicate<String, String> {
            override fun test(t: String, u: String): Boolean {
                return t.startsWith(u, true)
            }
        }

        var newSelector = selector
        prefixMatches.forEach { (colName, searchValue) ->
            val col = table.stringColumn(colName)
            val stringSelector = if (ignoreCase) col.eval(StartsWithIgnoreCasePredicate(), searchValue)
            else col.startsWith(searchValue)
            newSelector = if (selector == null) stringSelector else selector.and(stringSelector)
        }
        return newSelector
    }

    /**
     * Lookup a value in the provided [lookupColumn] name that exactly match the provided [exactMatches] that contain
     * column name and value to match pairs. The matches are case-sensitive if [ignoreCase] is set to false.
     * @return the found unique value or null if no unique value was found or the specified columns do not exist
     */
    fun lookupValue(
        lookupColumn: String,
        exactMatches: Map<String, String>,
        ignoreCase: Boolean = true
    ): String? {
        val values = lookupValues(lookupColumn, exactMatches, ignoreCase)
        return if (values.size == 1) values[0] else null
    }

    /**
     * Lookup all unique values in the provided [lookupColumn] name that exactly match the provided [exactMatches]
     * that contain column name and value to match pairs. The matches are case-sensitive if [ignoreCase] is set to
     * false.
     * @return a list of unique values found, or an empty list if no value was found or the specified columns do not exist
     */
    fun lookupValues(
        lookupColumn: String,
        exactMatches: Map<String, String>,
        ignoreCase: Boolean = true
    ): List<String> {
        return lookupPrefixValues(lookupColumn, emptyMap(), exactMatches, ignoreCase)
    }

    /**
     * Lookup a value in the provided [lookupColumn] name that starts with the provided [prefixMatches] that contain
     * column name and value to match pairs. The optional [exactMatches] further filter the results with exact matches.
     * The matches are case-sensitive if [ignoreCase] is set to false.
     * @return the found unique value or null if no unique value was found or the specified columns do not exist
     */
    fun lookupPrefixValue(
        lookupColumn: String,
        prefixMatches: Map<String, String>,
        exactMatches: Map<String, String> = emptyMap(),
        ignoreCase: Boolean = true
    ): String? {
        val values = lookupPrefixValues(lookupColumn, prefixMatches, exactMatches, ignoreCase)
        return if (values.size == 1) values[0] else null
    }

    /**
     * Lookup all unique values in the provided [lookupColumn] name that starts with the provided [prefixMatches]
     * that contain column name and value to match pairs. The optional [exactMatches] further filter the
     * results with exact matches. The matches are case-sensitive if [ignoreCase] is set to false.
     * @return a list of unique values found, or an empty list if no value was found or the specified columns do not exist
     */
    fun lookupPrefixValues(
        lookupColumn: String,
        prefixMatches: Map<String, String>,
        exactMatches: Map<String, String> = emptyMap(),
        ignoreCase: Boolean = true
    ): List<String> {
        check(prefixMatches.isNotEmpty() || exactMatches.isNotEmpty())
        return try {
            var selector = getStartsWithSelector(prefixMatches, ignoreCase)
            selector = getExatchMatchSelector(exactMatches, ignoreCase, selector)
            @Suppress("UNCHECKED_CAST") // All columns are string columns
            table.where(selector).column(lookupColumn).unique().asList() as List<String>
        } catch (e: IllegalStateException) {
            emptyList()
        }
    }

    /**
     * Get all distinct values from a [column] in the table
     * @return a set with all unique values in the given column or an empty set if the table is empty or the
     * specified column do not exist
     */
    fun getDistinctValuesInColumn(column: String): Set<String> {
        return try {
            @Suppress("UNCHECKED_CAST") // All columns are string columns
            table.column(column).unique().asSet() as Set<String>
        } catch (e: IllegalStateException) {
            emptySet()
        }
    }

    /**
     * Generate a new lookup table by filtering the original table based on the given [exactMatches] that contain
     * column name and value to match pairs. The matches are case-sensitive if [ignoreCase] is set to false.
     * @return a filtered table
     * @throws IllegalStateException if one or more specified columns do not exist
     */
    fun filter(
        exactMatches: Map<String, String>,
        ignoreCase: Boolean = true
    ): LookupTable {
        if (exactMatches.isEmpty()) return this
        try {
            return LookupTable(name, table.where(getExatchMatchSelector(exactMatches, ignoreCase)))
        } catch (e: IllegalStateException) {
            error("One or more columns specified in the matches was not found.")
        }
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
        fun filterRows2(): Table {
            return if (filterColumn != null && filterValue != null) {
                table.where(getExatchMatchSelector(mapOf(filterColumn to filterValue)))
            } else table
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
        fun scoreRows2(table: Table): List<Pair<Double, Int>> {
            // Score based on the search words that are passed in
            val searchWords = wordsFromRaw(searchValue)
            val uncommonSearchWords = searchWords.filter { !commonWords.contains(it) }
            val commonSearchWords = searchWords.filter { commonWords.contains(it) }
            val uncommonFactor = uncommonSearchWords.size + 1
            // +1 means that a full match on common words is less than an uncommon word match

            return table.mapIndexed { rowIndex, rawRow ->
                // match uncommon search words
                val rowWords = wordsFromRaw(rawRow.getString(searchColumn))
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

        val filteredRows = filterRows2()
        val rowScores = scoreRows2(filteredRows)
        val maxRow = rowScores.maxByOrNull { it.first }
        return if (maxRow != null && maxRow.first > 0.0) {
            // If a match, do a lookup
            filteredRows.column(lookupColumn).getString(maxRow.second)
        } else null
    }

    /**
     * Load the table [version] from the database.
     * @return a reference to the lookup table
     */
    fun loadTable(version: Int): LookupTable {
        Preconditions.checkArgument(version > 0)
        logger.trace("Loading database lookup table $name version $version...")
        try {
            val dbTableData = tableDbAccess.fetchTable(name, version)
            val colNames = DatabaseLookupTableAccess.extractTableHeadersFromJson(dbTableData[0].data)
            val lookupTableData = mutableListOf<List<String>>()
            lookupTableData.add(colNames)
            dbTableData.forEach { row ->
                val rowData = jsonMapper.readValue<Map<String, String>>(row.data.data())
                lookupTableData.add(colNames.map { rowData[it] ?: "" })
            }
            setTableData(lookupTableData)
            this.version = version
            this.isSourceDatabase = true
            logger.info("Loaded database lookup table $name version $version with ${dbTableData.size} rows.")
        } catch (e: DataAccessException) {
            logger.error("There was an error loading the database lookup tables.", e)
            throw e
        }
        return this
    }

    companion object {
        /**
         * Import a table from a given CSV [pathname].
         * @return the lookup table
         */
        fun read(pathname: String): LookupTable {
            val csvReaderOptions = CsvReadOptions.builder(pathname).columnTypesToDetect(listOf(ColumnType.STRING))
                .build()
            return LookupTable(FilenameUtils.getBaseName(pathname), Table.read().usingOptions(csvReaderOptions))
        }

        /**
         * Import a table named [name] from a given CSV file contents as an [inputStream].
         * @return the lookup table
         */
        fun read(name: String = "", inputStream: InputStream): LookupTable {
            val csvReaderOptions = CsvReadOptions.builder(inputStream).columnTypesToDetect(listOf(ColumnType.STRING))
                .build()
            return LookupTable(name, Table.read().usingOptions(csvReaderOptions))
        }

        /**
         * Mapper to convert objects to JSON.
         */
        private val jsonMapper: ObjectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()
    }
}