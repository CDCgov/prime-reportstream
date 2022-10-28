package gov.cdc.prime.router.messageTracker

import java.time.LocalDateTime

data class Message(
    val id: Long,
    val messageId: String,
    val sender: String,
    val submittedDate: LocalDateTime,
    val reportId: String,
    val fileName: String?,
    val fileUrl: String?,
    val warnings: List<MessageActionLog>? = emptyList(),
    val errors: List<MessageActionLog>? = emptyList(),
    val receiverData: List<MessageReceiver>? = emptyList()
)

data class MessageReceiver(
    val reportId: String,
    val receivingOrg: String,
    val receivingOrgSvc: String,
    val transportResult: String?,
    val fileName: String?,
    val fileUrl: String?,
    val createdAt: LocalDateTime,
    val qualityFilters: List<MessageActionLog>? = emptyList()
)