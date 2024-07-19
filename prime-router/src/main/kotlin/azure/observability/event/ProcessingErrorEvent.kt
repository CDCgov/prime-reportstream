package gov.cdc.prime.router.azure.observability.event

class ProcessingErrorEvent(
    override val reportEventData: ReportEventData,
    val message: String,
) : IReportEvent