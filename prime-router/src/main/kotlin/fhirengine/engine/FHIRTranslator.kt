package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils.defaultHl7EncodingFiveChars
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils.defaultHl7EncodingFourChars
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.deleteResource
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.getResourceReferences
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
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
        // pull fhir document and parse FHIR document
        val bundle = FhirTranscoder.decode(message.downloadContent())

        // track input report
        actionHistory.trackExistingInputReport(message.reportId)

        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val receiverEndpoints = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()

        receiverEndpoints.forEach { receiverEndpoint ->
            val receiverName = receiverEndpoint.identifier[0].value
            val receiver = settings.findReceiver(receiverName)
            if (receiver != null) {
                actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)
            }

            // We only process receivers that are active and for this pipeline.
            if (receiver != null && receiver.topic.isUniversalPipeline) {
                try {
                    val updatedBundle = pruneBundleForReceiver(bundle, receiverEndpoint)

                    val bodyBytes = getByteArrayFromBundle(receiver, updatedBundle)

                    // get a Report from the message
                    val (report, event, blobInfo) = Report.generateReportAndUploadBlob(
                        Event.EventAction.BATCH,
                        bodyBytes,
                        listOf(message.reportId),
                        receiver,
                        this.metadata,
                        actionHistory,
                        topic = message.topic,
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
                } catch (e: Exception) { // handle translation errors
                    logger.error(e)
                    actionLogger.error(InvalidReportMessage(e.message ?: ""))
                }
            }
        }
    }

    /**
     * Returns a byteArray representation of the [bundle] in a format [receiver] expects, or throws an exception if the
     * expected format isn't supported.
     */
    internal fun getByteArrayFromBundle(
        receiver: Receiver,
        bundle: Bundle
    ) = when (receiver.format) {
        Report.Format.FHIR -> {
            if (receiver.schemaName.isNotEmpty()) {
                val transformer = FhirTransformer(receiver.schemaName)
                transformer.transform(bundle)
            }
            FhirTranscoder.encode(bundle, FhirContext.forR4().newJsonParser()).toByteArray()
        }
        Report.Format.HL7, Report.Format.HL7_BATCH -> {
            val hl7Message = getHL7MessageFromBundle(bundle, receiver)
            hl7Message.encodePreserveEncodingChars().toByteArray()
        }
        else -> {
            error("Receiver format ${receiver.format} not supported.")
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
     * Turn a fhir [bundle] into a hl7 message formatter for the [receiver] specified.
     * @return HL7 Message in the format required by the receiver
     */
    internal fun getHL7MessageFromBundle(bundle: Bundle, receiver: Receiver): ca.uhn.hl7v2.model.Message {
        val converter = FhirToHl7Converter(
            receiver.schemaName,
            context = FhirToHl7Context(CustomFhirPathFunctions())
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
     * Removes observations from a [bundle] that are not referenced in [receiverEndpoint] and any endpoints that are
     * not [receiverEndpoint]
     *
     * @return a copy of [bundle] with the unwanted observations/endpoints removed
     */
    internal fun pruneBundleForReceiver(bundle: Bundle, receiverEndpoint: Endpoint): Bundle {
        // Copy bundle to make sure original stays untouched
        val newBundle = bundle.copy()
        newBundle.removeUnwantedConditions(receiverEndpoint)
        newBundle.removeUnwantedProvenanceEndpoints(receiverEndpoint)
        return newBundle
    }

    /**
     * Removes observations from this bundle that are not referenced in [receiverEndpoint]
     *
     * @return the bundle with the unwanted observations removed
     */
    internal fun Bundle.removeUnwantedConditions(receiverEndpoint: Endpoint): Bundle {
        // Get observation references to keep from the receiver endpoint
        val observationsToKeep = receiverEndpoint.extension.flatMap { it.getResourceReferences() }

        // If endpoint doesn't have any references don't remove any
        if (observationsToKeep.isNotEmpty()) {
            // Get all diagnostic reports in the bundle
            val diagnosticReports =
                FhirPathUtils.evaluate(null, this, this, "Bundle.entry.resource.ofType(DiagnosticReport)")

            // Get all observation references in the diagnostic reports
            val allObservations =
                diagnosticReports.filterIsInstance<DiagnosticReport>().flatMap { it.result }.map { it.reference }

            // Determine observations ids to remove
            val observationsIdsToRemove = allObservations - observationsToKeep.toSet()

            // Get observation resources to be removed from the bundle
            val observationsToRemove = this.entry.filter { it.resource.id in observationsIdsToRemove }

            observationsToRemove.forEach { this.deleteResource(it.resource) }
        }
        return this
    }

    /**
     * Removes endpoints from this bundle that do not match [receiverEndpoint]
     *
     * @return the bundle with the unwanted endpoints removed
     */
    internal fun Bundle.removeUnwantedProvenanceEndpoints(receiverEndpoint: Endpoint): Bundle {
        val provenance = this.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        provenance.target.map { it.resource }.filterIsInstance<Endpoint>().forEach {
            if (it != receiverEndpoint)
                this.deleteResource(it)
        }
        return this
    }
}

/**
 * Encodes a message while avoiding an error when MSH-2 is five characters long
 *
 * @return the encoded message as a string
 */
fun Message.encodePreserveEncodingChars(): String {
    // get encoding characters ...
    val msh = this.get("MSH") as Segment
    val encCharString = Terser.get(msh, 2, 0, 1, 1)
    val hasFiveEncodingChars = encCharString == defaultHl7EncodingFiveChars
    if (hasFiveEncodingChars) Terser.set(msh, 2, 0, 1, 1, defaultHl7EncodingFourChars)
    var encodedMsg = encode()
    if (hasFiveEncodingChars) {
        encodedMsg = encodedMsg.replace(defaultHl7EncodingFourChars, defaultHl7EncodingFiveChars)
        // Set MSH-2 back in the in-memory message to preserve original value
        Terser.set(msh, 2, 0, 1, 1, defaultHl7EncodingFiveChars)
    }
    return encodedMsg
}