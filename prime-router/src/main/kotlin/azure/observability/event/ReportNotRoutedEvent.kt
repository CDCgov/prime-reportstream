package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.reportstream.shared.Topic
import gov.cdc.prime.router.ReportId

/**
 * Event definition for when a report does not get routed to any receivers
 */

data class ReportNotRoutedEvent(
    val reportId: ReportId,
    val parentReportId: ReportId,
    val submittedReportId: ReportId,
    val topic: Topic,
    val sender: String,
    val bundleSize: Int,
    val messageId: AzureEventUtils.MessageID,
) : AzureCustomEvent