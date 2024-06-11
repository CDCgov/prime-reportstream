package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

/**
 * Event definition for when a report gets routed to a receiver
 */

data class ReportNotRoutedEvent(
    val reportId: ReportId,
    val parentReportId: ReportId,
    val submittedId: ReportId,
    val topic: Topic,
    val sender: String,
    val observations: List<ObservationSummary>,
    val bundleSize: Int,
    val messageId: AzureEventUtils.MessageID,
) : AzureCustomEvent