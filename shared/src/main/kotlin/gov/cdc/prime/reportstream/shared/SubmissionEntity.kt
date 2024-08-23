package gov.cdc.prime.reportstream.shared

import com.azure.data.tables.models.TableEntity

/**
 *
 */
data class SubmissionEntity(
    val submissionId: String,
    val status: String,
    val body_url: String,
    val detail: String? = null,
) {
    /**
     * TableEntity() sets PartitionKey and RowKey. Both are required by azure and combine to create the PK
     * submissionId identifies the partition. Within that partition status should unique
     */
    private val tableEntity = TableEntity(submissionId, status)
        .setProperties(mapOf("body_url" to body_url, "detail" to detail))

    /**
     *
     */
    fun toTableEntity(): TableEntity = tableEntity
}