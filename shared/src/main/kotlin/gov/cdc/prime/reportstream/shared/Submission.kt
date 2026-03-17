package gov.cdc.prime.reportstream.shared

import com.azure.data.tables.models.TableEntity
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.OffsetDateTime

/**
 * Represents a submission entity to be stored in Azure Table Storage.
 *
 * @property submissionId The unique identifier for the submission, used as the PartitionKey.
 * @property status The status of the submission, used as the RowKey.
 * @property bodyURL The URL pointing to the body of the submission.
 * @property detail Optional additional details about the submission.
 */
@JsonPropertyOrder(value = ["submissionId", "overallStatus", "timestamp"])
data class Submission(
    val submissionId: String,
    @JsonProperty("overallStatus")
    val status: String,
    @JsonIgnore
    val bodyURL: String,
    @JsonIgnore
    val detail: String? = null,
    val timestamp: OffsetDateTime? = null,
) {
    companion object {
        /**
         * Creates a SubmissionEntity from a TableEntity.
         *
         * @param tableEntity The TableEntity to convert.
         * @return The corresponding SubmissionEntity.
         */
        fun fromTableEntity(tableEntity: TableEntity): Submission = Submission(
                submissionId = tableEntity.partitionKey,
                status = tableEntity.rowKey,
                bodyURL = tableEntity.getProperty("body_url") as String,
                detail = tableEntity.getProperty("detail") as String?,
                timestamp = tableEntity.timestamp
            )
    }

    /**
     * Converts this SubmissionEntity into an Azure TableEntity.
     * The PartitionKey is set to the submissionId, and the RowKey is set to the status.
     * Additional properties (bodyURL and detail) are included as part of the TableEntity.
     *
     * @return A TableEntity object that can be inserted into an Azure Table.
     */
    fun toTableEntity(): TableEntity = TableEntity(submissionId, status)
            .setProperties(
            mapOf(
                "body_url" to bodyURL,
                "detail" to detail.takeIf { it != null }
            )
        )
}