package gov.cdc.prime.reportstream.shared

import com.azure.data.tables.models.TableEntity

data class SubmissionsEntity(
    val reportId: String,
    val status: String,
) {
    // TableEntity() sets PartitionKey and RowKey. Both are required by azure and combine to create the PK
    // reportId identifies the partition. Within that partition status should unique
    val tableEntity = TableEntity(reportId, status)

    fun toTableEntity(): TableEntity {
        return tableEntity
    }
}