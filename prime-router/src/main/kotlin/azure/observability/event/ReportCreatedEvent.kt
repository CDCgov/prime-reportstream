package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

/**
 * An event emitted during every report created
 */
data class ReportCreatedEvent(
    val reportId: ReportId,
    val topic: Topic,
) : AzureCustomEvent