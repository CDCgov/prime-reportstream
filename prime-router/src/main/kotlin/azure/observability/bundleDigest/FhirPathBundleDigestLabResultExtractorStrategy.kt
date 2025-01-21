package gov.cdc.prime.router.azure.observability.bundleDigest

import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.OrderingFacilitySummary
import gov.cdc.prime.router.azure.observability.event.PerformerSummary
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.hl7.fhir.r4.model.Bundle

class FhirPathBundleDigestLabResultExtractorStrategy(
    private val context: CustomContext? = null,
) : FhirPathBundleDigestExtractorStrategy(context) {

    private val eventCodePath = "Bundle.entry.resource.ofType(MessageHeader).event.display"
    private val patientStatePath = "Bundle.entry.resource.ofType(Patient).address.state"

    // The original strategy to collect the performerStates and orderingFacilityStates was to collect an
    // aggregate list using the fhirpath below. This strategy needs to change when retrieving the associated
    // names and CLIA as well. Instead, we will collect a list of performer and requester references, resolve
    // them to DomainResource objects, and use those to build lists of PerformerSummary and OrderingFacilitySummary
    // objects.

    //private val performerStatePath =
    //    "Bundle.entry.resource.ofType(ServiceRequest)[0].performer.resolve().address.state"
    //private val orderingFacilityStatePath =
    //    "Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state"
    private val performerPath =
        "Bundle.entry.resource.ofType(ServiceRequest)[0].performer.resolve()"
    private val orderingFacilityPath =
        "Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve()"

    override fun extract(bundle: Bundle): BundleDigest {
        val patientStates = getListOfFHIRValues(bundle, patientStatePath)
        //val performerStates = getListOfFHIRValues(bundle, performerStatePath)
        //val orderingFacilityState = getListOfFHIRValues(bundle, orderingFacilityStatePath)
        val performerSummaries = FhirPathUtils.evaluate(context, bundle, bundle, performerPath)
            .map { PerformerSummary.fromPerformer(it) }
        val orderingFacilitySummaries = FhirPathUtils.evaluate(context, bundle, bundle, orderingFacilityPath)
            .map { OrderingFacilitySummary.fromOrganization(it) }
        val eventCode = FhirPathUtils.evaluateString(context, bundle, bundle, eventCodePath)

        val observationSummaries = AzureEventUtils.getObservationSummaries(bundle)

        return BundleDigestLabResult(
            observationSummaries,
            patientStates,
            //performerStates,
            //orderingFacilityState,
            performerSummaries,
            orderingFacilitySummaries,
            eventCode
        )
    }
}