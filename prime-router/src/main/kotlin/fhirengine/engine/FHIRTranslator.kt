package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder

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
    queue: QueueAccess = QueueAccess,
) : FHIREngine(metadata, settings, db, blob, queue) {

    /**
     * Accepts a FHIR [message], parses it, and generates translated output files for each item in the destinations
     *  element.
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     * [metadata] will usually be null; mocked metadata can be passed in for unit tests
     */
    override fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
        metadata: Metadata?
    ) {
        logger.trace("Translating FHIR file for receivers.")
        try {
            // pull fhir document and parse FHIR document
            val bundle = FhirTranscoder.decode(message.downloadContent())

            // track input report
            actionHistory.trackExistingInputReport(message.reportId)

            // todo: iterate over each receiver, translating on a per-receiver basis - for phase 1, hard coded to CO
            val receivers = listOf("ignore.FULL_ELR")

            receivers.forEach { receiver ->
                // todo: get schema for receiver - for Phase 1 this is solely going to convert to HL7 and not do any
                //  receiver-specific transforms
                val converter = FhirToHl7Converter(
                    "ORU_R01-base",
                    "metadata/hl7_mapping/ORU_R01"
                )
                val hl7Message = converter.convert(bundle)

                // create report object
                val sources = emptyList<Source>()
                val report = Report(
                    Report.Format.HL7,
                    sources,
                    1,
                    metadata = metadata,
                    // todo: when we actually want to send HL7 data to a receiver, we will need to ensure the
                    //  destination property of the report is set
                    // destination = settings.findReceiver(it)
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

                // create batch event
                val batchEvent = ProcessEvent(
                    Event.EventAction.BATCH,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )

                // upload the translated copy to blobstore
                val bodyBytes = hl7Message.encode().toByteArray()
                val blobInfo = BlobAccess.uploadBody(
                    Report.Format.HL7,
                    bodyBytes,
                    report.name,
                    receiver,
                    batchEvent.eventAction
                )

                // track generated reports, one per receiver
                actionHistory.trackCreatedReport(batchEvent, report, blobInfo)

                // insert batch task into Task table
                this.insertBatchTask(
                    report,
                    report.bodyFormat.toString(),
                    blobInfo.blobUrl,
                    batchEvent
                )
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
}