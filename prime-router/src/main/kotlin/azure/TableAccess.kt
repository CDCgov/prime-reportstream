package gov.cdc.prime.router.azure

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableClientBuilder
import com.azure.data.tables.TableServiceClient
import com.azure.data.tables.TableServiceClientBuilder
import com.azure.data.tables.models.TableEntity
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

open class TableAccess : Logging {

    companion object {
        private val defaultEnvVar = Environment.get().storageEnvVar

        fun getConnectionString(): String = System.getenv(defaultEnvVar)
    }

    private val tableServiceClient: TableServiceClient by lazy {
        TableServiceClientBuilder()
            .connectionString(getConnectionString())
            .buildClient()
    }

    fun insertTableEntity(tableName: String, entity: TableEntity) {
        try {
            getTableClient(tableName).createEntity(entity)
            logger.info("Entity inserted successfully: ${entity.partitionKey} is ${entity.rowKey}")
        } catch (e: Exception) {
            logger.error("Failed to insert entity: ${entity.partitionKey} with ${entity.rowKey}", e)
        }
    }

    private fun getTableClient(tableName: String): TableClient {
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

class SubmissionTableService : TableAccess() {

    fun insertTableEntity(entity: TableEntity) {
        super.insertTableEntity("submission", entity)
    }
}