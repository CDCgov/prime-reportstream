package gov.cdc.prime.reportstream.submissions

import java.time.Instant
import java.util.UUID

data class SubmissionReceivedEvent(
    val timeStamp: Instant,
    val reportId: UUID,
    val parentReportId: UUID,
    val rootReportId: UUID,
    val headers: Map<String, String>,
    val sender: String,
    val senderIP: String,
    val fileSize: String,
    val blobUrl: String,
)