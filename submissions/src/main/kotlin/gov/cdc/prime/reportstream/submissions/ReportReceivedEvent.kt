package gov.cdc.prime.reportstream.submissions

import java.time.OffsetDateTime
import java.util.UUID

data class ReportReceivedEvent(
    val timeStamp: OffsetDateTime,
    val reportId: UUID,
    val parentReportId: UUID,
    val rootReportId: UUID,
    val headers: Map<String, String>,
    val sender: String,
    val senderIP: String,
    val fileSize: String,
    val blobUrl: String,
)