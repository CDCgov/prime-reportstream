package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.reportstream.shared.Topic
import gov.cdc.prime.router.ReportId

/**
 * Event definition for when a report gets routed to a receiver
 */
data class ReportRouteEvent(
    val reportId: ReportId,
    val parentReportId: ReportId,
    val submittedReportId: ReportId,
    val topic: Topic,
    val sender: String,
    val receiver: String?, // TODO: this should not be nullable anymore after #14450
    val observations: List<ObservationSummary>,
    val filteredObservations: List<ObservationSummary>,
    val bundleSize: Int,
    val messageId: AzureEventUtils.MessageID,
) : AzureCustomEvent