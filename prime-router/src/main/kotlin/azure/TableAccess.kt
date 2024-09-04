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
 * The TableServiceClient is lazily initialized and can be re-initialized upon failures.
 */
class TableAccess : Logging {

    companion object {
        /**
         * Singleton instance of TableAccess, initialized lazily.
         * Ensures that the instance is created only when it is first accessed.
         */
        val instance: TableAccess by lazy { TableAccess() }

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

    /**
     * The TableServiceClient used to interact with Azure Table Storage.
     * It is volatile to ensure visibility across threads and lazily initialized when accessed.
     */
    @Volatile
    private var tableServiceClient: TableServiceClient? = null

    /**
     * Lazily retrieves the TableServiceClient, initializing it if necessary.
     *
     * The method is synchronized to ensure thread safety and avoid race conditions during initialization.
     *
     * @return The initialized TableServiceClient instance.
     */
    private fun getTableServiceClient(): TableServiceClient {
        return synchronized(this) {
            tableServiceClient ?: initializeClient(getConnectionString()).also { tableServiceClient = it }
        }
    }

    /**
     * Initializes the TableServiceClient using the provided connection string.
     *
     * @param connectionString The connection string used to create the TableServiceClient.
     * @return The initialized TableServiceClient instance.
     */
    private fun initializeClient(connectionString: String): TableServiceClient {
        return TableServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
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
     * This method fetches a TableEntity based on the given partition key and row key.
     * If the table does not exist or an error occurs during the retrieval, the method logs the error and returns null.
     *
     * @param tableName The name of the table from which to retrieve the entity.
     * @param partitionKey The partition key identifying the entity.
     * @param rowKey The row key identifying the entity.
     * @return The TableEntity if found, or null if an error occurs.
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
     * This method checks if the table exists. If the table exists, it returns a TableClient for interacting with it.
     * If the table does not exist or an error occurs, it logs the error and attempts to retry the operation after reinitializing the client.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table, or null if the table does not exist or an error occurs.
     */
    private fun getTableClient(tableName: String): TableClient? {
        try {
            val tableExists = getTableServiceClient().listTables().any { it.name == tableName }
            return if (tableExists) {
                getTableServiceClient().getTableClient(tableName)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting table service client", e)
            handleClientFailure()
            return retryGetTableClient(tableName)
        }
    }

    /**
     * Retries retrieving a TableClient after the client has been reinitialized due to a failure.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table, or null if the table does not exist.
     */
    private fun retryGetTableClient(tableName: String): TableClient? {
        val tableExists = getTableServiceClient().listTables().any { it.name == tableName }
        return if (tableExists) {
            getTableServiceClient().getTableClient(tableName)
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
            getTableServiceClient().createTable(tableName)
            getTableServiceClient().getTableClient(tableName)
        }
    }

    /**
     * Handles client failures by reinitializing the TableServiceClient.
     *
     * This method is invoked when an error occurs during table operations. It ensures that the TableServiceClient
     * is reinitialized to recover from connection or channel-related failures.
     */
    private fun handleClientFailure() {
        logger.warn("Detected client failure, reinitializing TableServiceClient...")
        synchronized(this) {
            tableServiceClient = initializeClient(getConnectionString()) // Reinitialize the client
        }
    }
}