package gov.cdc.prime.router.azure.observability.bundleDigest

import gov.cdc.prime.router.azure.observability.event.ObservationSummary
import org.hl7.fhir.r4.model.Bundle

class BundleDigestExtractor(private val strategy: BundleDigestExtractorStrategy) {
    fun generateDigest(bundle: Bundle): BundleDigest = strategy.extract(bundle)
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
    val performerState: List<String>,
    val orderingFacilityState: List<String>,
    override val eventType: String,
) : BundleDigest