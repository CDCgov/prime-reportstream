package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonProperty
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ErrorCode
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
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
import gov.cdc.prime.router.azure.observability.event.ReceiverFilterFailedEvent
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
 * [azureEventService] mockable azure event service
 * [reportService] mockable report service
 */
class FHIRReceiverFilter(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService) {
    override val finishedField: Field<OffsetDateTime> = Tables.TASK.RECEIVER_FILTERED_AT

    override val engineType: String = "ReceiverFilter"

    /**
     * Accepts a [message] in internal FHIR format
     *
     * [message] is the incoming message with metadata for evaluating receiver filters
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
                    "Message was not a FhirReceiverFilter and cannot be processed: $message"
                )
            }
        }

    class ReceiverItemFilteredActionLogDetail(
        val filter: String,
        @JsonProperty
        val filterType: ReportStreamFilterType,
        @JsonProperty
        val receiverOrg: String,
        @JsonProperty
        val receiverName: String,
        val index: Int = 1,
    ) : ActionLogDetail {
        override val scope: ActionLogScope = ActionLogScope.item

        override val message: String = "Item was not routed to $receiverOrg.$receiverName because it did not pass the" +
            " $filterType. Item failed on: $filter"

        override val errorCode: ErrorCode = ErrorCode.UNKNOWN
    }

    data class FilterDetails(val filters: List<String>, val filterType: ReportStreamFilterType)

    sealed class ReceiverFilterEvaluationResult {
        data class Success(val bundle: Bundle) : ReceiverFilterEvaluationResult()
        data class Failure(val failingFilter: FilterDetails) : ReceiverFilterEvaluationResult()
    }

    sealed class FhirExpressionEvaluationResult {
        data object Success : FhirExpressionEvaluationResult()
        data class Failure(val failingFilter: FilterDetails) : FhirExpressionEvaluationResult()
    }

    /**
     * Runs a [receiver]'s filters on a [bundle], returning a data class with the pruned bundle or
     * filter failure information
     *
     * [actionLogger] ensure all activities are logged.
     */
    private fun evaluateReceiverFilters(
        receiver: Receiver,
        bundle: Bundle,
        actionLogger: ActionLogger,
    ): ReceiverFilterEvaluationResult {
        val trackingId = bundle.identifier.value

        // filter groups for looped evaluation
        val fhirFilters = listOf(
            Pair(receiver.qualityFilter, ReportStreamFilterType.QUALITY_FILTER),
            Pair(receiver.routingFilter, ReportStreamFilterType.ROUTING_FILTER),
            Pair(receiver.processingModeFilter, ReportStreamFilterType.PROCESSING_MODE_FILTER),
        )

        // evaluate all filter groups
        fhirFilters.forEach {
            val result = evaluateFhirExpressionFilters(
                receiver,
                bundle,
                actionLogger,
                trackingId,
                it.first,
                it.second
            )
            if (result is FhirExpressionEvaluationResult.Failure) {
                return ReceiverFilterEvaluationResult.Failure(result.failingFilter)
            }
        }

        return evaluateObservationConditionFilters(receiver, bundle, actionLogger, trackingId)
    }

    class MisconfiguredReceiverConditionFilters(val receiver: Receiver) : RuntimeException() {
        override val message: String =
            """${receiver.fullName} has \"conditionFilter\" and \"mappedConditionFilter\" which is not allowed as it can
                | result in unintended bugs.  Please update the receiver to only use one
""".trimMargin()
    }

    /**
     * Evaluate a [receiver]'s condition filters on a [bundle], returning the pruned bundle or null if all observations
     * were pruned.
     *
     * [actionLogger] and [trackingId] facilitate logging
     */
    private fun evaluateObservationConditionFilters(
        receiver: Receiver,
        bundle: Bundle,
        actionLogger: ActionLogger,
        trackingId: String,
    ): ReceiverFilterEvaluationResult {
        val conditionFilters = receiver.conditionFilter
        val mappedConditionFilters = receiver.mappedConditionFilter

        // These filters are not compatible with each other and configuring them at the same time is likely to produce
        // unintended results
        if (conditionFilters.isNotEmpty() && mappedConditionFilters.isNotEmpty()) {
            throw MisconfiguredReceiverConditionFilters(receiver)
        }

        val allObservations = bundle.getObservations()
        val result: ReceiverFilterEvaluationResult = if (conditionFilters.isNotEmpty()) {
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
            val allRemainingObservationsAreAoe = keptObservations
                .all {
                    val conditions = it.getMappedConditionCodes()
                    conditions.isNotEmpty() && conditions.all { code -> code == "AOE" }
                }
            if (keptObservations.isEmpty() || allRemainingObservationsAreAoe) {
                actionLogger.getItemLogger(1, trackingId).warn(
                    ReceiverItemFilteredActionLogDetail(
                        conditionFilters.joinToString(","),
                        ReportStreamFilterType.CONDITION_FILTER,
                        receiver.organizationName,
                        receiver.name,
                        1
                    )
                )
                ReceiverFilterEvaluationResult.Failure(
                    FilterDetails(conditionFilters, ReportStreamFilterType.CONDITION_FILTER)
                )
            } else {
                filteredObservations.forEach { observation ->
                    withLoggingContext(mapOf(MDCUtils.MDCProperty.OBSERVATION_ID to observation.id.toString())) {
                        logger.info("Observations were filtered from the bundle")
                    }
                }
                ReceiverFilterEvaluationResult.Success(
                    bundle.filterObservations(conditionFilters, shorthandLookupTable)
                )
            }
        } else if (mappedConditionFilters.isNotEmpty()) {
            val codes = mappedConditionFilters.codes()
            val keptObservations = bundle.getObservationsWithCondition(codes)
            if (keptObservations.isEmpty() ||
                keptObservations.all {
                    it.getMappedConditionCodes().all { code -> code == "AOE" }
                }
            ) {
                actionLogger.getItemLogger(1, trackingId).warn(
                    ReceiverItemFilteredActionLogDetail(
                        mappedConditionFilters.joinToString(","),
                        ReportStreamFilterType.MAPPED_CONDITION_FILTER,
                        receiver.organizationName,
                        receiver.name,
                        1
                    )
                )
                ReceiverFilterEvaluationResult.Failure(
                    FilterDetails(
                        mappedConditionFilters.map { it.value },
                        ReportStreamFilterType.MAPPED_CONDITION_FILTER
                    )
                )
            } else {
                val (filteredObservationIds, filteredBundle) = bundle.filterMappedObservations(
                    receiver.mappedConditionFilter
                )
                filteredObservationIds.forEach { observationId ->
                    withLoggingContext(mapOf(MDCUtils.MDCProperty.OBSERVATION_ID to observationId)) {
                        logger.info("Observations were filtered from the bundle")
                    }
                }
                ReceiverFilterEvaluationResult.Success(filteredBundle)
            }
        } else {
            ReceiverFilterEvaluationResult.Success(bundle)
        }

        return result
    }

    /**
     * Evaluates a list of FHIR expression [filters] for a [receiver] on a [bundle].
     *
     * [actionLogger], [trackingId], and [filterType] facilitate logging
     */
    private fun evaluateFhirExpressionFilters(
        receiver: Receiver,
        bundle: Bundle,
        actionLogger: ActionLogger,
        trackingId: String,
        filters: List<String>,
        filterType: ReportStreamFilterType,
    ): FhirExpressionEvaluationResult {
        val filtersEvaluated = filters.map { filter ->
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
        if (!filtersEvaluated.all { (passes, _) -> passes }) {
            val failingFilters = filtersEvaluated.filter { (passes, _) -> !passes }.map { (_, filter) ->
                actionLogger.getItemLogger(1, trackingId).warn(
                    ReceiverItemFilteredActionLogDetail(
                        filter,
                        filterType,
                        receiver.organizationName,
                        receiver.name,
                        1
                    )
                )
                filter
            }
            return FhirExpressionEvaluationResult.Failure(FilterDetails(failingFilters, filterType))
        }
        return FhirExpressionEvaluationResult.Success
    }

    /**
     * Process a [queueMessage] from the azure queue
     *
     * [actionHistory] and [actionHistory] ensure all activities are logged.
     */
    private fun fhirEngineRunResults(
        queueMessage: FhirReceiverFilterQueueMessage,
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

            // track input report
            actionHistory.trackExistingInputReport(queueMessage.reportId)

            // gather receiver and sender objects
            val receiver = settings.receivers.first { it.fullName == queueMessage.receiverFullName }
            val rootReport = reportService.getRootReport(queueMessage.reportId)
            val sender = "${rootReport.sendingOrg}.${rootReport.sendingOrgClient}"

            // download and parse FHIR document
            val fhirJson = LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                "Downloaded content from queue message"
            ) { queueMessage.downloadContent() }
            val bundle = FhirTranscoder.decode(fhirJson)

            actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)

            when (val filterResult = evaluateReceiverFilters(receiver, bundle, actionLogger)) {
                is ReceiverFilterEvaluationResult.Success -> {
                    logger.info("Bundle was returned after evaluating receiver filters.")
                    val receiverBundle = filterResult.bundle
                    val report = Report(
                        MimeFormat.FHIR,
                        emptyList(),
                        parentItemLineageData = listOf(
                            Report.ParentItemLineageData(queueMessage.reportId, 1)
                        ),
                        metadata = this.metadata,
                        topic = queueMessage.topic,
                        nextAction = TaskAction.translate
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
                        MimeFormat.FHIR,
                        bodyString.toByteArray(),
                        report.id.toString(),
                        queueMessage.blobSubFolderName,
                        nextEvent.eventAction
                    )
                    // ensure tracking is set
                    actionHistory.trackCreatedReport(nextEvent, report, blobInfo = blobInfo)

                    // send event to Azure AppInsights
                    val receiverObservationSummary = AzureEventUtils.getObservationSummaries(receiverBundle)

                    val filteredObservationSummary = AzureEventUtils.getObservationSummaries(
                        bundle.getObservations().filter { observation ->
                            receiverBundle.getObservations().none { receiverObservation ->
                                observation == receiverObservation ||
                                    observation.id == receiverObservation.id ||
                                    observation.identifier == receiverObservation.identifier
                            }
                        }
                    )

                    azureEventService.trackEvent(
                        ReportRouteEvent(
                            report.id,
                            queueMessage.reportId,
                            rootReport.reportId,
                            queueMessage.topic,
                            sender,
                            receiver.fullName,
                            receiverObservationSummary,
                            filteredObservationSummary,
                            bodyString.length,
                            AzureEventUtils.getIdentifier(receiverBundle)
                        )
                    )

                    return listOf(
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
                }

                is ReceiverFilterEvaluationResult.Failure -> {
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
                        MimeFormat.FHIR,
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

                    val observationSummary = AzureEventUtils.getObservationSummaries(bundle)
                    azureEventService.trackEvent(
                        ReceiverFilterFailedEvent(
                            emptyReport.id,
                            queueMessage.reportId,
                            rootReport.reportId,
                            queueMessage.topic,
                            sender,
                            receiver.fullName,
                            observationSummary,
                            filterResult.failingFilter.filters,
                            filterResult.failingFilter.filterType,
                            fhirJson.length,
                            AzureEventUtils.getIdentifier(bundle)
                        )
                    )

                    return emptyList()
                }
            }
        }
    }
}