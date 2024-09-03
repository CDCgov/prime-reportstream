package gov.cdc.prime.router.azure

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableServiceClient
import com.azure.data.tables.TableServiceClientBuilder
import com.azure.data.tables.models.TableEntity
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

/**
 * Singleton class responsible for providing access to Azure Table Storage services.
 *
 * This class manages the connection to Azure Table Storage and provides methods to interact with
 * individual tables. The connection is established using the environment-specific connection string.
 */
class TableAccess : Logging {

    companion object {
        /**
         * The singleton instance of TableAccess, initialized lazily.
         * Ensures that the instance is created only when it is first accessed.
         */
        val instance: TableAccess by lazy { TableAccess() }

        private val defaultEnvVar = Environment.get().storageEnvVar

        /**
         * Retrieves the Azure Storage connection string from environment variables.
         *
         * @return The connection string for Azure Storage.
         */
        fun getConnectionString(): String = System.getenv(defaultEnvVar)
    }

    /**
     * Eagerly initialized TableServiceClient to interact with Azure Table Storage.
     * The client is created once during the instantiation of the TableAccess singleton and reused for all operations.
     */
    private fun tableServiceClient(): TableServiceClient = TableServiceClientBuilder()
        .connectionString(getConnectionString())
        .buildClient()

    /**
     * Inserts a TableEntity into the specified table.
     *
     * If the table does not exist, it is created before attempting to insert the entity.
     * Logs success or failure of the operation.
     *
     * @param tableName The name of the table where the entity will be inserted.
     * @param entity The TableEntity to be inserted into the table.
     */
    fun insertEntity(tableName: String, entity: TableEntity) {
        try {
            val tableClient = getOrCreateTableClient(tableName)
            tableClient.createEntity(entity)
            logger.info("Entity inserted successfully: ${entity.partitionKey} is ${entity.rowKey}")
        } catch (e: Exception) {
            logger.error("Failed to insert entity: ${entity.partitionKey} with ${entity.rowKey}", e)
        }
    }

    /**
     * Retrieves a TableEntity from the specified table.
     *
     * This method fetches a TableEntity from Azure Table Storage based on the given partition key and row key.
     * If the entity is found, it is returned. If the table does not exist or an error occurs during the retrieval,
     * the method logs the error and returns null.
     *
     * @param tableName The name of the table from which to retrieve the entity.
     * @param partitionKey The partition key identifying the entity.
     * @param rowKey The row key identifying the entity.
     * @return The TableEntity if found, or null if the table does not exist or an error occurs.
     */
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
     * If the table does not exist, this method returns null.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table, or null if the table does not exist.
     */
    private fun getTableClient(tableName: String): TableClient? {
        val tableExists = tableServiceClient().listTables().any { it.name == tableName }
        return if (tableExists) {
            tableServiceClient().getTableClient(tableName)
        } else {
            null
        }
    }

    /**
     * Retrieves a TableClient for the specified table, creating the table if it does not exist.
     *
     * If the table does not exist, it is created before returning the client.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table.
     */
    private fun getOrCreateTableClient(tableName: String): TableClient {
        val tableClient = getTableClient(tableName)
        return tableClient ?: run {
            tableServiceClient().createTable(tableName)
            tableServiceClient().getTableClient(tableName)
        }
    }
}