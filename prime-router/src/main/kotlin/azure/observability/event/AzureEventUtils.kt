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
            // Note: Current use case for ELR expects that there will only be one coding per observation.
            // If there is more than one, get mapped conditions will pull the mapped conditions from all of them.
            val conditions = observation
                .getMappedConditions()
                .map(ConditionSummary::fromCoding)
            val code = observation.code.codingFirstRep.code ?: "Unknown"
            val display = observation.code.codingFirstRep.display ?: "Unknown"
            ObservationSummary(conditions, code, display)
        }
    }

    /**
     * For tracking the Message ID, we need the bundle.identifier value and system.
     * In FHIR bundle.identifier is optional so one or both may not be present.
     */
    data class MessageID(val value: String? = null, val system: String? = null)

    /**
     * Returns the bundle identifier elements that relate to HL7v2 Message Header information for tracking
     */
    fun getIdentifier(bundle: Bundle): MessageID {
        return MessageID(bundle.identifier?.value, bundle.identifier?.system)
    }
}