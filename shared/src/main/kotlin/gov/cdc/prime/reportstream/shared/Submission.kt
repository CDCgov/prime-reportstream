package gov.cdc.prime.reportstream.shared

import com.azure.data.tables.models.TableEntity

/**
 * Represents a submission entity to be stored in Azure Table Storage.
 *
 * @property submissionId The unique identifier for the submission, used as the PartitionKey.
 * @property status The status of the submission, used as the RowKey.
 * @property bodyURL The URL pointing to the body of the submission.
 * @property detail Optional additional details about the submission.
 */
data class Submission(
    val submissionId: String,
    val status: String,
    val bodyURL: String,
    val detail: String? = null,
) {
    companion object {
        /**
         * Creates a SubmissionEntity from a TableEntity.
         *
         * @param tableEntity The TableEntity to convert.
         * @return The corresponding SubmissionEntity.
         */
        fun fromTableEntity(tableEntity: TableEntity): Submission {
            return Submission(
                submissionId = tableEntity.partitionKey,
                status = tableEntity.rowKey,
                bodyURL = tableEntity.getProperty("body_url") as String,
                detail = tableEntity.getProperty("detail") as String?
            )
        }
    }

    /**
     * Converts this SubmissionEntity into an Azure TableEntity.
     * The PartitionKey is set to the submissionId, and the RowKey is set to the status.
     * Additional properties (bodyURL and detail) are included as part of the TableEntity.
     *
     * @return A TableEntity object that can be inserted into an Azure Table.
     */
    fun toTableEntity(): TableEntity {
        return TableEntity(submissionId, status)
            .setProperties(
            mapOf(
                "body_url" to bodyURL,
                "detail" to detail.takeIf { it != null }
            )
        )
    }
}