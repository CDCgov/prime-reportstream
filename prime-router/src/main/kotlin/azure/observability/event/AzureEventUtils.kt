package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation

/**
 * Collection of Azure Event utility functions
 */
object AzureEventUtils {

    /**
     * Retrieves all observations from a bundle and maps them to a list of [ObservationSummary] each
     * containing a list of [TestSummary]
     */
    fun getObservationSummaries(bundle: Bundle): List<ObservationSummary> =
        getObservationSummaries(bundle.getObservations())

    /**
     * Retrieves all observations from a list of observations and maps them to a list of [ObservationSummary] each
     * containing a list of [TestSummary]
     */
    fun getObservationSummaries(
        observations: List<Observation>,
    ): List<ObservationSummary> = observations.map { observation ->
        ObservationSummary.fromObservation(observation)
    }

    /**
     * For tracking the Message ID, we need the bundle.identifier value and system.
     * In FHIR bundle.identifier is optional so one or both may not be present.
     */
    data class MessageID(val value: String? = null, val system: String? = null)

    /**
     * Returns the bundle identifier elements that relate to HL7v2 Message Header information for tracking
     */
    fun getIdentifier(bundle: Bundle): MessageID = MessageID(bundle.identifier?.value, bundle.identifier?.system)
}