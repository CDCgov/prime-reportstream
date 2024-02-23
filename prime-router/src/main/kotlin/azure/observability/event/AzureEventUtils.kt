package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.fhirengine.utils.getAllMappedConditions
import org.hl7.fhir.r4.model.Bundle

/**
 * Collection of Azure Event utility functions
 */
object AzureEventUtils {

    /**
     * Retrieves all mapped conditions from a [Bundle] and converts them to a [ConditionSummary]
     */
    fun getConditions(bundle: Bundle): List<ConditionSummary> {
        return bundle
            .getAllMappedConditions()
            .map(ConditionSummary::fromCoding)
    }
}