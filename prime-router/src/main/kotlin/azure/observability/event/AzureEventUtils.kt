package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.fhirengine.utils.getMappedConditions
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Bundle

/**
 * Collection of Azure Event utility functions
 */
object AzureEventUtils {

    /**
     * Retrieves all observations from a bundle and maps them to a list of [ObservationSummary] each
     * containing a list of [ConditionSummary]
     */
    fun getObservations(bundle: Bundle): List<ObservationSummary> {
        return bundle.getObservations().map { observation ->
            val conditions = observation
                .getMappedConditions()
                .map(ConditionSummary::fromCoding)
            ObservationSummary(conditions)
        }
    }
}