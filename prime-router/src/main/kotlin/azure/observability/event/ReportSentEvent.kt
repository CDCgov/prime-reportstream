package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

/**
 * Event definition for when a report has been successfully sent to a receiver.
 * [trasnportType] is nullable as some receivers will purposefully not have transports configured
 */
data class ReportSentEvent(
    val sentReportId: ReportId?,
    val reportId: ReportId,
    val topic: Topic,
    val senderName: String?,
    val receiverName: String,
    val transportType: String?,
    val externalFilename: String,
) : AzureCustomEvent