package gov.cdc.prime.router.metadata

import com.google.common.base.Preconditions
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException

/**
 * Database based lookup table.
 * @param name the name of the table
 * @param tableDbAccess for dependency injection
 */
class DatabaseLookupTable(
    val name: String,
    private val tableDbAccess: DatabaseLookupTableAccess = DatabaseLookupTableAccess()
) : LookupTable(emptyList()), Logging {
    /**
     * The current version of this table.
     */
    var version: Int = 0

    /**
     * Load the table [version] from the database.
     */
    fun loadTable(version: Int): DatabaseLookupTable {
        Preconditions.checkArgument(version > 0)
        logger.trace("Loading database lookup table $name version $version...")
        try {
            val dbTableData = tableDbAccess.fetchTable(name, version)
            val colNames = DatabaseLookupTableAccess.extractTableHeadersFromJson(dbTableData[0].data)
            val lookupTableData = mutableListOf<List<String>>()
            lookupTableData.add(colNames)
            dbTableData.forEach { row ->
                val rowData = Json.parseToJsonElement(row.data.toString()).jsonObject
                val newTableRow = mutableListOf<String>()
                colNames.forEach { colName ->
                    newTableRow.add((rowData[colName] as JsonPrimitive).contentOrNull ?: "")
                }
                lookupTableData.add(newTableRow)
            }
            setTableData(lookupTableData)
            this.version = version
            logger.info("Loaded database lookup table $name with ${dbTableData.size} rows.")
        } catch (e: DataAccessException) {
            logger.error("There was an error loading the database lookup tables.", e)
            throw e
        }
        return this
    }
}