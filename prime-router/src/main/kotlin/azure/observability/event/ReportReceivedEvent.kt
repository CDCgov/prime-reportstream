package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

data class ReportReceivedEvent(
    val reportId: ReportId,
    val topic: Topic,
    val format: String,
    val senderName: String,
    val payload: String?,
) : AzureCustomEvent