package gov.cdc.prime.router.azure.observability.bundleDigest

import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.hl7.fhir.r4.model.Bundle

class FhirPathBundleDigestLabResultExtractorStrategy(private val context: CustomContext? = null) :
    FhirPathBundleDigestExtractorStrategy(context) {

    private val eventCodePath = "Bundle.entry.resource.ofType(MessageHeader).event.display"
    private val patientStatePath = "Bundle.entry.resource.ofType(Patient).address.state"
    private val performerStatePath =
        "Bundle.entry.resource.ofType(ServiceRequest)[0].performer.resolve().address.state"
    private val orderingFacilityStatePath =
        "Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state"

    override fun extract(bundle: Bundle): BundleDigest {
        val patientStates = getListOfFHIRValues(bundle, patientStatePath)
        val performerStates = getListOfFHIRValues(bundle, performerStatePath)
        val orderingFacilityState = getListOfFHIRValues(bundle, orderingFacilityStatePath)
        val eventCode = FhirPathUtils.evaluateString(context, bundle, bundle, eventCodePath)

        val observationSummaries = AzureEventUtils.getObservationSummaries(bundle)

        return BundleDigestLabResult(
            observationSummaries,
            patientStates,
            performerStates,
            orderingFacilityState,
            eventCode
        )
    }
}