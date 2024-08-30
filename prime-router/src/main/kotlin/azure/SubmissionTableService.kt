package gov.cdc.prime.router.azure

import com.azure.data.tables.TableClient
import gov.cdc.prime.reportstream.shared.SubmissionEntity
import org.apache.logging.log4j.kotlin.Logging

/**
 * Service class responsible for handling operations related to the "submission" table in Azure Table Storage.
 *
 * This service uses the TableAccess singleton to interact with Azure Table Storage.
 */
class SubmissionTableService private constructor() : Logging {

    companion object {
        /**
         * The singleton instance of SubmissionService, initialized lazily.
         */
        val instance: SubmissionTableService by lazy { SubmissionTableService() }
    }

    private val tableName = "submission"

    /**
     * Lazily initialized TableClient to interact with the "submission" table.
     * The client is created once and reused for all operations on the "submission" table.
     */
    private val tableAccess: TableAccess by lazy { TableAccess.instance }
    private val tableClient: TableClient by lazy { tableAccess.getTableClient("submission") }

    /**
     * Inserts a SubmissionEntity into the "submission" table.
     *
     * Converts the provided SubmissionEntity into a TableEntity and attempts to insert it into the table.
     * Logs success or failure of the operation.
     *
     * @param submission The SubmissionEntity to be inserted into the table.
     */
    fun insertSubmission(submission: SubmissionEntity) {
        try {
            val entity = submission.toTableEntity()
            tableAccess.insertTableEntity(tableName, entity)
            logger
                .info(
                "Submission entity insert succeeded: ${submission.submissionId} with status ${submission.status}"
                )
        } catch (e: Exception) {
            logger
                .error(
                "Submission entity insert failed: ${submission.submissionId} with status ${submission.status}",
                e
                )
        }
    }

    /**
     * Reads a submission entity from the "submission" table.
     *
     * @param partitionKey The partition key of the entity.
     * @param rowKey The row key of the entity.
     * @return The SubmissionEntity if found, otherwise null.
     */
    fun readSubmissionEntity(partitionKey: String, rowKey: String): SubmissionEntity? = try {
            val tableEntity = tableClient.getEntity(partitionKey, rowKey)
            SubmissionEntity.fromTableEntity(tableEntity)
        } catch (e: Exception) {
            logger
                .error(
                    "Failed to read submission entity: $partitionKey with status $rowKey",
                    e
                )
            null
        }
}