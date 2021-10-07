package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.common.base.Preconditions
import de.m3y.kformat.Table
import de.m3y.kformat.table
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jooq.JSONB
import java.io.File

/**
 * Commands to manipulate lookup tables.
 */
class LookupTableCommands : CliktCommand(
    name = "lookuptable",
    help = "Manage lookup tables"
) {

    override fun run() {
        // No operation.  The help will be printed out as default.
    }

    companion object {
        /**
         * Extract table column names from the [row] JSON data.
         * @return the list of column names
         */
        internal fun extractTableHeadersFromJson(row: JSONB): List<String> {
            val jsonData = Json.parseToJsonElement(row.data())
            return (jsonData as JsonObject).keys.toList()
        }

        /**
         * Extract table data from a JSON [row] given a list of [colNames].
         * @return a list of data from the row in the same order as the given [colNames]
         */
        internal fun extractTableRowFromJson(row: JSONB, colNames: List<String>): List<String> {
            val rowData = mutableListOf<String>()
            val jsonData = Json.parseToJsonElement(row.data()) as JsonObject
            colNames.forEach { colName ->
                val value = if (jsonData[colName] != null)
                    (jsonData[colName] as JsonPrimitive).content
                else
                    ""
                rowData.add(value)
            }
            return rowData
        }

        /**
         * Sets the JSON for a table [row].
         * @return the JSON representation of the data
         */
        internal fun setTableRowToJson(row: Map<String, String>): JSONB {
            val colNames = row.keys.toList()
            val retVal = buildJsonObject {
                colNames.forEach { col ->
                    put(col, JsonPrimitive(row[col]))
                }
            }
            return JSONB.jsonb(retVal.toString())
        }

        /**
         * Converts table data in [tableRows] to a human readable table.
         * @param addRowNum set to true to add row numbers to the left of the table
         * @return the human readable table
         */
        fun rowsToPrintableTable(tableRows: List<LookupTableRow>, addRowNum: Boolean = true): StringBuilder {
            Preconditions.checkArgument(tableRows.isNotEmpty())

            return table {
                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }

                val colNames = extractTableHeadersFromJson(tableRows[0].data).toMutableList()
                val headers = colNames.toMutableList()
                if (addRowNum) headers.add(0, "Row #")
                header(headers)
                tableRows.forEachIndexed { index, row ->
                    val data = extractTableRowFromJson(row.data, colNames).toMutableList()
                    if (addRowNum) data.add(0, (index + 1).toString())
                    // Row takes varargs, so we convert the list to varargs
                    row(values = data.map { it }.toTypedArray())
                }
            }.render()
        }

        /**
         * Converts a [versionList] to a human readable table.
         * @return the human readable table
         */
        fun infoToPrintableTable(versionList: List<LookupTableVersion>): StringBuilder {
            Preconditions.checkArgument(versionList.isNotEmpty())

            return table {
                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Table Name", "Version", "Is Active", "Created By", "Created At")
                versionList.forEach {
                    row(
                        it.tableName, it.tableVersion, it.isActive.toString(), it.createdBy,
                        it.createdAt.toString()
                    )
                }
            }.render()
        }
    }
}

/**
 * Print out a lookup table.
 */
class LookupTableGetCommand : CliktCommand(
    name = "get",
    help = "Fetch the contents of a lookup table"
) {
    /**
     * Optional output file to save the table to.
     */
    private val outputFile by option("-o", "--output-file", help = "Specify a file to save the table's data as CSV")
        .file(false, canBeDir = false)

    /**
     * Table name option.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * Table version option.
     */
    private val version by option("-v", "--version", help = "The version of the table to get").int()

    override fun run() {
        val tableDbAccess = DatabaseLookupTableAccess()
        val versionNormalized = version
            ?: (
                tableDbAccess.fetchLatestVersion(tableName)
                    ?: error("Lookup table $tableName does not exist.")
                )
        val tableRows = tableDbAccess.fetchTable(tableName, versionNormalized)

        if (tableRows.isNotEmpty()) {
            if (outputFile == null) {
                TermUi.echo("")
                TermUi.echo("Table name: $tableName")
                TermUi.echo("Version: $versionNormalized")
                TermUi.echo(LookupTableCommands.rowsToPrintableTable(tableRows))
                TermUi.echo("")
            } else {
                saveTable(outputFile!!, tableRows)
                TermUi.echo(
                    "Saved ${tableRows.size} rows of table $tableName version $versionNormalized " +
                        "to ${outputFile!!.absolutePath} "
                )
            }
        } else {
            TermUi.echo("Table $tableName version $versionNormalized has no rows.")
        }
    }

    /**
     * Save table data in [tableRows] to an [outputFile] in CSV format.
     */
    private fun saveTable(outputFile: File, tableRows: List<LookupTableRow>) {
        val colNames = LookupTableCommands.extractTableHeadersFromJson(tableRows[0].data)
        val rows = mutableListOf(colNames)
        tableRows.forEach { row ->
            // Row takes varargs, so we convert the list to varargs
            rows.add(LookupTableCommands.extractTableRowFromJson(row.data, colNames))
        }
        csvWriter().writeAll(rows, outputFile.outputStream())
    }
}

/**
 * Create a new lookup table.
 */
class LookupTableCreateCommand : CliktCommand(
    name = "create",
    help = "Create a new version of a lookup table"
) {
    /**
     * The input file to get the table data from.
     */
    private val inputFile by option("-i", "--input-file", help = "Input CSV file with the table data")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    override fun run() {
        val tableDbAccess = DatabaseLookupTableAccess()
        val inputData = csvReader().readAllWithHeader(inputFile)
        if (inputData.size <= 1)
            error("Input file ${inputFile.absolutePath} has no data.")

        val latestActiveVersion = tableDbAccess.fetchLatestVersion(tableName) ?: 0
        val nextVersion = latestActiveVersion + 1

        val tableData = inputData.map { row ->
            val tableRow = LookupTableRow()
            tableRow.data = LookupTableCommands.setTableRowToJson(row)
            tableRow
        }

        TermUi.echo("Here is the table data to be created:")
        TermUi.echo(LookupTableCommands.rowsToPrintableTable(tableData))
        TermUi.echo("")

        if (TermUi.confirm("Continue to create $tableName version $nextVersion with ${tableData.size} rows?") == true) {
            tableDbAccess.createTable(tableName, nextVersion, tableData)
            TermUi.echo("")
            TermUi.echo(
                "${tableData.size} rows created for lookup table $tableName version $nextVersion. " +
                    "Table left inactive, so don't forget to activate it."
            )
        } else
            TermUi.echo("Aborted the creation of the lookup table.")
    }
}

class LookupTableListCommand : CliktCommand(
    name = "list",
    help = "List the lookup tables"
) {
    /**
     * List all the tables including inactive ones if set.
     */
    private val showAll by option("-a", "--all", help = "List all active and inactive tables")
        .flag(default = false)

    override fun run() {
        val tableDbAccess = DatabaseLookupTableAccess()
        val tableList = tableDbAccess.fetchTableList()
        // Determine if we will have any rows to display on the table.
        val emptyDisplayTable = if (showAll)
            tableList.isEmpty()
        else
            tableList.none { it.isActive == true }

        if (showAll)
            TermUi.echo("Listing all lookup tables including inactive.")
        else
            TermUi.echo("Listing only active lookup tables.")

        if (!emptyDisplayTable) {
            TermUi.echo(
                LookupTableCommands
                    .infoToPrintableTable(tableList.filter { showAll || (!showAll && it.isActive) })
            )
            TermUi.echo("")
        } else {
            if (showAll)
                TermUi.echo("No lookup tables were found.")
            else
                TermUi.echo("No active lookup tables were found.")
        }
    }
}

class LookupTableDiffCommand : CliktCommand(
    name = "diff",
    help = "Generate a difference between two versions of a lookup table"
) {
    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * The table version to compare to.
     */
    private val version1 by option("-v1", "--version1", help = "original version of the table to compare from")
        .int().required()

    /**
     * The table version to compare from.
     */
    private val version2 by option("-v2", "--version2", help = "revised version of the table to compare to")
        .int().required()

    /**
     * Display the entire diff output including unchanged lines if set.
     */
    private val fullDiff by option("-a", "--all", help = "Show all diff lines including unchanged lines")
        .flag(default = false)

    override fun run() {
        val tableDbAccess = DatabaseLookupTableAccess()
        if (!tableDbAccess.doesTableExist(tableName, version1))
            error("Lookup table $tableName version $version1 does not exist.")
        if (!tableDbAccess.doesTableExist(tableName, version2))
            error("Lookup table $tableName version $version2 does not exist.")

        val version1Table = tableDbAccess.fetchTable(tableName, version1)
        val version1Info = tableDbAccess.fetchVersionInfo(tableName, version1)
        val version2Table = tableDbAccess.fetchTable(tableName, version2)
        val version2Info = tableDbAccess.fetchVersionInfo(tableName, version2)

        TermUi.echo("Comparing lookup table $tableName versions $version1 and $version2:")
        TermUi.echo(LookupTableCommands.infoToPrintableTable(listOf(version1Info!!, version2Info!!)))
        TermUi.echo("")

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .oldTag { start: Boolean? ->
                if (true == start) "\u001B[9m" else "\u001B[0m" // Use strikethrough for deleted changes
            }
            .newTag { start: Boolean? ->
                if (true == start) "\u001B[1m" else "\u001B[0m" // Use bold for additions
            }
            .build()
        val diff = generator.generateDiffRows(
            LookupTableCommands.rowsToPrintableTable(version1Table, false).toString().split("\n"),
            LookupTableCommands.rowsToPrintableTable(version2Table, false).toString().split("\n")
        )

        if (diff.isNotEmpty()) {
            if (fullDiff) {
                diff.forEach { row ->
                    TermUi.echo(row.oldLine)
                }
            } else {
                var changed = true
                diff.forEachIndexed { index, row ->
                    if (index < 2)
                        TermUi.echo(row.oldLine)
                    else if (row.tag == DiffRow.Tag.EQUAL && changed) {
                        TermUi.echo("...")
                        changed = false
                    } else if (row.tag != DiffRow.Tag.EQUAL) {
                        TermUi.echo(row.oldLine)
                        changed = true
                    }
                }
            }
        } else
            TermUi.echo("Lookup table $tableName version $version1 and $version2 are identical.")
    }
}

class LookupTableActivateCommand : CliktCommand(
    name = "activate",
    help = "Activate a specific version of a lookup table"
) {
    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * Table version option.
     */
    private val version by option("-v", "--version").int().required()

    override fun run() {
        val tableDbAccess = DatabaseLookupTableAccess()
        if (tableDbAccess.doesTableExist(tableName, version)) {
            val rows = tableDbAccess.fetchTable(tableName, version)
            if (rows.isEmpty()) {
                TermUi.echo("ERROR: There is no table data for lookup table $tableName and version $version.")
                return
            } else
                TermUi.echo("Lookup table $tableName version $version has ${rows.size} rows")

            when (val activeVersion = tableDbAccess.fetchActiveVersion(tableName)) {
                version -> {
                    TermUi.echo(
                        "Nothing to do. Lookup table $tableName's active version number is already " +
                            "$activeVersion."
                    )
                    return
                }
                null ->
                    TermUi.echo("Lookup table $tableName is not currently active")
                else ->
                    TermUi.echo("Current Lookup table $tableName's active version number is $activeVersion")
            }

            if (TermUi.confirm("Set $version as active?") == true) {
                if (tableDbAccess.activateTable(tableName, version))
                    TermUi.echo("Version $version for lookup table $tableName was set active.")
                else
                    error("Unknown error when setting lookup table $tableName Version $version to active.")
            } else
                TermUi.echo("Aborted the activation of the lookup table.")
        } else {
            TermUi.echo("ERROR: Lookup table $tableName with version $version does not exist.")
        }
    }
}