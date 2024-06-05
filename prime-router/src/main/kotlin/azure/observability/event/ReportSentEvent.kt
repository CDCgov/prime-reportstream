package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic

data class ReportSentEvent(
    val sentReportId: ReportId,
    val reportId: ReportId,
    val topic: Topic,
    val sender: String,
    val receiverName: String,
    val transportType: String?,
    val externalFilename: String,
) : AzureCustomEvent