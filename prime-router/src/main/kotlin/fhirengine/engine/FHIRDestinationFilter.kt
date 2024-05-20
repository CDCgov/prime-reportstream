package gov.cdc.prime.router.fhirengine.engine

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.DestinationFilterReportNoReceiversEvent
import gov.cdc.prime.router.azure.observability.event.DestinationFilterReportNotRoutedEvent
import gov.cdc.prime.router.azure.observability.event.DestinationFilterReportRoutedEvent
import gov.cdc.prime.router.azure.observability.event.ReportAcceptedEvent
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.report.ReportService
import org.jooq.Field
import java.time.OffsetDateTime

/**
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRDestinationFilter(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService) {

    /**
     * Adds logs for reports that pass through various methods in the FHIRRouter
     */
    private var actionLogger: ActionLogger? = null

    /**
     * Process a [message] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        this.actionLogger = actionLogger

        message as ReportPipelineMessage

        // track input report
        actionHistory.trackExistingInputReport(message.reportId)

        // pull fhir document and parse FHIR document
        val fhirJson = message.downloadContent()
        val bundle = FhirTranscoder.decode(fhirJson)
        val bodyString = FhirTranscoder.encode(bundle)

        // go up the report lineage to get the sender of the root report
        val sender = reportService.getSenderName(message.reportId)

        // send event to Azure AppInsights
        val observationSummary = AzureEventUtils.getObservations(bundle)
        azureEventService.trackEvent(
            ReportAcceptedEvent(
                message.reportId,
                message.topic,
                sender,
                observationSummary,
                fhirJson.length
            )
        )

        // get the receivers that this bundle should go to
        val receivers = settings.receivers.filter { receiver ->
            val pass = if (receiver.customerStatus != CustomerStatus.ACTIVE || receiver.topic != message.topic) {
                false
            } else {
                receiver.jurisdictionalFilter.all { filter ->
                    FhirPathUtils.evaluateCondition(
                        CustomContext(bundle, bundle, shorthandLookupTable, CustomFhirPathFunctions()),
                        bundle,
                        bundle,
                        bundle,
                        filter
                    )
                }
            }
            if (!pass) {
                azureEventService.trackEvent(
                    DestinationFilterReportNotRoutedEvent(
                        message.reportId,
                        message.topic,
                        sender,
                        receiver.fullName,
                        fhirJson.length,
                    )
                )
            }
            pass
        }

        // check if there are any receivers
        if (receivers.isNotEmpty()) {
            return receivers.flatMap { receiver ->
                val report = Report(
                    Report.Format.FHIR,
                    emptyList(),
                    1,
                    metadata = this.metadata,
                    topic = message.topic,
                    destination = receiver
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

                val nextEvent = ProcessEvent(
                    Event.EventAction.RECEIVER_FILTER,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )

                // upload new copy to blobstore
                val blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyString.toByteArray(),
                    report.name,
                    message.blobSubFolderName,
                    nextEvent.eventAction
                )
                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, report, blobInfo = blobInfo)

                azureEventService.trackEvent(
                    DestinationFilterReportRoutedEvent(
                        message.reportId,
                        report.id,
                        message.topic,
                        sender,
                        receiver.fullName,
                        bodyString.length
                    )
                )

                listOf(
                    FHIREngineRunResult(
                        nextEvent,
                        report,
                        blobInfo.blobUrl,
                        FhirReceiverFilterQueueMessage(
                            report.id,
                            blobInfo.blobUrl,
                            BlobAccess.digestToString(blobInfo.digest),
                            message.blobSubFolderName,
                            message.topic,
                            receiver.fullName
                        )
                    )
                )
            }
        } else {
            // this bundle does not have receivers; only perform the work necessary to track the routing action
            // create none event
            val nextEvent = ProcessEvent(
                Event.EventAction.NONE,
                message.reportId,
                Options.None,
                emptyMap(),
                emptyList()
            )
            val report = Report(
                Report.Format.FHIR,
                emptyList(),
                1,
                metadata = this.metadata,
                topic = message.topic
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

            // ensure tracking is set
            actionHistory.trackCreatedReport(nextEvent, report)

            // send event to Azure AppInsights
            azureEventService.trackEvent(
                DestinationFilterReportNoReceiversEvent(
                    message.reportId,
                    report.id,
                    message.topic,
                    sender,
                    fhirJson.length
                )
            )

            return emptyList()
        }
    }

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.DESTINATION_FILTERED_AT
    override val engineType: String = "DestinationFilter"
}