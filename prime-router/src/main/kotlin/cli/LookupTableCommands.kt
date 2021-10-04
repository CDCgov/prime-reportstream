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
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.m3y.kformat.Table
import de.m3y.kformat.table
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.LOOKUP_TABLE_ROW
import gov.cdc.prime.router.azure.db.Tables.LOOKUP_TABLE_VERSION
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jooq.JSONB
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import java.io.File

sealed class LoadConfig(name: String) : OptionGroup(name)
class ListTables : LoadConfig("Options to list all tables") {
    val showAll by option("--a", "--all", help = "List all active and inactive tables").flag(default = false)
}

class CreateTable : LoadConfig("Options to create a new table version") {
    val inputFile by option("--i", "--input-file").file(true, canBeDir = false, mustBeReadable = true).required()
}

class GetTable : LoadConfig("Fetch a table version and output to a CSV file") {
    val outputFile by option("--o", "--output-file").file(false, canBeDir = false)
}

class ActivateTable : LoadConfig("Activate a specific version of a table")

class DeactivateTable : LoadConfig("Deactivate a table")

class LookupTableCommands : CliktCommand(
    name = "lookuptable",
    help = "Create or update lookup tables in the database"
) {
    val tableName by option("--n", "--name", help = "The name of the table to perform the operation on")
    val version by option("--v", "--version").int()
    val cmd by option("--c", "--command", help = "").groupChoice(
        "list" to ListTables(),
        "get" to GetTable(),
        "create" to CreateTable(),
        "activate" to ActivateTable(),
        "deactivate" to DeactivateTable()
    ).defaultByName("list")
    val tableDbAccess = DatabaseTableLookupAccess()

    override fun run() {
        // Check if we need the table name.
        if (cmd !is ListTables && tableName.isNullOrBlank()) {
            error("Table name is required.")
        }

        try {
            when (cmd) {
                is ListTables -> listTables((cmd as ListTables).showAll)
                is GetTable -> getTable(tableName, version, (cmd as GetTable).outputFile)
                is CreateTable -> println()
                is ActivateTable -> {
                    if (version == null) {
                        error("Table version is required.")
                    } else {
                        activate(tableName!!, version!!)
                    }
                }
                is DeactivateTable -> deactivate(tableName!!)
            }
        } catch (e: DataAccessException) {
            TermUi.echo("There was an error getting data from the database.")
            e.printStackTrace()
        }
    }

    private fun getTable(tableName: String?, version: Int?, outputFile: File?) {
        if (tableName.isNullOrBlank()) throw IllegalStateException("Table name is required")
        var versionNormalized = version
        try {
            var tableRows = if (version != null) {
                tableDbAccess.fetchTable(tableName, version)
            } else {
                tableDbAccess.fetchTable(tableName)
            }
            versionNormalized = tableDbAccess.fetchActiveVersion(tableName)

            if (versionNormalized == null) {
                error("Unable to obtain a version for table $tableName")
            }

            if (tableRows.isNotEmpty()) {
                if (outputFile == null)
                    printTable(tableName, versionNormalized!!, tableRows)
                else
                    saveTable(outputFile, tableRows)
            } else {
                TermUi.echo("No tables were found.")
            }
        } catch (e: DataAccessException) {
            TermUi.echo("There was an error fetching the list of tables.")
            e.printStackTrace()
        }
    }

    private fun getTableHeadersFromJson(row: JSONB): List<String> {
        val jsonData = Json.parseToJsonElement(row.data())
        return (jsonData as JsonObject).keys.toList()
    }

    private fun getTableRowFromJson(row: JSONB, colNames: List<String>): List<String> {
        val rowData = mutableListOf<String>()
        val jsonData = Json.parseToJsonElement(row.data()) as JsonObject
        colNames.forEach { colName ->
            rowData.add((jsonData[colName] as JsonPrimitive).contentOrNull ?: "")
        }
        return rowData
    }

    private fun printTable(name: String, version: Int, tableRows: List<LookupTableRow>) {
        val table = table {
            hints {
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }

            val colNames = getTableHeadersFromJson(tableRows[0].data)
            header(colNames)
            tableRows.forEach { row ->
                // Row takes varargs, so we convert the list to varargs
                row(values = getTableRowFromJson(row.data, colNames).map { it }.toTypedArray())
            }
        }.render()
        TermUi.echo("")
        TermUi.echo("Table name: $name")
        TermUi.echo("Version: $version")
        TermUi.echo(table)
        TermUi.echo("")
    }

    private fun saveTable(outputFile: File, tableRows: List<LookupTableRow>) {
        val colNames = getTableHeadersFromJson(tableRows[0].data)
        val rows = mutableListOf(colNames)
        tableRows.forEach { row ->
            // Row takes varargs, so we convert the list to varargs
            rows.add(getTableRowFromJson(row.data, colNames))
        }
        csvWriter().writeAll(rows, outputFile.outputStream())
        TermUi.echo("Wrote ${tableRows.size} rows to ${outputFile.absolutePath}")
    }

    private fun listTables(showAll: Boolean) {
        try {
            var tableList = tableDbAccess.fetchTableList()
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

    private fun activate(tableName: String, version: Int) {
        if (tableName.isNullOrBlank()) throw IllegalStateException("Table name is required")
        if (version == null) throw IllegalStateException("Table version is required")

        if (tableDbAccess.doesTableExist(tableName, version)) {
            val rows = tableDbAccess.fetchTable(tableName, version)
            if (rows.isEmpty()) {
                TermUi.echo("ERROR: There is no table data for table $tableName and version $version.")
                return
            } else
                TermUi.echo("Table $tableName version $version has ${rows.size} rows")

            val activeVersion = tableDbAccess.fetchActiveVersion(tableName)
            when (activeVersion) {
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

    private fun deactivate(tableName: String) {
        if (tableName.isNullOrBlank()) throw IllegalStateException("Table name is required")

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
}

class DatabaseTableLookupAccess() {
    private val db = DatabaseAccess()
    fun fetchActiveVersion(tableName: String): Int? {
        var version: Int? = null
        db.transact { txn ->
            version = DSL.using(txn).select(LOOKUP_TABLE_VERSION.TABLE_VERSION).from(LOOKUP_TABLE_VERSION)
                .where(LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName).and(LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true)))
                .fetchOneInto(Int::class.java)
        }
        return version
    }

    fun fetchTableList(): List<LookupTableVersion> {
        var tables = emptyList<LookupTableVersion>()
        db.transact { txn ->
            tables = DSL.using(txn)
                .selectFrom(LOOKUP_TABLE_VERSION)
                .orderBy(LOOKUP_TABLE_VERSION.TABLE_NAME.asc(), LOOKUP_TABLE_VERSION.TABLE_VERSION.asc())
                .fetchInto(LookupTableVersion::class.java)
        }
        return tables
    }

    fun fetchTable(name: String): List<LookupTableRow> {
        var rows = emptyList<LookupTableRow>()
        db.transact { txn ->
            rows = DSL.using(txn).select().from(LOOKUP_TABLE_ROW).join(LOOKUP_TABLE_VERSION)
                .on(LOOKUP_TABLE_ROW.LOOKUP_TABLE_VERSION_ID.eq(LOOKUP_TABLE_VERSION.LOOKUP_TABLE_VERSION_ID))
                .where(
                    LOOKUP_TABLE_VERSION.TABLE_NAME.eq(name)
                        .and(LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true))
                )
                .fetchInto(LookupTableRow::class.java)
        }
        return rows
    }

    fun fetchTable(name: String, version: Int): List<LookupTableRow> {
        var rows = emptyList<LookupTableRow>()
        db.transact { txn ->
            rows = DSL.using(txn).select().from(LOOKUP_TABLE_ROW).join(LOOKUP_TABLE_VERSION)
                .on(LOOKUP_TABLE_ROW.LOOKUP_TABLE_VERSION_ID.eq(LOOKUP_TABLE_VERSION.LOOKUP_TABLE_VERSION_ID))
                .where(
                    LOOKUP_TABLE_VERSION.TABLE_NAME.eq(name)
                        .and(LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
                )
                .fetchInto(LookupTableRow::class.java)
        }
        return rows
    }

    fun doesTableExist(name: String, version: Int): Boolean {
        var retVal = false
        db.transact { txn ->
            retVal = DSL.using(txn).fetchCount(
                LOOKUP_TABLE_VERSION,
                LOOKUP_TABLE_VERSION.TABLE_NAME.eq(name)
                    .and(LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
            ) == 1
        }
        return retVal
    }

    fun doesTableExist(name: String): Boolean {
        var retVal = false
        db.transact { txn ->
            retVal = DSL.using(txn).fetchCount(
                LOOKUP_TABLE_VERSION,
                LOOKUP_TABLE_VERSION.TABLE_NAME.eq(name)
            ) >= 1
        }
        return retVal
    }

    fun deactivateTable(name: String): Boolean {
        return updateTableState(name, 0, false)
    }

    fun activateTable(name: String, version: Int): Boolean {
        return updateTableState(name, version, true)
    }

    private fun updateTableState(name: String, version: Int, isActive: Boolean): Boolean {
        var retVal = false
        var updateCount = 0
        db.transact { txn ->
            // First disable all the other tables if any are active.
            DSL.using(txn).update(LOOKUP_TABLE_VERSION).set(LOOKUP_TABLE_VERSION.IS_ACTIVE, false)
                .where(LOOKUP_TABLE_VERSION.TABLE_NAME.eq(name).and(LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true))).execute()

            // Then enable the one table version.
            if (isActive)
                updateCount = DSL.using(txn).update(LOOKUP_TABLE_VERSION).set(LOOKUP_TABLE_VERSION.IS_ACTIVE, true)
                    .where(
                        LOOKUP_TABLE_VERSION.TABLE_NAME.eq(name)
                            .and(LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
                    ).execute()
        }

        if ((isActive && updateCount == 1) || !isActive) {
            retVal = true
        }
        return retVal
    }
}