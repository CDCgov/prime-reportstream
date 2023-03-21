package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.deleteResource
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.getResourceReferences
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7MessageHelpers
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Provenance

/**
 * Translate a full-ELR FHIR message into the formats needed by any receivers from the route step
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRTranslator(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    queue: QueueAccess = QueueAccess
) : FHIREngine(metadata, settings, db, blob, queue) {

    /**
     * Accepts a FHIR [message], parses it, and generates translated output files for each item in the destinations
     *  element.
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory
    ) {
        logger.trace("Translating FHIR file for receivers.")
        try {
            // pull fhir document and parse FHIR document
            val bundle = FhirTranscoder.decode(message.downloadContent())

            // track input report
            actionHistory.trackExistingInputReport(message.reportId)

            val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
            val receiverEndpoints = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()

            receiverEndpoints.forEach { receiverEndpoint ->
                val receiverName = receiverEndpoint.identifier[0].value
                val receiver = settings.findReceiver(receiverName)
                // We only process receivers that are active and for this pipeline.
                if (receiver != null && receiver.topic == Topic.FULL_ELR) {
                    val updatedBundle = removeUnwantedConditions(bundle, receiverEndpoint)
                    val hl7Message = getHL7MessageFromBundle(updatedBundle, receiver)
                    val bodyBytes = hl7Message.encode().toByteArray()

                    // get a Report from the hl7 message
                    val (report, event, blobInfo) = HL7MessageHelpers.takeHL7GetReport(
                        Event.EventAction.BATCH,
                        bodyBytes,
                        listOf(message.reportId),
                        receiver,
                        this.metadata,
                        actionHistory
                    )

                    // insert batch task into Task table
                    this.insertBatchTask(
                        report,
                        report.bodyFormat.toString(),
                        blobInfo.blobUrl,
                        event
                    )

                    // nullify the previous task next_action
                    db.updateTask(
                        message.reportId,
                        TaskAction.none,
                        null,
                        null,
                        finishedField = Tables.TASK.TRANSLATED_AT,
                        null
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }

    /**
     * Inserts a 'batch' task into the task table for the [report] in question. This is just a pass-through function
     * but is present here for proper separation of layers and testing. This may need to be modified in the future.
     * The task will track the [report] in the [format] specified and knows it is located at [reportUrl].
     * [nextAction] specifies what is going to happen next for this report
     *
     */
    private fun insertBatchTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }

    /**
     * Turn a fhir [bundle] into an hl7 message formatter for the [receiver] specified.
     * @return HL7 Message in the format required by the receiver
     */
    internal fun getHL7MessageFromBundle(bundle: Bundle, receiver: Receiver): ca.uhn.hl7v2.model.Message {
        val converter = FhirToHl7Converter(
            receiver.schemaName
        )
        val hl7Message = converter.convert(bundle)

        // if receiver is 'testing' or useTestProcessingMode is true, set to 'T', otherwise leave it as is
        if (receiver.customerStatus == CustomerStatus.TESTING ||
            (
                (receiver.translation is Hl7Configuration) &&
                    receiver.translation.useTestProcessingMode
                )
        ) {
            Terser(hl7Message).set("MSH-11-1", "T")
        }

        return hl7Message
    }

    /**
     * Removes observations from a [bundle] that are not referenced in [receiverEndpoint]
     *
     * @return [Bundle] with the unwanted observations removed
     */
    internal fun removeUnwantedConditions(bundle: Bundle, receiverEndpoint: Endpoint): Bundle {

        // Copy bundle to make sure original stays untouched
        val newBundle = bundle.copy()

        // Get observation references to keep from the receiver endpoint
        val observationsToKeep = receiverEndpoint.extension.flatMap { it.getResourceReferences() }

        // If endpoint doesn't have any references don't remove any
        if (observationsToKeep.isNotEmpty()) {
            // Get all diagnostic reports in the bundle
            val diagnosticReports =
                FhirPathUtils.evaluate(null, newBundle, newBundle, "Bundle.entry.resource.ofType(DiagnosticReport)")

            // Get all observation references in the diagnostic reports
            val allObservations =
                diagnosticReports.filterIsInstance<DiagnosticReport>().flatMap { it.result }.map { it.reference }

            // Determine observations ids to remove
            val observationsIdsToRemove = allObservations - observationsToKeep.toSet()

            // Get observation resources to be removed from the bundle
            val observationsToRemove = newBundle.entry.filter { it.resource.id in observationsIdsToRemove }

            observationsToRemove.forEach { newBundle.deleteResource(it.resource) }
        }

        return newBundle
    }
}