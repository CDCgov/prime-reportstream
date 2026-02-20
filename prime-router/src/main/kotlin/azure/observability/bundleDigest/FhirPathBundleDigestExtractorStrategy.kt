package gov.cdc.prime.router.azure.observability.bundleDigest

import gov.cdc.prime.fhirconverter.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.hl7.fhir.r4.model.Bundle

abstract class FhirPathBundleDigestExtractorStrategy(private val context: CustomContext?) :
    BundleDigestExtractorStrategy {
    internal fun getListOfFHIRValues(
        bundle: Bundle,
        path: String,
    ) = FhirPathUtils.evaluate(context, bundle, bundle, path)
        .filter { it.isPrimitive }
        .map { base -> FhirPathUtils.pathEngine.convertToString(base) }
}