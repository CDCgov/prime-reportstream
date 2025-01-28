package gov.cdc.prime.router.fhirengine.engine

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigestExtractor
import gov.cdc.prime.router.azure.observability.bundleDigest.FhirPathBundleDigestLabResultExtractorStrategy
import gov.cdc.prime.router.azure.observability.context.MDCUtils
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.logging.LogMeasuredTime
import gov.cdc.prime.router.report.ReportService
import org.jooq.Field
import java.time.OffsetDateTime

class FHIRReceiverEnrichment(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
    reportStreamEventService: IReportStreamEventService,
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService, reportStreamEventService) {

    /**
     * Accepts a [FhirReceiverEnrichmentQueueMessage] [message] and sends a report to the
     * next pipeline step containing enrichments configured per the receiver's settings.
     * [actionHistory] and [actionLogger] ensure all activities are recorded to the database and logged.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        when (message) {
            is FhirReceiverEnrichmentQueueMessage -> {
                val contextMap = mapOf(
                    MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
                    MDCUtils.MDCProperty.REPORT_ID to message.reportId,
                    MDCUtils.MDCProperty.TOPIC to message.topic,
                    MDCUtils.MDCProperty.BLOB_URL to message.blobURL
                )
                withLoggingContext(contextMap) {
                    logger.trace("Starting FHIR ReceiverEnrichment work")
                    actionHistory.trackExistingInputReport(message.reportId)
                    val receiver = settings.findReceiver(message.receiverFullName)
                        ?: throw RuntimeException("Receiver with name ${message.receiverFullName} was not found")
                    actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)
                    return fhirEngineRunResults(message, receiver, actionHistory)
                }
            }
            else -> {
                // Handle the case where casting failed
                throw RuntimeException(
                    "Message was not a FhirReceiverEnrichmentQueueMessage and cannot be " +
                        "processed by FHIRReceiverEnrichment: $message"
                )
            }
        }
    }

    /**
     * Process a [queueMessage] off of the elr-fhir-receiver-enrichment-queue azure queue, add enrichments, and store
     * for next step.
     * [actionHistory] ensures all activities are logged.
     */
    private fun fhirEngineRunResults(
        queueMessage: FhirReceiverEnrichmentQueueMessage,
        receiver: Receiver,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        // pull fhir document and parse FHIR document
        val fhirJson = LogMeasuredTime.measureAndLogDurationWithReturnedValue(
            "Downloaded content from queue message"
        ) {
            BlobAccess.downloadBlob(queueMessage.blobURL, queueMessage.digest)
        }
        val bundle = FhirTranscoder.decode(fhirJson)
        if (receiver.enrichmentSchemaNames.isNotEmpty()) {
            receiver.enrichmentSchemaNames.forEach { enrichmentSchemaName ->
                logger.info("Applying enrichment schema $enrichmentSchemaName")
                val transformer = FhirTransformer(
                    enrichmentSchemaName,
                )
                transformer.process(bundle)
            }
        }
        val bodyString = FhirTranscoder.encode(bundle)

        val report = Report(
            MimeFormat.FHIR,
            emptyList(),
            1,
            metadata = this.metadata,
            topic = queueMessage.topic,
            destination = receiver,
            nextAction = TaskAction.receiver_enrichment
        )

        // create item lineage
        report.itemLineages = listOf(
            ItemLineage(
                null,
                queueMessage.reportId,
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
            MimeFormat.FHIR,
            bodyString.toByteArray(),
            report.id.toString(),
            queueMessage.blobSubFolderName,
            nextEvent.eventAction
        )
        report.bodyURL = blobInfo.blobUrl
        // ensure tracking is set
        actionHistory.trackCreatedReport(nextEvent, report, blobInfo = blobInfo)

        val bundleDigestExtractor = BundleDigestExtractor(
            FhirPathBundleDigestLabResultExtractorStrategy(
                CustomContext(
                    bundle,
                    bundle,
                    mutableMapOf(),
                    CustomFhirPathFunctions()
                )
            )
        )
        reportEventService.sendItemEvent(
            eventName = ReportStreamEventName.ITEM_ROUTED,
            childReport = report,
            pipelineStepName = TaskAction.receiver_enrichment
        ) {
            parentReportId(queueMessage.reportId)
            params(
                mapOf(
                    ReportStreamEventProperties.RECEIVER_NAME to receiver.fullName,
                    ReportStreamEventProperties.BUNDLE_DIGEST
                        to bundleDigestExtractor.generateDigest(bundle),
                    ReportStreamEventProperties.ENRICHMENTS to receiver.enrichmentSchemaNames
                )
            )
            trackingId(bundle)
        }

        return listOf(
            FHIREngineRunResult(
                nextEvent,
                report,
                blobInfo.blobUrl,
                FhirReceiverFilterQueueMessage(
                    report.id,
                    blobInfo.blobUrl,
                    BlobUtils.digestToString(blobInfo.digest),
                    queueMessage.blobSubFolderName,
                    queueMessage.topic,
                    receiver.fullName
                )
            )
        )
    }

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.RECEIVER_ENRICHED_AT
    override val engineType: String = "ReceiverEnrichment"
    override val taskAction: TaskAction = TaskAction.receiver_enrichment
}