package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
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

    /**
     * Returns a ReportSentEvent
     */
    fun createReportSentEvent(
        receiver: Receiver,
        reportFiles: List<ReportFile?>,
        reportId: ReportId,
        externalFilename: String,
    ): ReportSentEvent {
        val reportFile = if (reportFiles.size == 1) reportFiles.first() else null
        return ReportSentEvent(
            reportFile?.reportId,
            reportId,
            receiver.topic,
            Sender.createFullName(reportFile?.sendingOrg, reportFile?.sendingOrgClient),
            receiver.fullName,
            receiver.transport?.type,
            externalFilename
        )
    }
}