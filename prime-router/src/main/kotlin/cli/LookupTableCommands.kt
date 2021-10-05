package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import org.jooq.JSONB
import org.jooq.exception.DataAccessException
import java.io.File

/**
 * Group command configuration.
 */
sealed class LoadConfig(name: String) : OptionGroup(name)

/**
 * Configuration options for the list tables command.
 */
class ListTables : LoadConfig("Options to list lookup tables") {
    val showAll by option("--a", "--all", help = "List all active and inactive tables").flag(default = false)
}

/**
 * Configuration options for the create table command.
 */
class CreateTable : LoadConfig("Options to create a new lookup table version") {
    val inputFile by option("--i", "--input-file", help = "Input CSV file with the table data")
        .file(true, canBeDir = false, mustBeReadable = true).required()
}

/**
 * Configuration options for the get table command.
 */
class GetTable : LoadConfig("Options to fetch a lookup table version") {
    val outputFile by option("--o", "--output-file", help = "Specify a file to save the table's data as CSV")
        .file(false, canBeDir = false, mustBeWritable = true)
}

/**
 * Configuration options for the diff table command.
 */
class DiffTable : LoadConfig("Perform a diff between two table versions") {
    val version1 by option("--v1", "--version1", help = "original version of the table to compare from")
        .int().required()
    val version2 by option("--v2", "--version2", help = "revised version of the table to compare to")
        .int().required()
    val fullDiff by option("--f", "--full-diff", help = "Show all diff lines including unchanged lines")
        .flag(default = false)
}

/**
 * Configuration options for the activate table command.
 */
class ActivateTable : LoadConfig("Options to activate a specific version of a table")

/**
 * Configuration options for the deactivate table command.
 */
class DeactivateTable : LoadConfig("Options to deactivate a table")

/**
 * Commands to manipulate lookup tables.
 */
class LookupTableCommands : CliktCommand(
    name = "lookuptable",
    help = "Create or update lookup tables in the database"
) {
    /**
     * Table name option.
     */
    private val tableName by option("--n", "--name", help = "The name of the table to perform the operation on")

    /**
     * Table version option.
     */
    private val version by option("--v", "--version").int()

    /**
     * Operation options to manipulate lookup tables.
     */
    private val operation by option("--o", "--operation", help = "").groupChoice(
        "list" to ListTables(),
        "get" to GetTable(),
        "create" to CreateTable(),
        "activate" to ActivateTable(),
        "deactivate" to DeactivateTable(),
        "diff" to DiffTable()
    ).defaultByName("list")

    /**
     * Object to access the database tables.
     */
    private val tableDbAccess = DatabaseLookupTableAccess()

    override fun run() {
        // Check if we need the table name.
        if (operation !is ListTables && tableName.isNullOrBlank()) {
            error("Table name is required.")
        }

        try {
            when (operation) {
                is ListTables -> listTables((operation as ListTables).showAll)
                is GetTable -> getTable(tableName!!, version, (operation as GetTable).outputFile)
                is CreateTable -> createTable(tableName!!, (operation as CreateTable).inputFile)
                is ActivateTable -> {
                    when {
                        version == null -> {
                            error("Table version is required.")
                        }
                        version!! >= 0 -> {
                            error("Table version must be a positive number.")
                        }
                        else -> {
                            activate(tableName!!, version!!)
                        }
                    }
                }
                is DeactivateTable -> deactivate(tableName!!)
                is DiffTable -> diffTables(
                    tableName!!, (operation as DiffTable).version1, (operation as DiffTable).version2,
                    (operation as DiffTable).fullDiff
                )
            }
        } catch (e: DataAccessException) {
            TermUi.echo("There was an error getting data from the database.")
            e.printStackTrace()
        }
    }

    /**
     * Get the data for a [tableName] and [version].  If an [outputFile] is specified
     * then the data is saved as a CSV file, otherwise the data is output to the screen.
     * If [version] is null then the latest table is fetched.
     */
    private fun getTable(tableName: String, version: Int?, outputFile: File?) {
        if (tableName.isBlank()) throw IllegalStateException("Table name is required")
        try {
            val versionNormalized = version
                ?: (
                    tableDbAccess.fetchActiveVersion(tableName)
                        ?: error("Unable to obtain a version for table $tableName")
                    )
            val tableRows = if (version != null) {
                tableDbAccess.fetchTable(tableName, version)
            } else {
                tableDbAccess.fetchTable(tableName)
            }

            if (tableRows.isNotEmpty()) {
                if (outputFile == null) {
                    TermUi.echo("")
                    TermUi.echo("Table name: $tableName")
                    TermUi.echo("Version: $versionNormalized")
                    TermUi.echo(toPrintableTable(tableRows))
                    TermUi.echo("")
                } else
                    saveTable(outputFile, tableRows)
            } else {
                TermUi.echo("No tables were found.")
            }
        } catch (e: DataAccessException) {
            TermUi.echo("There was an error fetching the list of tables.")
            e.printStackTrace()
        }
    }

    /**
     * Extract table column names from the [row] JSON data.
     * @return the list of column names
     */
    private fun extractTableHeadersFromJson(row: JSONB): List<String> {
        val jsonData = Json.parseToJsonElement(row.data())
        return (jsonData as JsonObject).keys.toList()
    }

    /**
     * Extract table data from a JSON [row] given a list of [colNames].
     * @return a list of data from the row in the same order as the given [colNames]
     */
    private fun extractTableRowFromJson(row: JSONB, colNames: List<String>): List<String> {
        val rowData = mutableListOf<String>()
        val jsonData = Json.parseToJsonElement(row.data()) as JsonObject
        colNames.forEach { colName ->
            rowData.add((jsonData[colName] as JsonPrimitive).contentOrNull ?: "")
        }
        return rowData
    }

    /**
     * Sets the JSON for a table [row].
     * @return the JSON representation of the data
     */
    private fun setTableRowToJson(row: Map<String, String>): JSONB {
        Preconditions.checkArgument(row.isNotEmpty())
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
    private fun toPrintableTable(tableRows: List<LookupTableRow>, addRowNum: Boolean = true): StringBuilder {
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
     * Save table data in [tableRows] to an [outputFile] in CSV format.
     */
    private fun saveTable(outputFile: File, tableRows: List<LookupTableRow>) {
        val colNames = extractTableHeadersFromJson(tableRows[0].data)
        val rows = mutableListOf(colNames)
        tableRows.forEach { row ->
            // Row takes varargs, so we convert the list to varargs
            rows.add(extractTableRowFromJson(row.data, colNames))
        }
        csvWriter().writeAll(rows, outputFile.outputStream())
        TermUi.echo("Wrote ${tableRows.size} rows to ${outputFile.absolutePath}")
    }

    /**
     * If [showAll] is true then list all the tables.  If false only list active tables.
     */
    private fun listTables(showAll: Boolean) {
        try {
            val tableList = tableDbAccess.fetchTableList()
            if (tableList.isNotEmpty()) {
                val table = table {
                    hints {
                        borderStyle = Table.BorderStyle.SINGLE_LINE
                    }
                    header("Table Name", "Version", "Is Active", "Created By", "Created At")
                    tableList.forEach {
                        if (showAll || (!showAll && it.isActive))
                            row(
                                it.tableName, it.tableVersion, it.isActive.toString(), it.createdBy,
                                it.createdAt.toString()
                            )
                    }
                }.render()
                TermUi.echo("")
                TermUi.echo(table)
                TermUi.echo("")
            } else {
                TermUi.echo("No tables were found.")
            }
        } catch (e: DataAccessException) {
            TermUi.echo("There was an error fetching the list of tables.")
            e.printStackTrace()
        }
    }

    /**
     * Activate a [tableName] for a given [version].
     */
    private fun activate(tableName: String, version: Int) {
        Preconditions.checkNotNull(tableName.isBlank())
        Preconditions.checkNotNull(version)

        if (tableDbAccess.doesTableExist(tableName, version)) {
            val rows = tableDbAccess.fetchTable(tableName, version)
            if (rows.isEmpty()) {
                TermUi.echo("ERROR: There is no table data for table $tableName and version $version.")
                return
            } else
                TermUi.echo("Table $tableName version $version has ${rows.size} rows")

            when (val activeVersion = tableDbAccess.fetchActiveVersion(tableName)) {
                version -> {
                    TermUi.echo("Nothing to do. Table $tableName's active version number is already $activeVersion.")
                    return
                }
                null ->
                    TermUi.echo("Table $tableName is not currently active")
                else ->
                    TermUi.echo("Current table $tableName's active version number is $activeVersion")
            }

            if (TermUi.confirm("Set $version as active?") == true) {
                if (tableDbAccess.activateTable(tableName, version))
                    TermUi.echo("Version $version for table $tableName was set active.")
                else
                    error("Unknown error when setting table $tableName Version $version to active.")
            } else
                TermUi.echo("Aborted operation.")
        } else {
            TermUi.echo("ERROR: Table $tableName with version $version does not exist.")
        }
    }

    /**
     * Deactivate a [tableName].
     */
    private fun deactivate(tableName: String) {
        Preconditions.checkNotNull(tableName.isBlank())

        if (tableDbAccess.doesTableExist(tableName)) {
            val activeVersion = tableDbAccess.fetchActiveVersion(tableName)
            if (activeVersion != null) {
                TermUi.echo("Current active version for table $tableName is $activeVersion.")
                if (TermUi.confirm("Disable table $tableName?") == true) {
                    if (tableDbAccess.deactivateTable(tableName))
                        TermUi.echo("Table $tableName is now inactive.")
                    else
                        error("Unknown error deactivating table $tableName.")
                } else
                    TermUi.echo("Aborted operation.")
            } else
                TermUi.echo("Nothing to do. There are no active versions for table $tableName.")
        } else
            TermUi.echo("ERROR: No table with name $tableName exists.")
    }

    /**
     * Create a new version of a table named [tableName] from the given CSV [inputFile].
     */
    private fun createTable(tableName: String, inputFile: File?) {
        Preconditions.checkNotNull(tableName.isBlank())
        Preconditions.checkNotNull(inputFile)

        val inputData = csvReader().readAllWithHeader(inputFile!!)
        if (inputData.size <= 1)
            error("Input file ${inputFile.absolutePath} has no data.")

        val latestActiveVersion = tableDbAccess.fetchLatestVersion(tableName) ?: 0
        val nextVersion = latestActiveVersion + 1

        val tableData = inputData.map { row ->
            val tableRow = LookupTableRow()
            tableRow.data = setTableRowToJson(row)
            tableRow
        }

        TermUi.echo("Here is the table data to be created:")
        TermUi.echo(toPrintableTable(tableData))
        TermUi.echo("")

        if (TermUi.confirm("Continue to create $tableName version $nextVersion?") == true) {
            tableDbAccess.createTable(tableName, nextVersion, tableData)
            TermUi.echo("")
            TermUi.echo("Table $tableName version $nextVersion created but inactive.  Don't forget to activate it.")
        } else
            error("Unknown error deactivating table $tableName.")
    }

    /**
     * Generate a diff of table [tableName] between [version1] and [version2].  If [showAll] is set to true then
     * the entire table is printed, otherwise only changed lines are printed.
     */
    private fun diffTables(tableName: String, version1: Int?, version2: Int?, showAll: Boolean) {
        Preconditions.checkNotNull(tableName.isBlank())
        Preconditions.checkNotNull(version1)
        Preconditions.checkNotNull(version2)
        Preconditions.checkArgument(version1!! > 0)
        Preconditions.checkArgument(version2!! > 0)

        if (!tableDbAccess.doesTableExist(tableName, version1))
            error("Table $tableName version $version1 does not exist.")
        if (!tableDbAccess.doesTableExist(tableName, version2))
            error("Table $tableName version $version2 does not exist.")

        val version1Table = tableDbAccess.fetchTable(tableName, version1)
        val version2Table = tableDbAccess.fetchTable(tableName, version2)

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .oldTag { start: Boolean? ->
                if (true == start) "\u001B[9m" else "\u001B[0m"
            }
            .newTag { start: Boolean? ->
                if (true == start) "\u001B[1m" else "\u001B[0m"
            }
            .build()
        val diff = generator.generateDiffRows(
            toPrintableTable(version1Table, false).toString().split("\n"),
            toPrintableTable(version2Table, false).toString().split("\n")
        )

        if (diff.isNotEmpty()) {
            if (showAll) {
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
            TermUi.echo("Table $tableName version $version1 and $version2 are identical.")
    }
}