package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Preconditions
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import org.jooq.JSONB
import org.jooq.impl.DSL
import java.lang.IllegalArgumentException
import java.security.MessageDigest

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
            version = DSL.using(txn).select(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION).from(
                Tables.LOOKUP_TABLE_VERSION
            )
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
            version = DSL.using(txn).select(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION).from(
                Tables.LOOKUP_TABLE_VERSION
            )
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
            rows = DSL.using(txn).select().from(Tables.LOOKUP_TABLE_ROW).join(
                Tables.LOOKUP_TABLE_VERSION
            )
                .on(
                    Tables.LOOKUP_TABLE_ROW.LOOKUP_TABLE_VERSION_ID
                        .eq(Tables.LOOKUP_TABLE_VERSION.LOOKUP_TABLE_VERSION_ID)
                )
                .where(
                    Tables.LOOKUP_TABLE_VERSION.TABLE_NAME.eq(tableName)
                        .and(Tables.LOOKUP_TABLE_VERSION.TABLE_VERSION.eq(version))
                )
                .orderBy(Tables.LOOKUP_TABLE_ROW.LOOKUP_TABLE_ROW_ID.asc())
                .fetchInto(LookupTableRow::class.java)
        }
        return rows
    }

    /**
     * Activate a [tableName].
     * @return true if the table was activated, false if the table was already active
     */
    fun activateTable(tableName: String, version: Int): Boolean {
        var updateCount = 0
        db.transact { txn ->
            // First deactivate the table if it is active
            DSL.using(txn).update(Tables.LOOKUP_TABLE_VERSION).set(
                Tables.LOOKUP_TABLE_VERSION.IS_ACTIVE, false
            )
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

        return updateCount == 1
    }

    /**
     * Create a new table [version] for a [tableName] using the provided [tableData].  The [force] flag is used
     * to force an update of a lookup table.
     * This function will throw an exception upon an error and rollback any data inserted into the database.
     */
    fun createTable(tableName: String, version: Int, tableData: List<JSONB>, username: String, force: Boolean) {
        val batchSize = 5000
        val newTableChecksum = tableData.toString().toSHA256()
        // Check for duplicate tables.  If it is up-to-date and force=true,
        // we force to update the table regardless.
        val versionConflict = isTableUpToDate(tableName, newTableChecksum)
        if (versionConflict != null && !force)
            throw DuplicateTableException(
                "Table is identical to existing table version $versionConflict."
            )

        db.transact { txn ->
            val newVersion = DSL.using(txn).newRecord(Tables.LOOKUP_TABLE_VERSION)
            newVersion.isActive = false
            newVersion.createdBy = username
            newVersion.tableName = tableName
            newVersion.tableSha256Checksum = newTableChecksum
            newVersion.tableVersion = version
            if (newVersion.store() != 1) error("Error creating new version in database.")

            val versionId = newVersion.lookupTableVersionId

            // Use batching to make this faster
            var batchNumber = 1
            do {
                val dataBatch = getDataBatch(tableData, batchNumber, batchSize)
                if (dataBatch != null) {
                    val newRecords = dataBatch.mapIndexed { index, row ->
                        val newRow = DSL.using(txn).newRecord(Tables.LOOKUP_TABLE_ROW)
                        newRow.data = row
                        newRow.rowNum = ((batchNumber - 1) * batchSize) + index + 1 // Row numbers start at 1
                        newRow.lookupTableVersionId = versionId
                        newRow
                    }
                    if (DSL.using(txn).batchInsert(newRecords).execute().any { it < 0 })
                        error("Error batch creating rows for table $tableName version $version")
                    batchNumber++
                }
            } while (dataBatch != null)
        }
        // refresh the materialized views
        db.refreshMaterializedViews(tableName)
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

    /**
     * Calculate the SHA-256 checksum of the input data.
     * @return SHA-256 checksum value
     */
    private fun String.toSHA256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Check against active or latest table if it equals to either of those table.
     * @return version that equal to the new table, otherwise, null
     */
    private fun isTableUpToDate(tableName: String, tableSHA256: String): Int? {
        val activeVersion = fetchActiveVersion(tableName)
        if (activeVersion != null) {
            val activeTableVersion = fetchVersionInfo(tableName, activeVersion)
            if (activeTableVersion?.tableSha256Checksum == tableSHA256) return activeVersion
        }

        val latestVersion = fetchLatestVersion(tableName)
        if (latestVersion != null) {
            val latestTableVersion = fetchVersionInfo(tableName, latestVersion)
            if (latestTableVersion?.tableSha256Checksum == tableSHA256) return latestVersion
        }
        return null
    }

    companion object {
        /**
         * Extract table column names from the [row] JSON data.
         * @return the list of column names
         */
        internal fun extractTableHeadersFromJson(row: JSONB): List<String> {
            try {
                val rows = jacksonObjectMapper().readValue<Map<String, String>>(row.data())
                Preconditions.checkArgument(rows.isNotEmpty())
                return rows.keys.toList()
            } catch (e: MismatchedInputException) {
                throw IllegalArgumentException(e)
            }
        }

        /**
         * Split the [inputData] into a data batch of max of [batchSize] and return the batch for [batchNumber].
         * @return a list of data or null of no data is left
         */
        internal fun getDataBatch(inputData: List<JSONB>, batchNumber: Int, batchSize: Int): List<JSONB>? {
            Preconditions.checkArgument(batchNumber > 0)
            Preconditions.checkArgument(batchSize > 0)
            val start = (batchNumber - 1) * batchSize
            return if (start > inputData.size || inputData.isEmpty()) null
            else {
                val end = if ((start + batchSize) < inputData.size) start + batchSize else inputData.size
                inputData.subList(start, end)
            }
        }

        class DuplicateTableException(message: String) : Exception(message)
    }
}