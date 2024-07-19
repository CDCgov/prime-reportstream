package gov.cdc.prime.router.azure.observability.event

class ItemRoutedEvent(
    override val itemEventData: ItemEventData,
    override val reportEventData: ReportEventData,
    val receiver: String,
) : IItemEvent, IReportEvent