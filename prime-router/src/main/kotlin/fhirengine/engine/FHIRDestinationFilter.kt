package gov.cdc.prime.router.fhirengine.engine

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
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
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.logging.LogMeasuredTime
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
    reportStreamEventService: IReportStreamEventService,
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService, reportStreamEventService) {
    override val finishedField: Field<OffsetDateTime> = Tables.TASK.DESTINATION_FILTERED_AT

    override val engineType: String = "DestinationFilter"
    override val taskAction: TaskAction = TaskAction.destination_filter

    internal fun findTopicReceivers(topic: Topic): List<Receiver> =
        settings.receivers.filter { it.customerStatus != CustomerStatus.INACTIVE && it.topic == topic }

    /**
     * Accepts a [message] in internal FHIR format
     *
     * [message] is the incoming message to be evaluated for valid receivers and routed to the appropriate queues
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        return when (message) {
            is FhirDestinationFilterQueueMessage -> {
                check(message.topic.isUniversalPipeline) {
                    "Unexpected topic $message.topic in the Universal Pipeline routing step."
                }
                fhirEngineRunResults(message, actionHistory)
            }

            else -> {
                throw RuntimeException(
                    "Message was not a FhirDestinationFilter and cannot be processed: $message"
                )
            }
        }
    }

    /**
     * Process a [queueMessage] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
     * [actionHistory] ensures all activities are logged.
     */
    private fun fhirEngineRunResults(
        queueMessage: FhirDestinationFilterQueueMessage,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        val contextMap = mapOf(
            MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
            MDCUtils.MDCProperty.REPORT_ID to queueMessage.reportId,
            MDCUtils.MDCProperty.TOPIC to queueMessage.topic,
            MDCUtils.MDCProperty.BLOB_URL to queueMessage.blobURL
        )
        withLoggingContext(contextMap) {
            // track input report
            logger.info("Starting FHIR DestinationFilter step")
            actionHistory.trackExistingInputReport(queueMessage.reportId)

            // pull fhir document and parse FHIR document
            val fhirJson = LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                "Downloaded content from queue message"
            ) {
                BlobAccess.downloadBlob(queueMessage.blobURL, queueMessage.digest)
            }
            val bundle = FhirTranscoder.decode(fhirJson)
            val bodyString = FhirTranscoder.encode(bundle)

            // get the receivers that this bundle should go to
            val receivers = findTopicReceivers(queueMessage.topic).filter { receiver ->
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

            // check if there are any receivers
            if (receivers.isNotEmpty()) {
                logger.info("Routing to receiver filter queue for ${receivers.size} receiver(s)")
                return receivers.flatMap { receiver ->
                    val report = Report(
                        MimeFormat.FHIR,
                        emptyList(),
                        1,
                        metadata = this.metadata,
                        topic = queueMessage.topic,
                        destination = receiver,
                        nextAction = TaskAction.receiver_filter
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
                        pipelineStepName = TaskAction.destination_filter
                    ) {
                        parentReportId(queueMessage.reportId)
                        params(
                            mapOf(
                            ReportStreamEventProperties.RECEIVER_NAME to receiver.fullName,
                            ReportStreamEventProperties.BUNDLE_DIGEST
                                to bundleDigestExtractor.generateDigest(bundle)
                        )
                        )
                        trackingId(bundle)
                    }

                    listOf(
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
            } else {
                logger.info("No receivers for this message. Terminating lineage.")
                // this bundle does not have receivers; only perform the work necessary to track the routing action
                // create none event
                val nextEvent = ProcessEvent(
                    Event.EventAction.NONE,
                    queueMessage.reportId,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )
                val report = Report(
                    MimeFormat.FHIR,
                    emptyList(),
                    1,
                    metadata = this.metadata,
                    topic = queueMessage.topic
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

                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, report)

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
                    eventName = ReportStreamEventName.ITEM_NOT_ROUTED,
                    childReport = report,
                    pipelineStepName = TaskAction.destination_filter
                ) {
                    parentReportId(queueMessage.reportId)
                    trackingId(bundle)
                    params(
                        mapOf(
                            ReportStreamEventProperties.BUNDLE_DIGEST
                                to bundleDigestExtractor.generateDigest(bundle)
                        )
                    )
                }

                return emptyList()
            }
        }
    }
}