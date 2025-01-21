package gov.cdc.prime.router.azure.observability.bundleDigest

import gov.cdc.prime.router.azure.observability.event.ObservationSummary
import gov.cdc.prime.router.azure.observability.event.OrderingFacilitySummary
import gov.cdc.prime.router.azure.observability.event.PerformerSummary
import org.hl7.fhir.r4.model.Bundle

class BundleDigestExtractor(private val strategy: BundleDigestExtractorStrategy) {
    fun generateDigest(bundle: Bundle): BundleDigest {
        return strategy.extract(bundle)
    }
}

interface BundleDigestExtractorStrategy {
    fun extract(bundle: Bundle): BundleDigest
}

interface BundleDigest {
    val eventType: String
}

data class BundleDigestLabResult(
    val observationSummaries: List<ObservationSummary>,
    val patientState: List<String>,
    val performerState: List<PerformerSummary>,
    val orderingFacilityState: List<OrderingFacilitySummary>,
    override val eventType: String,
) : BundleDigest