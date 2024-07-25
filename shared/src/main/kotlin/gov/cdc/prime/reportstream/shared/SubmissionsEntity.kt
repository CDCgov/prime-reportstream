package gov.cdc.prime.reportstream.shared

import com.azure.data.tables.models.TableEntity

data class SubmissionsEntity(
    val reportReceivedTime: String,
    val reportId: String,
    val status: String,
) {
    // TableEntity() sets PartitionKey and RowKey. Both are required by azure and combine to create the PK
    // reportId identifies the partition. Within that partition status should unique
    val tableEntity = TableEntity(reportId, reportId)
    val tableProperties = mapOf(
        "report_received_time" to reportReceivedTime,
        "report_accepted_time" to reportReceivedTime, // Will be updated when the report is accepted
        "report_id" to reportId,
        "status" to status
    )

    fun toTableEntity(): TableEntity {
        return tableEntity.setProperties(tableProperties)
    }
}