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
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader

/**
 * Process a [message] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRConverter(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    queue: QueueAccess = QueueAccess,
) : FHIREngine(metadata, settings, db, blob, queue) {

    /**
     * [message] is the incoming HL7 message to be turned into FHIR and saved
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ) {
        logger.trace("Processing HL7 data for FHIR conversion.")
        try {
            // create the hl7 reader
            val hl7Reader = HL7Reader(actionLogger)

            // get the hl7 from the blob store
            val hl7messages = hl7Reader.getMessages(message.downloadContent())

            if (actionLogger.hasErrors()) {
                throw java.lang.IllegalArgumentException(actionLogger.errors.joinToString("\n") { it.detail.message })
            }

            // use fhir transcoder to turn hl7 into FHIR
            val fhirBundles = hl7messages.map {
                HL7toFhirTranslator.getInstance().translate(it)
            }

            logger.debug("Generated ${fhirBundles.size} FHIR bundles.")

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
                    ),
                    metadata = this.metadata
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

                // create route event
                val routeEvent = ProcessEvent(
                    Event.EventAction.ROUTE,
                    report.id,
                    Options.None,
                    emptyMap<String, String>(),
                    emptyList<String>()
                )

                // upload to blobstore
                var bodyBytes = FhirTranscoder.encode(bundle).toByteArray()
                var blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyBytes,
                    report.name,
                    message.blobSubFolderName,
                    routeEvent.eventAction
                )

                // track created report
                actionHistory.trackCreatedReport(routeEvent, report, blobInfo)

                // insert task
                this.insertRouteTask(
                    report,
                    report.bodyFormat.toString(),
                    blobInfo.blobUrl,
                    routeEvent
                )

                // nullify the previous task next_action
                db.updateTask(
                    message.reportId,
                    TaskAction.none,
                    null,
                    null,
                    finishedField = Tables.TASK.PROCESSED_AT,
                    null
                )

                // move to routing (send to <elrRoutingQueueName> queue)
                this.queue.sendMessage(
                    elrRoutingQueueName,
                    RawSubmission(
                        report.id,
                        blobInfo.blobUrl,
                        BlobAccess.digestToString(blobInfo.digest),
                        message.blobSubFolderName
                    ).serialize()
                )
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }

    /**
     * Inserts a 'route' task into the task table for the [report] in question. This is just a pass-through function
     * but is present here for proper separation of layers and testing. This may need to be modified in the future.
     * The task will track the [report] in the [format] specified and knows it is located at [reportUrl].
     * [nextAction] specifies what is going to happen next for this report
     *
     */
    private fun insertRouteTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }
}