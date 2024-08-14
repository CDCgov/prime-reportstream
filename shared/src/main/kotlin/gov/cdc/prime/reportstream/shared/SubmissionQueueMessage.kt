package gov.cdc.prime.reportstream.shared

import java.util.UUID

data class SubmissionQueueMessage(val reportId: UUID, val blobUrl: String, val headers: Map<String, String>)