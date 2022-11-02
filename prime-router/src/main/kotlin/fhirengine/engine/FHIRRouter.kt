package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder

/**
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRRouter(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    queue: QueueAccess = QueueAccess,
) : FHIREngine(metadata, settings, db, blob, queue) {

    /**
     * Process a [message] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory
    ) {
        logger.trace("Processing HL7 data for FHIR conversion.")
        try {
            // track input report
            actionHistory.trackExistingInputReport(message.reportId)

            // pull fhir document and parse FHIR document
            val bundle = FhirTranscoder.decode(message.downloadContent())
            val listOfReceivers = mutableListOf<Receiver>()

            // find all receivers that have the full ELR topic and determine which applies
            val fullElrReceivers = settings.receivers.filter { it.topic == Topic.FULL_ELR.json_val }
            fullElrReceivers.forEach { receiver ->
                // Get the applicable filters, either receiver or organization level if there are no receiver
                //  filters. If there are no filters, the default juris rule is allowNone
                val filter = getJurisFilters(receiver)

                // default filter is 'allowNone', so there has to be at least one configured filter for the receiver
                //  to get this record
                if (filter != null) {
                    // go through all filter conditions in all juris filters that are configured and boolean AND each
                    //  predicate
                    val passesFilter =
                        filter.all {
                            FhirPathUtils.evaluateCondition(CustomContext(bundle, bundle), bundle, bundle, it)
                        }

                    if (passesFilter)
                        listOfReceivers.add(receiver)
                }
            }

            // TODO: other filter types

            // add the receivers, if any, to the fhir bundle
            if (listOfReceivers.isNotEmpty()) {
                FHIRBundleHelpers.addReceivers(bundle, listOfReceivers)
            }

            // create report object
            val sources = emptyList<Source>()
            val report = Report(
                Report.Format.FHIR,
                sources,
                1,
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

            // create translate event
            val translateEvent = ProcessEvent(
                Event.EventAction.TRANSLATE,
                message.reportId,
                Options.None,
                emptyMap(),
                emptyList()
            )

            // upload new copy to blobstore
            val bodyBytes = FhirTranscoder.encode(bundle).toByteArray()
            val blobInfo = BlobAccess.uploadBody(
                Report.Format.FHIR,
                bodyBytes,
                report.name,
                message.blobSubFolderName,
                translateEvent.eventAction
            )

            // ensure tracking is set
            actionHistory.trackCreatedReport(translateEvent, report, blobInfo)

            // insert translate task into Task table
            this.insertTranslateTask(
                report,
                report.bodyFormat.toString(),
                blobInfo.blobUrl,
                translateEvent
            )

            // nullify the previous task next_action
            db.updateTask(
                message.reportId,
                TaskAction.none,
                null,
                null,
                finishedField = Tables.TASK.ROUTED_AT,
                null
            )

            // move to translation (send to <elrTranslationQueueName> queue). This passes the same message on, but
            //  the destinations have been updated in the FHIR
            this.queue.sendMessage(
                elrTranslationQueueName,
                RawSubmission(
                    report.id,
                    blobInfo.blobUrl,
                    BlobAccess.digestToString(blobInfo.digest),
                    message.blobSubFolderName
                ).serialize()
            )
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }

    /**
     * Inserts a 'translate' task into the task table for the [report] in question. This is just a pass-through function
     * but is present here for proper separation of layers and testing. This may need to be modified in the future.
     * The task will track the [report] in the [format] specified and knows it is located at [reportUrl].
     * [nextAction] specifies what is going to happen next for this report
     *
     */
    private fun insertTranslateTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }

    /**
     * Gets the applicable jurisdictional filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any jurs filters configured for
     * this topic
     */
    internal fun getJurisFilters(receiver: Receiver): ReportStreamFilter? {
        return receiver.jurisdictionalFilter.ifEmpty {
            val org = settings.findOrganization(receiver.organizationName)!!
            if (org.filters != null &&
                org.filters.any { it.topic == Topic.FULL_ELR.json_val } &&
                org.filters.first { it.topic == Topic.FULL_ELR.json_val }.jurisdictionalFilter != null
            )
                org.filters.first { it.topic == Topic.FULL_ELR.json_val }.jurisdictionalFilter!!
            else
                null
        }
    }
}