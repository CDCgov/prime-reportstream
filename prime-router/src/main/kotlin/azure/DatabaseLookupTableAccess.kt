package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jooq.JSONB
import org.jooq.impl.DSL

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseLookupTableAccess(private val db: DatabaseAccess = DatabaseAccess()) {
    /**
     * Get the active version of a [tableName] if action.
     * @return the active version or null if no version is active
     */
    fun fetchActiveVersion(tableName: String): Int? {
        var version: Int? = null
        db.transact { txn ->
            version = DSL.using(txn).select(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION).from(Tables.LOOKUP_TABLE_VERSION)
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true))
                )
                .fetchOneInto(Int::class.java)
        }
        return version
    }

    /**
     * Get the latest version of a [tableName].
     * @return the latest version or null if the table does not exist
     */
    fun fetchLatestVersion(tableName: String): Int? {
        var version: Int? = null
        db.transact { txn ->
            version = DSL.using(txn).select(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION).from(Tables.LOOKUP_TABLE_VERSION)
                .where(Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName))
                .orderBy(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.desc())
                .limit(1)
                .fetchOneInto(Int::class.java)
        }
        return version
    }

    /**
     * Get the list of tables.
     * @return the list of tables or an empty list if no tables are present
     */
    fun fetchTableList(fetchInactive: Boolean = false): List<LookupTableVersion> {
        var tables = emptyList<LookupTableVersion>()
        db.transact { txn ->
            val dsl = DSL.using(txn).selectFrom(Tables.LOOKUP_TABLE_VERSION)
            if (!fetchInactive) dsl.where(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true))
            tables = dsl
                .orderBy(Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.asc(), Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.asc())
                .fetchInto(LookupTableVersion::class.java)
        }
        return tables
    }

    /**
     * Test if a [tableName] and [version] exists.
     * @return true if the table exists, false otherwise.
     */
    fun doesTableExist(tableName: String, version: Int): Boolean {
        var retVal = false
        db.transact { txn ->
            retVal = DSL.using(txn).fetchCount(
                Tables.LOOKUP_TABLE_VERSION,
                Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                    .and(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
            ) == 1
        }
        return retVal
    }

    /**
     * Test if a [tableName] regardless of version exists.
     * @return true if the table exists, false otherwise.
     */
    fun doesTableExist(tableName: String): Boolean {
        var retVal = false
        db.transact { txn ->
            retVal = DSL.using(txn).fetchCount(
                Tables.LOOKUP_TABLE_VERSION,
                Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
            ) >= 1
        }
        return retVal
    }

    /**
     * Fetch a give [version] of a [tableName].
     * @return the table data or an empty list if no data is found
     */
    fun fetchTable(tableName: String, version: Int): List<LookupTableRow> {
        var rows = emptyList<LookupTableRow>()
        db.transact { txn ->
            rows = DSL.using(txn).select().from(Tables.LOOKUP_TABLE_ROW).join(Tables.LOOKUP_TABLE_VERSION)
                .on(
                    Tables.LOOKUP_TABLE_ROW.LOOKUP_TABLE_VERSION_ID
                        .eq(Tables.LOOKUP_TABLE_VERSION.LOOKUP_TABLE_VERSION_ID)
                )
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
                )
                .fetchInto(LookupTableRow::class.java)
        }
        return rows
    }

    /**
     * Deactivate a [tableName].
     * @return true if the table was deactivated, false if the table was already inactive
     */
    fun deactivateTable(tableName: String): Boolean {
        var retVal = false
        var updateCount = 0
        db.transact { txn ->
            updateCount = DSL.using(txn).update(Tables.LOOKUP_TABLE_VERSION)
                .set(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE, false)
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true))
                ).execute()
        }

        if (updateCount == 1)
            retVal = true
        return retVal
    }

    /**
     * Activate a [tableName].
     * @return true if the table was activated, false if the table was already active
     */
    fun activateTable(tableName: String, version: Int): Boolean {
        var retVal = false
        var updateCount = 0
        db.transact { txn ->
            // First deactivate the table if it is active
            DSL.using(txn).update(Tables.LOOKUP_TABLE_VERSION).set(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE, false)
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE.eq(true))
                ).execute()

            // Now activate it
            updateCount = DSL.using(txn).update(Tables.LOOKUP_TABLE_VERSION)
                .set(Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE, true)
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
                ).execute()
        }

        if (updateCount == 1)
            retVal = true
        return retVal
    }

    /**
     * Create a new table [version] for a [tableName] using the provided [tableData].
     * This function will throw an exception upon an error and rollback any data inserted into the database.
     */
    fun createTable(tableName: String, version: Int, tableData: List<JSONB>) {
        db.transact { txn ->
            val newVersion = DSL.using(txn).newRecord(Tables.LOOKUP_TABLE_VERSION)
            newVersion.isActive = false
            newVersion.createdBy = System.getProperty("user.name")
            newVersion.tableName = tableName
            newVersion.tableVersion = version
            if (newVersion.store() != 1) error("Error creating new version in database.")

            val versionId = newVersion.lookupTableVersionId
            tableData.forEachIndexed { index, row ->
                val newRow = DSL.using(txn).newRecord(Tables.LOOKUP_TABLE_ROW)
                newRow.data = row
                newRow.rowNum = index + 1
                newRow.lookupTableVersionId = versionId
                if (newRow.store() != 1) error("Error creating new table row in database.")
            }
        }
    }

    /**
     * Get the version information for a given [tableName] and [version].
     * @return table version information or null if not found
     */
    fun fetchVersionInfo(tableName: String, version: Int): LookupTableVersion? {
        var retVal: LookupTableVersion? = null
        db.transact { txn ->
            retVal = DSL.using(txn).selectFrom(Tables.LOOKUP_TABLE_VERSION)
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
                ).fetchOne()?.into(LookupTableVersion::class.java)
        }
        return retVal
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
    }
}