package gov.cdc.prime.router

import java.time.LocalDateTime

data class Message(
    val messageId: String,
    val sender: String,
    val submittedDate: LocalDateTime,
    val reportId: String
)