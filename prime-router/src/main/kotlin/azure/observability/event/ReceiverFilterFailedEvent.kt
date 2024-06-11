package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

/**
 * Event definition for when a report fails a receiver's filters
 */

data class ReceiverFilterFailedEvent(
    val reportId: ReportId,
    val parentReportId: ReportId,
    val submittedReportId: ReportId,
    val topic: Topic,
    val sender: String,
    val receiver: String,
    val observations: List<ObservationSummary>,
    val bundleSize: Int,
    val messageId: AzureEventUtils.MessageID,
) : AzureCustomEvent