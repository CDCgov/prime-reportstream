package gov.cdc.prime.router.azure

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableClientBuilder
import com.azure.data.tables.models.TableEntity
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

class TableAccess : Logging {

    private val tableName = "submissions"
    private val connectionString = System.getenv(Environment.get().blobEnvVar)

    private val tableClient: TableClient = TableClientBuilder()
        .connectionString(connectionString)
        .tableName(tableName)
        .buildClient()

    /**
     * Updates multiple fields of an existing entity in the Azure Table.
     *
     * This method retrieves the entity identified by the given partitionKey and rowKey,
     * updates the specified fields with the new values, and saves the entity back to the table.
     *
     * @param partitionKey The PartitionKey of the entity to update.
     * @param rowKey The RowKey of the entity to update.
     * @param updates A map of field names to their new values.
     */
    fun updateMultipleFields(partitionKey: String, rowKey: String, updates: Map<String, Any>) {
        try {
            // Retrieve the entity
            val entity = tableClient.getEntity(partitionKey, rowKey)

            // Update the desired fields
            updates.forEach { (fieldName, fieldValue) ->
                entity.properties[fieldName] = fieldValue
            }

            // Save the updated entity back to the table
            tableClient.updateEntity(entity)

            logger.info("Fields updated successfully for entity: $rowKey")
        } catch (e: Exception) {
            logger.error("Failed to update fields for entity: $rowKey", e)
        }
    }

    /**
     * Inserts a new entity into the Azure Table.
     *
     * This method creates a new entity in the specified table. The entity must have a unique
     * PartitionKey and RowKey.
     *
     * @param entity The TableEntity to insert. It must include the PartitionKey and RowKey
     * identifying the entity to insert.
     */
    fun insertEntity(entity: TableEntity) {
        try {
            tableClient.createEntity(entity)
            logger.info("Entity inserted successfully: ${entity.rowKey}")
        } catch (e: Exception) {
            logger.error("Failed to insert entity: ${entity.rowKey}", e)
        }
    }
}