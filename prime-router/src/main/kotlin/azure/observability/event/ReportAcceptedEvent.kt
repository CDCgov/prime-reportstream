package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

/**
 * Event definition for when a report is ready to be processed per receiver
 *
 * This event should contain all conditions sent by the sender since no
 * receiver specific filters have been run
 */
data class ReportAcceptedEvent(
    val reportId: ReportId,
    val topic: Topic,
    val sender: String,
    val conditions: List<ConditionSummary>,
) : AzureCustomEvent {
    // extract out conditions count into top level custom dimension for easier querying
    val conditionsCount = conditions.size
}