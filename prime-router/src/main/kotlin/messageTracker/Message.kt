package gov.cdc.prime.router.messageTracker

import java.time.LocalDateTime

data class Message(
    val id: Long,
    val messageId: String,
    val sender: String,
    val submittedDate: LocalDateTime,
    val reportId: String,
    val reportFileName: String?,
    val reportFileUrl: String?,
    val warnings: List<MessageActionLog>? = emptyList(),
    val errors: List<MessageActionLog>? = emptyList()
)