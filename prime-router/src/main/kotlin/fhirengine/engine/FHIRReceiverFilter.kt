package gov.cdc.prime.router.fhirengine.engine

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ErrorCode
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.context.MDCUtils
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.ReportRouteEvent
import gov.cdc.prime.router.codes
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.filterMappedObservations
import gov.cdc.prime.router.fhirengine.utils.filterObservations
import gov.cdc.prime.router.fhirengine.utils.getMappedConditionCodes
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.fhirengine.utils.getObservationsWithCondition
import gov.cdc.prime.router.logging.LogMeasuredTime
import gov.cdc.prime.router.report.ReportService
import org.hl7.fhir.r4.model.Bundle
import org.jooq.Field
import java.time.OffsetDateTime

/**
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRReceiverFilter(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService) {
    private var actionLogger: ActionLogger? = null

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.RECEIVER_FILTERED_AT

    override val engineType: String = "ReceiverFilter"

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
    ): List<FHIREngineRunResult> =
        when (message) {
            is FhirReceiverFilterQueueMessage -> {
                check(message.topic.isUniversalPipeline) {
                    "Unexpected topic $message.topic in the Universal Pipeline routing step."
                }
                fhirEngineRunResults(message, actionLogger, actionHistory)
            }

            else -> {
                throw RuntimeException(
                    "Message was not a FhirDestinationFilter and cannot be processed: $message"
                )
            }
        }

    class ReceiverItemFilteredActionLogDetail(
        val filter: String,
        val filterType: ReportStreamFilterType,
        val receiverOrg: String,
        val receiverName: String,
        val index: Int = 1,
    ) : ActionLogDetail {
        override val scope: ActionLogScope = ActionLogScope.item
        override val message: String
            get() = TODO("Not yet implemented")

        override val errorCode: ErrorCode = ErrorCode.UNKNOWN
    }

    private fun getBundleToRoute(receiver: Receiver, bundle: Bundle, actionLogger: ActionLogger): Bundle? {
        val trackingId = bundle.identifier.value

        // TODO dry this out
        val qualityFiltersEvaluated = receiver.qualityFilter.map { filter ->
            Pair(
                FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, bundle, shorthandLookupTable, CustomFhirPathFunctions()),
                    bundle,
                    bundle,
                    bundle,
                    filter
                ),
                    filter
            )
        }
        if (!qualityFiltersEvaluated.all { (passes, _) -> passes }) {
            qualityFiltersEvaluated.filter { (passes, _) -> !passes }.forEach { (_, filter) ->
                actionLogger.getItemLogger(1, trackingId).warn(
                    ReceiverItemFilteredActionLogDetail(
                        filter,
                        ReportStreamFilterType.QUALITY_FILTER,
                        receiver.organizationName,
                        receiver.name,
                        1
                    )
                )
            }
            return null
        }

        val routingFiltersEvaluated = receiver.routingFilter.map { filter ->
            Pair(
                FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, bundle, shorthandLookupTable, CustomFhirPathFunctions()),
                    bundle,
                    bundle,
                    bundle,
                    filter
                ),
                    filter
            )
        }
        if (!routingFiltersEvaluated.all { (passes, _) -> passes }) {
            routingFiltersEvaluated.filter { (passes, _) -> !passes }.forEach { (_, filter) ->
                actionLogger.getItemLogger(1, trackingId).warn(
                    ReceiverItemFilteredActionLogDetail(
                        filter,
                        ReportStreamFilterType.ROUTING_FILTER,
                        receiver.organizationName,
                        receiver.name,
                        1
                    )
                )
            }
            return null
        }

        val processingModeFiltersEvaluated = receiver.processingModeFilter.map { filter ->
            Pair(
                FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, bundle, shorthandLookupTable, CustomFhirPathFunctions()),
                    bundle,
                    bundle,
                    bundle,
                    filter
                ),
                    filter
            )
        }
        if (!processingModeFiltersEvaluated.all { (passes, _) -> passes }) {
            processingModeFiltersEvaluated.filter { (passes, _) -> !passes }.forEach { (_, filter) ->
                actionLogger.getItemLogger(1, trackingId).warn(
                    ReceiverItemFilteredActionLogDetail(
                        filter,
                        ReportStreamFilterType.PROCESSING_MODE_FILTER,
                        receiver.organizationName,
                        receiver.name,
                        1
                    )
                )
            }
            return null
        }

        // TODO extract this into its own function
        val conditionFilters = receiver.conditionFilter
        val mappedConditionFilters = receiver.mappedConditionFilter

        // These filters are not compatible with each other and configuring them at the same time is likely to produce
        // unintended results
        if (conditionFilters.isNotEmpty() && mappedConditionFilters.isNotEmpty()) {
            throw RuntimeException(
                "Both conditionFilters and mappedConditionFilters should not be configured at the same time"
            )
        }

        // TODO logging
        // What is useful to log?
        // log the observations not keeping
        // log the whole conditional filter
        // which observation, filter?
        // how many observations were filtered? the whole condition filter?
        val allObservations = bundle.getObservations()
        val prunedBundle = if (conditionFilters.isNotEmpty()) {
            val (keptObservations, filteredObservations) = allObservations.partition { observation ->
                conditionFilters.any { filter ->
                    FhirPathUtils.evaluateCondition(
                        CustomContext(bundle, observation, shorthandLookupTable, CustomFhirPathFunctions()),
                        observation,
                        bundle,
                        bundle,
                        filter
                    )
                }
            }
            if (keptObservations.isEmpty()) {
                // Corresponds not routing
                null
            } else {
                bundle.filterObservations(conditionFilters, shorthandLookupTable)
            }
        } else if (mappedConditionFilters.isNotEmpty()) {
            val codes = mappedConditionFilters.codes()
            val keptObservations = bundle.getObservationsWithCondition(codes)
            if (keptObservations.isEmpty() || keptObservations.all {
                    it.getMappedConditionCodes().all { code -> code == "AOE" }
                }
            ) {
                null
            } else {
                val (_, filteredBundle) = bundle.filterMappedObservations(
                    receiver.mappedConditionFilter
                )
                filteredBundle
            }
        } else {
            bundle
        }

        return prunedBundle
    }

    /**
     * Process a [queueMessage] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
     * [actionHistory] ensures all activities are logged.
     */
    private fun fhirEngineRunResults(
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        val contextMap = mapOf(
            MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
            MDCUtils.MDCProperty.REPORT_ID to queueMessage.reportId,
            MDCUtils.MDCProperty.TOPIC to queueMessage.topic,
            MDCUtils.MDCProperty.BLOB_URL to queueMessage.blobURL
        )
        withLoggingContext(contextMap) {
            logger.info("Starting FHIR ReceiverFilter step")
            this.actionLogger = actionLogger

            // TODO move this up, this cast can fail
            queueMessage as FhirReceiverFilterQueueMessage

            // track input report
            actionHistory.trackExistingInputReport(queueMessage.reportId)

            // pull fhir document and parse FHIR document
            val fhirJson = LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                "Downloaded content from queue message"
            ) { queueMessage.downloadContent() }
            val bundle = FhirTranscoder.decode(fhirJson)

            val receiver = settings.receivers.first { it.fullName == queueMessage.receiverFullName }
            actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)

            val receiverBundle = getBundleToRoute(receiver, bundle, actionLogger)
            // go up the report lineage to get the sender of the root report
            val sender = reportService.getSenderName(queueMessage.reportId)
            return if (receiverBundle != null) {
                logger.info("Bundle was returned after evaluating receiver filters.")
                val report = Report(
                    Report.Format.FHIR,
                    emptyList(),
                    parentItemLineageData = listOf(
                        Report.ParentItemLineageData(queueMessage.reportId, 1)
                    ),
                    metadata = this.metadata,
                    topic = queueMessage.topic,
                    nextAction = TaskAction.route
                )

                val nextEvent = ProcessEvent(
                    Event.EventAction.TRANSLATE,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )

                // upload new copy to blobstore
                val bodyString = FhirTranscoder.encode(receiverBundle)
                val blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyString.toByteArray(),
                    report.name,
                    queueMessage.blobSubFolderName,
                    nextEvent.eventAction
                )
                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, report, blobInfo = blobInfo)

                // send event to Azure AppInsights
                val receiverObservationSummary = AzureEventUtils.getObservations(receiverBundle)
                azureEventService.trackEvent(
                    ReportRouteEvent(
                        queueMessage.reportId,
                        report.id,
                        queueMessage.topic,
                        sender,
                        receiver.fullName,
                        receiverObservationSummary,
                        // filteredObs,
                        bodyString.length
                    )
                )

                listOf(
                    FHIREngineRunResult(
                        nextEvent,
                        report,
                        blobInfo.blobUrl,
                        FhirTranslateQueueMessage(
                            report.id,
                            blobInfo.blobUrl,
                            BlobAccess.digestToString(blobInfo.digest),
                            queueMessage.blobSubFolderName,
                            queueMessage.topic,
                            receiver.fullName
                        )
                    )
                )
            } else {
                logger.info("Report did not pass receiver filters. Terminating lineage.")
                // this bundle does not have receivers; only perform the work necessary to track the routing action
                // create none event
                val nextEvent = ProcessEvent(
                    Event.EventAction.NONE,
                    queueMessage.reportId,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )
                val emptyReport = Report(
                    Report.Format.FHIR,
                    emptyList(),
                    1,
                    metadata = this.metadata,
                    topic = queueMessage.topic
                )

                // create item lineage
                emptyReport.itemLineages = listOf(
                    ItemLineage(
                        null,
                        queueMessage.reportId,
                        1,
                        emptyReport.id,
                        1,
                        null,
                        null,
                        null,
                        emptyReport.getItemHashForRow(1)
                    )
                )

                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, emptyReport)

                // TODO what events should be fired when?
                // send event to Azure AppInsights
                val receiverObservationSummary = AzureEventUtils.getObservations(bundle)
                azureEventService.trackEvent(
                    ReportRouteEvent(
                        queueMessage.reportId,
                        emptyReport.id,
                        queueMessage.topic,
                        sender,
                        null,
                        receiverObservationSummary,
                        fhirJson.length
                    )
                )

                emptyList()
            }
        }
    }
}