package gov.cdc.prime.router.azure

import gov.cdc.prime.reportstream.shared.Submission
import org.apache.logging.log4j.kotlin.Logging

/**
 * Service class responsible for handling operations related to the "submission" table in Azure Table Storage.
 *
 * This service uses the TableAccess singleton to interact with Azure Table Storage for inserting and retrieving
 * submissions in the "submission" table.
 *
 * It allows inserting `Submission` objects as table entities and retrieving them based on the submission ID and status.
 */
class SubmissionTableService private constructor() : Logging {

    companion object {
        /**
         * The singleton instance of SubmissionTableService, initialized lazily.
         *
         * This ensures that the instance is created only when it is first accessed,
         * providing thread-safe, lazy initialization.
         */
        val singletonInstance: SubmissionTableService by lazy {
            SubmissionTableService()
        }

        fun getInstance(): SubmissionTableService = singletonInstance
    }

    private val tableName = "submission"

    /**
     * The `TableAccess` object used to interact with Azure Table Storage.
     *
     * This is marked as `@Volatile` to ensure thread visibility. It is initialized lazily
     * and resettable to allow refreshing the connection if necessary.
     */
    private var tableAccess: TableAccess = TableAccess()

    /**
     * Resets the `TableAccess` instance used to interact with the "submission" table.
     *
     * This method ensures thread safety by synchronizing access to `tableAccess`, preventing multiple
     * threads from resetting the instance at the same time.
     */
    fun reset() {
        synchronized(this) {
            tableAccess.reset() // Re-initialize the TableAccess client
        }
    }

    /**
     * Inserts a SubmissionEntity into the "submission" table in Azure Table Storage.
     *
     * Converts the provided `Submission` object into a `TableEntity` and inserts it into the table.
     * Logs the outcome of the operation (success or failure).
     *
     * @param submission The `Submission` object to be inserted into the table.
     */
    fun insertSubmission(submission: Submission) {
        try {
            // Convert Submission to TableEntity and insert into the table
            val entity = submission.toTableEntity()
            tableAccess.insertEntity(tableName, entity)
            logger.info(
                "Submission entity insert succeeded: ${submission.submissionId} with status ${submission.status}"
            )
        } catch (e: Exception) {
            // Log the error if insertion fails
            logger.error(
                "Submission entity insert failed: ${submission.submissionId} with status ${submission.status}",
                e
            )
        }
    }

    /**
     * Retrieves a SubmissionEntity from the "submission" table based on submission ID and status.
     *
     * Fetches the corresponding table entity from Azure Table Storage using the provided partition key
     * (submission ID) and row key (status). If the entity is found, it is converted back into a `Submission` object.
     *
     * If the entity is not found, or if an error occurs during retrieval, the method returns `null` and logs the error.
     *
     * @param submissionID The partition key representing the submission ID.
     * @param status The row key representing the status of the submission.
     * @return The `Submission` object if found, otherwise `null`.
     */
    fun getSubmission(submissionID: String, status: String): Submission? = try {
        // Retrieve the TableEntity and convert it back to a Submission object if found
        val tableEntity = tableAccess.getEntity(tableName, submissionID, status)
        if (tableEntity != null) {
            Submission.fromTableEntity(tableEntity)
        } else {
            null
        }
    } catch (e: Exception) {
        // Log the error if retrieval fails and return null
        logger.error("Failed to read submission entity: $submissionID with status $status", e)
        null
    }
}