package gov.cdc.prime.router.messages

import gov.cdc.prime.router.Topic

/**
 * The report content message contains the content of a report that is kept in the service. This message
 * is closely aligned with the report_file table.
 *
 *  [reportId] is the UUID of the report.
 *  [schemaTopic] is the topic of the pipeline
 *  [schemaName] refers to the schema of the report
 *  [contentType] is the MIME content name. Usually,
 *  [content] is a JSON escaped string of the
 *  [origin] provides information about blob that the content came from
 *  [request] provides information about the request that created the message.
 */
class ReportFileMessage(
    val reportId: String,
    val schemaTopic: Topic,
    val schemaName: String,
    val contentType: String,
    val content: String,
    val origin: Origin? = null,
    val request: Request? = null,
) {
    data class Origin(
        val bodyUrl: String = "",
        val sendingOrg: String = "",
        val sendingOrgClient: String = "",
        val receivingOrg: String = "",
        val receivingOrgSvc: String = "",
        val indices: List<Int> = emptyList(),
        val createdAt: String = "",
    )

    data class Request(
        val reportId: String = "",
        val receiverReportIndices: List<Int>? = emptyList(),
        val messageID: String = "",
        val senderReportIndices: Int?,
    )
}