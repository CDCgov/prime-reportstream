package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

/**
 * Event definition for when a report gets routed to a receiver
 */
data class ReportRouteEvent(
    val parentReportId: ReportId,
    val reportId: ReportId,
    val topic: Topic,
    val sender: String,
    val receiver: String?,
    val observations: List<ObservationSummary>,
    val bundleSize: Int,
) : AzureCustomEvent