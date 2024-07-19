package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigest

class ItemNotRoutedEvent(
    override val itemEventData: ItemEventData,
    override val reportEventData: ReportEventData,
    val bundleDigest: BundleDigest,
) : IItemEvent, IReportEvent