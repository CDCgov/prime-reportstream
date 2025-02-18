package gov.cdc.prime.reportstream.submissions

import java.time.Instant
import java.util.UUID

data class SubmissionReceivedEvent(
    val timeStamp: Instant,
    val reportId: UUID,
    val parentReportId: UUID,
    val rootReportId: UUID,
    val requestParameters: SubmissionDetails,
    val method: String,
    val url: String,
    val senderName: String,
    val senderIp: String,
    val fileLength: String,
    val blobUrl: String,
    val pipelineStepName: String,
)

data class SubmissionDetails(val headers: Map<String, String>, val queryParameters: Map<String, List<String>>)