package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.reportstream.shared.Topic
import gov.cdc.prime.router.ReportId

/**
 * Event definition for when a report is ready to be processed per receiver
 *
 * This event should contain all observations sent by the sender since no
 * receiver specific filters have been run
 */
data class ReportAcceptedEvent(
    val reportId: ReportId,
    val submittedReportId: ReportId,
    val topic: Topic,
    val sender: String,
    val observations: List<ObservationSummary>,
    val bundleSize: Int,
    val messageId: AzureEventUtils.MessageID,
) : AzureCustomEvent