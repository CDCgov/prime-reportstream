package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.reportstream.shared.Topic
import gov.cdc.prime.router.ReportId

/**
 * An event emitted during every report created
 */
data class ReportCreatedEvent(
    val reportId: ReportId,
    val topic: Topic,
) : AzureCustomEvent