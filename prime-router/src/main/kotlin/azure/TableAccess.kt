package gov.cdc.prime.router.azure

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableClientBuilder
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
    }

    private val defaultEnvVar = Environment.get().storageEnvVar

    /**
     * Retrieves the Azure Storage connection string from environment variables.
     *
     * @return The connection string for Azure Storage.
     */
    fun getConnectionString(): String = System.getenv(defaultEnvVar)

    /**
     * Lazily initialized TableServiceClient to interact with Azure Table Storage.
     * The client is created once and reused for all operations across different tables.
     */
    private val tableServiceClient: TableServiceClient by lazy {
        TableServiceClientBuilder()
            .connectionString(getConnectionString())
            .buildClient()
    }

    /**
     * Inserts a TableEntity into the specified table.
     *
     * If the table does not exist, it is created before attempting to insert the entity.
     * Logs success or failure of the operation.
     *
     * @param tableName The name of the table where the entity will be inserted.
     * @param entity The TableEntity to be inserted into the table.
     */
    fun insertTableEntity(tableName: String, entity: TableEntity) {
        try {
            getTableClient(tableName).createEntity(entity)
            logger.info("Entity inserted successfully: ${entity.partitionKey} is ${entity.rowKey}")
        } catch (e: Exception) {
            logger.error("Failed to insert entity: ${entity.partitionKey} with ${entity.rowKey}", e)
        }
    }

    /**
     * Retrieves a TableClient for the specified table.
     *
     * If the table does not exist, it is created before returning the client.
     *
     * @param tableName The name of the table for which the client is needed.
     * @return A TableClient for interacting with the specified table.
     */
    fun getTableClient(tableName: String): TableClient {
        val tableExists = tableServiceClient.listTables().any { it.name == tableName }
        if (!tableExists) {
            tableServiceClient.createTable(tableName)
        }

        return TableClientBuilder()
            .connectionString(getConnectionString())
            .tableName(tableName)
            .buildClient()
    }
}