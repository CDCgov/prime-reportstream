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
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder

/**
 * Translate a full-ELR FHIR [message] into the formats needed by any receivers from the route step
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
            val receivers = listOf<String>("CO-PDH")

            receivers.forEach {
                // todo: get schema for receiver (?)

                // todo: do translation, get instance of Report
//                var schema = ConfigSchema("schema name", hl7Type = "ORU_R01", hl7Version = "2.5.1", elements = listOf(element))
//                val message = FhirToHl7Converter(bundle, schema).convert()

                // create report object  // DEBUG - this needs to be the report created by converting to HL7
                // todo: remove this. we should not be creating this report here. It is for interim commit work
                val sources = emptyList<Source>()
                val report = Report(
                    Report.Format.HL7,
                    sources,
                    1,
                    metadata = metadata
                )

                // create batch event
                val batchEvent = ProcessEvent(
                    Event.EventAction.BATCH,
                    report.id,
                    Options.None,
                    emptyMap<String, String>(),
                    emptyList<String>()
                )

                // upload new copy to blobstore
                var blobInfo = blob.generateBodyAndUploadReport(
                    report
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