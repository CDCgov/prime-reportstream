package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.apache.logging.log4j.kotlin.Logging

const val elrTranslateQueueName = "elr-fhir-translate"

/**
 * All logical processing for full ELR / FHIR processing should be within this class.
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIREngine(
    val metadata: Metadata = Metadata.getInstance(),
    val settings: SettingsProvider = WorkflowEngine.settingsProviderSingleton,
    val db: DatabaseAccess = WorkflowEngine.databaseAccessSingleton,
    val blob: BlobAccess = BlobAccess(),
    val queue: QueueAccess = QueueAccess,
) : Logging {

    /**
     * Process a [message] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
     * [workflowEngine] is used for some underlying functionality that should be pulled out into a base class
     * at some point (tech debt).
     * [actionHistory] and [actionLogger] ensure all activities are logged
     */
    fun processHL7(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
        workflowEngine: WorkflowEngine
    ) {
        logger.trace("Processing HL7 data for FHIR conversion.")
        try {
            // get the hl7 from the blob store
            val hl7messages = HL7Reader(actionLogger).getMessages(message.downloadContent())

            if (actionLogger.hasErrors()) {
                throw java.lang.IllegalArgumentException(actionLogger.errors.joinToString("\n") { it.detail.message })
            }

            // use fhir transcoder to turn hl7 into FHIR
            val fhirBundles = hl7messages.map {
                HL7toFhirTranslator.getInstance().translate(it)
            }

            logger.info("Received ${fhirBundles.size} FHIR bundles.")

            actionHistory.trackExistingInputReport(message.reportId)

            // operate on each fhir bundle
            for (bundle in fhirBundles) {
                // make a 'report'
                val report = Report(
                    Report.Format.FHIR,
                    emptyList(),
                    fhirBundles.size,
                    itemLineage = listOf(
                        ItemLineage()
                    )
                )

                // create item lineage
                report.itemLineages = listOf(
                    ItemLineage(
                        null,
                        message.reportId,
                        1,
                        report.id,
                        1,
                        null,
                        null,
                        null,
                        report.getItemHashForRow(1)
                    )
                )

                // create translate event
                val translateEvent = ProcessEvent(
                    Event.EventAction.TRANSLATE,
                    report.id,
                    Options.None,
                    emptyMap<String, String>(),
                    emptyList<String>()
                    // TODO: determine if we need to support these parameters for full ELR
//                        message.options,
//                        message.defaults,
//                        message.routeTo
                )

                // upload to blobstore
                var bodyBytes = FhirTranscoder.encode(bundle).toByteArray()
                var blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyBytes,
                    report.name,
                    message.sender,
                    translateEvent.eventAction
                )

                // track created report
                actionHistory.trackCreatedReport(translateEvent, report, blobInfo)

                // insert task
                this.insertTranslateTask(
                    report,
                    report.bodyFormat.toString(),
                    blobInfo.blobUrl,
                    translateEvent
                )

                // move to translation (send to <elrTranslateQueueName> queue)
                workflowEngine.queue.sendMessage(
                    elrTranslateQueueName,
                    RawSubmission(
                        report.id,
                        blobInfo.blobUrl,
                        BlobAccess.digestToString(blobInfo.digest),
                        message.sender
                        // TODO: do we need these here? Will need to figure out how to serialize/deserialize
                        // options,
                        // defaults,
                        // routeTo
                    ).serialize()
                )
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }

    /**
     * Inserts a 'translate' task into the task table for the [report] in question. This is just a passthrough function
     * but is present here for proper separation of layers and testing. This may need to be modified in the future.
     */
    private fun insertTranslateTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }
}