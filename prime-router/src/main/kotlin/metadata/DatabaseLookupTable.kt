package gov.cdc.prime.router.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Preconditions
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
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
     * Mapper to convert objects to JSON.
     */
    private val mapper: ObjectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

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
                val rowData = mapper.readValue<Map<String, String>>(row.data.data())
                lookupTableData.add(colNames.map { rowData[it] ?: "" })
            }
            setTableData(lookupTableData)
            this.version = version
            logger.info("Loaded database lookup table $name version $version with ${dbTableData.size} rows.")
        } catch (e: DataAccessException) {
            logger.error("There was an error loading the database lookup tables.", e)
            throw e
        }
        return this
    }
}