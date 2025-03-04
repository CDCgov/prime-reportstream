package gov.cdc.prime.router.azure

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableServiceClient
import com.azure.data.tables.TableServiceClientBuilder
import com.azure.data.tables.models.TableEntity
import com.azure.data.tables.models.TableServiceException
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

/**
 * Singleton class responsible for providing access to Azure Table Storage services.
 *
 * This class manages the connection to Azure Table Storage and provides methods to interact with
 * individual tables. The connection is established using the environment-specific connection string.
 * The TableServiceClient is lazily initialized and can be re-initialized upon failures.
 */
class TableAccess : Logging {

    companion object {

        /**
         * The environment variable that stores the connection string.
         */
        private val defaultEnvVar = Environment.get().storageEnvVar

        /**
         * Retrieves the Azure Storage connection string from environment variables.
         *
         * @return The connection string for Azure Storage.
         */
        fun getConnectionString(): String = System.getenv(defaultEnvVar)
    }

    private var tableServiceClient: TableServiceClient = buildClient()

    private fun buildClient(): TableServiceClient = TableServiceClientBuilder()
            .connectionString(getConnectionString())
            .buildClient()

    fun reset() {
        tableServiceClient = buildClient()
    }

    /**
     * Inserts a TableEntity into the specified table.
     *
     * If the table does not exist, it is created before inserting the entity.
     * Logs the success or failure of the operation.
     *
     * @param tableName The name of the table where the entity will be inserted.
     * @param entity The TableEntity to be inserted.
     */
    @Synchronized
    fun insertEntity(tableName: String, entity: TableEntity) {
        try {
            val tableClient = getOrCreateTableClient(tableName)
            tableClient.createEntity(entity)
            logger.info("Entity inserted successfully: ${entity.partitionKey} is ${entity.rowKey}")
        } catch (e: TableServiceException) {
            // Log the detailed error
            logger.error("Failed to insert entity: ${entity.partitionKey} with ${entity.rowKey}", e)
        }
    }

    /**
     * Retrieves a TableEntity from the specified table.
     *
     * This method fetches a TableEntity based on the given partition key and row key.
     * If the table does not exist or an error occurs during the retrieval, the method logs the error and returns null.
     *
     * @param tableName The name of the table from which to retrieve the entity.
     * @param partitionKey The partition key identifying the entity.
     * @param rowKey The row key identifying the entity.
     * @return The TableEntity if found, or null if an error occurs.
     */
    @Synchronized
    fun getEntity(tableName: String, partitionKey: String, rowKey: String): TableEntity? {
        try {
            val tableClient = getTableClient(tableName)
            return tableClient?.getEntity(partitionKey, rowKey)
        } catch (e: Exception) {
            logger.error("Failed to find entity: $partitionKey with $rowKey", e)
            return null
        }
    }

    /**
     * Retrieves a TableClient for the specified table if it exists.
     *
     * This method checks if the table exists. If the table exists, it returns a TableClient for interacting with it.
     * If the table does not exist or an error occurs, it logs the error and attempts to retry the operation after reinitializing the client.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table, or null if the table does not exist or an error occurs.
     */
    private fun getTableClient(tableName: String): TableClient? {
        val tableExists = tableServiceClient.listTables().any { it.name == tableName }
        return if (tableExists) {
            tableServiceClient.getTableClient(tableName)
        } else {
            null
        }
    }

    /**
     * Retrieves or creates a TableClient for the specified table.
     *
     * If the table does not exist, it is created, and a TableClient is returned for the newly created table.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table.
     */
    private fun getOrCreateTableClient(tableName: String): TableClient {
        val tableClient = getTableClient(tableName)
        return tableClient ?: run {
            tableServiceClient.createTable(tableName)
            tableServiceClient.getTableClient(tableName)
        }
    }
}