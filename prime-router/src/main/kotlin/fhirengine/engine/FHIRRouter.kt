package gov.cdc.prime.router.fhirengine.engine

import QueueMessage
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ConditionFilter
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.EvaluateFilterConditionErrorMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.PrunedObservationsLogMessage
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilterResult
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.ReportStreamFilters
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.Topic
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
import gov.cdc.prime.router.azure.observability.event.ReportAcceptedEvent
import gov.cdc.prime.router.azure.observability.event.ReportRouteEvent
import gov.cdc.prime.router.codes
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.filterMappedObservations
import gov.cdc.prime.router.fhirengine.utils.filterObservations
import gov.cdc.prime.router.fhirengine.utils.getMappedConditionCodes
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.fhirengine.utils.getObservationsWithCondition
import gov.cdc.prime.router.report.ReportService
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation
import org.jooq.Field
import java.time.OffsetDateTime

// TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
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
        logger.trace("Processing HL7 data for FHIR conversion.")
        this.actionLogger = actionLogger

        val castedMessage = message as? FhirRouteQueueMessage

        if (castedMessage == null) {
            // Handle the case where casting failed
            throw RuntimeException(
                "Message was not a FhirTranslateQueueMessage and cannot be " +
                    "processed by FHIRTranslator: $message"
            )
        } else {
            // track input report
            actionHistory.trackExistingInputReport(message.reportId)
            val rootReportId = reportService.getRootReport(message.reportId).reportId

            // pull fhir document and parse FHIR document
            val fhirJson = BlobAccess.downloadContent(message.blobURL, message.digest)
            val bundle = FhirTranscoder.decode(fhirJson)

            // get the receivers that this bundle should go to
            val listOfReceivers = findReceiversForBundle(bundle, message.reportId, actionHistory, message.topic)

            // go up the report lineage to get the sender of the root report
            val sender = reportService.getSenderName(message.reportId)

            // send event to Azure AppInsights
            val observationSummary = AzureEventUtils.getObservationSummaries(bundle)
            azureEventService.trackEvent(
                ReportAcceptedEvent(
                    message.reportId,
                    rootReportId,
                    message.topic,
                    sender,
                    observationSummary,
                    fhirJson.length,
                    AzureEventUtils.getIdentifier(bundle)
                )
            )

            // check if there are any receivers
            if (listOfReceivers.isNotEmpty()) {
                val filteredIdMap: MutableMap<String, MutableList<String>> = mutableMapOf()
                return listOfReceivers.flatMap { receiver ->
                    val sources = emptyList<Source>()
                    val report = Report(
                        MimeFormat.FHIR,
                        sources,
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

                    // If the receiver does not have a condition filter set send the entire bundle to the translate step
                    var receiverBundle = if (receiver.conditionFilter.isEmpty()) {
                        bundle
                    } else {
                        bundle.filterObservations(
                            receiver.conditionFilter,
                            shorthandLookupTable
                        )
                    }

                    // If the receiver does not have a mapped condition filter send the entire bundle to the translate step
                    if (receiver.mappedConditionFilter.isNotEmpty()) {
                        val (filteredIds, filteredBundle) = receiverBundle.filterMappedObservations(
                            receiver.mappedConditionFilter
                        )
                        filteredIds.forEach { id ->
                            filteredIdMap.getOrPut(id) { mutableListOf() }.add(receiver.fullName)
                        }
                        receiverBundle = filteredBundle
                    }

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
                        message.blobSubFolderName,
                        nextEvent.eventAction
                    )
                    // ensure tracking is set
                    actionHistory.trackCreatedReport(nextEvent, report, blobInfo = blobInfo)

                    // send event to Azure AppInsights
                    val bundleObservationSummary = AzureEventUtils.getObservationSummaries(bundle)
                    val receiverObservationSummary = AzureEventUtils.getObservationSummaries(receiverBundle)
                    azureEventService.trackEvent(
                        ReportRouteEvent(
                            report.id,
                            message.reportId,
                            rootReportId,
                            message.topic,
                            sender,
                            receiver.fullName,
                            receiverObservationSummary,
                            bundleObservationSummary,
                            bodyString.length,
                            AzureEventUtils.getIdentifier(bundle)
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
                                BlobUtils.digestToString(blobInfo.digest),
                                message.blobSubFolderName,
                                message.topic,
                                receiver.fullName
                            )
                        )
                    )
                }.also {
                    actionLogger.info(PrunedObservationsLogMessage(message.reportId, filteredIdMap))
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
                    MimeFormat.FHIR,
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
                val receiverObservationSummary = AzureEventUtils.getObservationSummaries(bundle)
                azureEventService.trackEvent(
                    ReportRouteEvent(
                        report.id,
                        message.reportId,
                        rootReportId,
                        message.topic,
                        sender,
                        null,
                        receiverObservationSummary,
                        receiverObservationSummary,
                        fhirJson.length,
                        AzureEventUtils.getIdentifier(bundle)
                    )
                )

                return emptyList()
            }
        }
    }

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.ROUTED_AT
    override val engineType: String = "Route"

    /**
     * Applies all filters to the list of all receivers with topic with a topic matching [topic] that are not set as
     * INACTIVE. FHIRPath expressions are run against the [bundle] to determine if the receiver should get this message
     * As it goes through the filters, results are logged onto the provided [report]
     * @return list of receivers that should receive this bundle
     */
    internal fun findReceiversForBundle(
        bundle: Bundle,
        reportId: ReportId,
        actionHistory: ActionHistory,
        topic: Topic,
    ): List<Receiver> {
        check(topic.isUniversalPipeline) { "Unexpected topic $topic in the Universal Pipeline routing step." }
        val listOfReceivers = mutableListOf<Receiver>()
        // find all receivers that have a matching topic and determine which applies
        val topicReceivers =
            settings.receivers.filter { it.customerStatus != CustomerStatus.INACTIVE && it.topic == topic }

        topicReceivers.forEach { receiver ->
            // get the receiver's organization, since we need to be able to find/combine the correct filters
            val orgFilters = settings.findOrganization(receiver.organizationName)!!.filters

            // Get the applicable filters, either receiver or organization level if there are no receiver filters

            // NOTE: these could all be combined into a single `if` statement, but that would reduce readability and
            // ability to debug this section so is being left split out like this

            // JURIS FILTER
            //  default: allowNone
            var passes = evaluateFilterConditionAsAnd(getJurisFilters(receiver, orgFilters), bundle, false).first

            // QUALITY FILTER
            //  default: must have message id, patient last name, patient first name, dob, specimen type
            //           must have at least one of patient street, zip code, phone number, email
            //           must have at least one of order test date, specimen collection date/time, test result date
            passes = passes && evaluateFilterAndLogResult(
                getQualityFilters(receiver, orgFilters),
                bundle,
                reportId,
                actionHistory,
                receiver,
                ReportStreamFilterType.QUALITY_FILTER,
                true,
                receiver.reverseTheQualityFilter,
            )

            // ROUTING FILTER
            //  default: allowAll
            passes = passes && evaluateFilterAndLogResult(
                getRoutingFilter(receiver, orgFilters),
                bundle,
                reportId,
                actionHistory,
                receiver,
                ReportStreamFilterType.ROUTING_FILTER,
                defaultResponse = true,
            )

            // PROCESSING MODE FILTER
            //  default: allowAll
            passes = passes && evaluateFilterAndLogResult(
                getProcessingModeFilter(receiver, orgFilters),
                bundle,
                reportId,
                actionHistory,
                receiver,
                ReportStreamFilterType.PROCESSING_MODE_FILTER,
                defaultResponse = true
            )

            // CONDITION FILTER
            //  default: allowAll
            val allObservationsExpression = "Bundle.entry.resource.ofType(DiagnosticReport).result.resolve()"
            val allObservations = FhirPathUtils.evaluate(
                CustomContext(bundle, bundle, shorthandLookupTable, CustomFhirPathFunctions()),
                bundle,
                bundle,
                allObservationsExpression
            )

            // Automatically passing if observations are empty is necessary for messages that may not
            // contain any observations but messages that must have observations are now at risk of getting
            // routed if they contain no observations. This case must be handled in one of the filters above
            // while UP validation is still being designed/implemented.
            passes = passes && (
                allObservations.isEmpty() || allObservations.any { observation ->
                    evaluateFilterAndLogResult(
                        getConditionFilter(receiver, orgFilters),
                        bundle,
                        reportId,
                        actionHistory,
                        receiver,
                        ReportStreamFilterType.CONDITION_FILTER,
                        defaultResponse = true,
                        reverseFilter = false,
                        focusResource = observation,
                        useOr = true
                    )
                }
                )

            // MAPPED CONDITION FILTER
            //  default: allowAll
            if (bundle.getObservations().isNotEmpty() && receiver.mappedConditionFilter.isNotEmpty()) {
                val codes = receiver.mappedConditionFilter.codes()
                val filteredObservations = bundle.getObservationsWithCondition(codes)
                if (filteredObservations.isEmpty()) {
                    logFilterResults(
                        "mappedConditionFilter: $codes",
                        bundle,
                        reportId,
                        actionHistory,
                        receiver,
                        ReportStreamFilterType.MAPPED_CONDITION_FILTER,
                        bundle
                    )
                }
                passes = passes && filteredObservations.isNotEmpty() && // don't pass a bundle with only AOEs
                    !filteredObservations.all { it.getMappedConditionCodes().all { code -> code == "AOE" } }
            }

            // if all filters pass, add this receiver to the list of valid receivers
            if (passes) {
                listOfReceivers.add(receiver)
            }
        }

        return listOfReceivers
    }

    /**
     * Takes a [bundle] and [filter], evaluates if the bundle passes the filter. If the filter is null,
     * return [defaultResponse]. If the filter doesn't pass the results are logged on the [report] for
     * that specific [filterType].
     * @param filters Filters that will be evaluated
     * @param bundle FHIR Bundle that will be evaluated
     * @param reportId Report ID passed for logging purposes
     * @param receiver Receiver of the report passed for logging purposes
     * @param filterType Type of filter passed for logging purposes
     * @param defaultResponse Response returned if the filter is null or empty
     * @param reverseFilter Optional flag used to reverse the result of the filter evaluation
     * @param focusResource Starting point for the evaluation, can be [bundle] if checking from root
     * @param useOr Optional flag used to allow conditions to be evaluated with "or" instead of "and"
     * @return Boolean indicating if the bundle passes the filter or not
     *        Result will be negated if [reverseFilter] is true
     */
    internal fun evaluateFilterAndLogResult(
        filters: ReportStreamFilter,
        bundle: Bundle,
        reportId: ReportId,
        actionHistory: ActionHistory,
        receiver: Receiver,
        filterType: ReportStreamFilterType,
        defaultResponse: Boolean,
        reverseFilter: Boolean = false,
        focusResource: Base = bundle,
        useOr: Boolean = false,
    ): Boolean {
        val evaluationFunction = if (useOr) ::evaluateFilterConditionAsOr else ::evaluateFilterConditionAsAnd
        val (passes, failingFilterName) = evaluationFunction(
            filters,
            bundle,
            defaultResponse,
            reverseFilter,
            focusResource
        )
        if (!passes) {
            logFilterResults(
                failingFilterName ?: "unknown",
                bundle,
                reportId,
                actionHistory,
                receiver,
                filterType,
                focusResource
            )
        }
        return passes
    }

    /**
     * Takes a [bundle] and [filter] and optionally a [focusResource], evaluates if the bundle passes all
     * the filter conditions, or when [reverseFilter] is true, evaluates if the bundle doesn't pass at least one of the
     * filter conditions.
     * @param filter Filter that will be evaluated
     * @param bundle FHIR Bundle that will be evaluated
     * @param defaultResponse result when there are no filter conditions in the list
     * @param reverseFilter Optional flag used to reverse the result of the filter evaluation
     * @param focusResource Starting point for the evaluation, can be [bundle] if checking from root
     * @return Pair: Boolean indicating if the bundle passes the filter or not
     *         and String to use when logging the filter result
     */
    internal fun evaluateFilterConditionAsAnd(
        filter: ReportStreamFilter?,
        bundle: Bundle,
        defaultResponse: Boolean,
        reverseFilter: Boolean = false,
        focusResource: Base = bundle,
    ): Pair<Boolean, String?> {
        if (filter.isNullOrEmpty()) {
            return Pair(defaultResponse, "defaultResponse")
        }
        val failingFilters = mutableListOf<String>()
        val exceptionFilters = mutableListOf<String>()
        filter.forEach { filterElement ->
            try {
                val filterElementResult = FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, focusResource, shorthandLookupTable, CustomFhirPathFunctions()),
                    focusResource,
                    bundle,
                    bundle,
                    filterElement
                )
                if (!filterElementResult) failingFilters += filterElement
            } catch (e: SchemaException) {
                actionLogger?.warn(EvaluateFilterConditionErrorMessage(e.message))
                exceptionFilters += filterElement
            }
        }

        return if (exceptionFilters.isNotEmpty()) {
            Pair(false, "(exception found) $exceptionFilters")
        } else if (failingFilters.isEmpty()) {
            if (reverseFilter) {
                Pair(false, "(reversed) $filter")
            } else {
                Pair(true, null)
            }
        } else {
            if (reverseFilter) {
                Pair(true, null)
            } else {
                Pair(false, failingFilters.toString())
            }
        }
    }

    /**
     * Takes a [bundle] and a [filter] and optionally a [focusResource], evaluates if the bundle passes any of
     * the filter conditions. When [reverseFilter] is true, this method will consider the passing of *any* filter
     * condition as a failure.
     * @param filter Filter that will be evaluated
     * @param bundle FHIR Bundle that will be evaluated
     * @param defaultResponse result when there are no filter conditions in the list
     * @param reverseFilter Optional flag used to reverse the result of the filter evaluation
     * @param focusResource Starting point for the evaluation, can be [bundle] if checking from root
     * @return Pair: Boolean indicating if the bundle passes the filter or not
     *         and String to use when logging the filter result
     */
    internal fun evaluateFilterConditionAsOr(
        filter: ReportStreamFilter?,
        bundle: Bundle,
        defaultResponse: Boolean,
        reverseFilter: Boolean = false,
        focusResource: Base = bundle,
    ): Pair<Boolean, String?> {
        if (filter.isNullOrEmpty()) {
            return Pair(defaultResponse, "defaultResponse")
        }
        val failingFilters = mutableListOf<String>()
        val exceptionFilters = mutableListOf<String>()
        val successfulFilters = mutableListOf<String>()
        filter.forEach { filterElement ->
            try {
                val filterElementResult = FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, focusResource, shorthandLookupTable, CustomFhirPathFunctions()),
                    focusResource,
                    bundle,
                    bundle,
                    filterElement
                )
                if (!filterElementResult) {
                    failingFilters += filterElement
                } else {
                    successfulFilters += filterElement
                }
            } catch (e: SchemaException) {
                actionLogger?.warn(EvaluateFilterConditionErrorMessage(e.message))
                exceptionFilters += filterElement
            }
        }

        return if (exceptionFilters.isNotEmpty()) {
            Pair(false, "(exception found) $exceptionFilters")
        } else if (successfulFilters.isNotEmpty()) {
            if (reverseFilter) {
                Pair(false, "(reversed) $successfulFilters")
            } else {
                Pair(true, null)
            }
        } else {
            if (reverseFilter) {
                Pair(true, null)
            } else {
                Pair(false, failingFilters.toString())
            }
        }
    }

    /**
     * Log the results of running filters (referenced by the given [filterName]) on items out of a [report] during the
     * "route" step for a [receiver], tracking the [filterType] and tying the results to a [receiver] and [bundle].
     * @param filterName Name of evaluated filter
     * @param bundle FHIR Bundle that was evaluated
     * @param reportId Report ID passed for logging purposes
     * @param receiver Receiver of the report
     * @param filterType Type of filter used
     * @param focusResource Starting point for the evaluation, used for logging
     */
    internal fun logFilterResults(
        filterName: String,
        bundle: Bundle,
        reportId: ReportId,
        actionHistory: ActionHistory,
        receiver: Receiver,
        filterType: ReportStreamFilterType,
        focusResource: Base,
    ) {
        val filteredTrackingElement = bundle.identifier.value ?: ""
        val filterResult = ReportStreamFilterResult(
            receiver.fullName,
            // The FHIR router will only ever process a single item
            1,
            filterName,
            emptyList(),
            filteredTrackingElement,
            filterType,
            filteredObservationDetails = if (focusResource is Observation) {
                "${focusResource.id} with system: |" +
                "${focusResource.code.coding.firstOrNull()?.system} |" +
                "and code: ${focusResource.code.coding.firstOrNull()?.code}"
            } else {
                null
            }
        )
        actionHistory.trackLogs(
            ActionLog(
                filterResult,
                filterResult.filteredTrackingElement,
                null, // we don't have accurate filteredIndex (rownums) to put here; due to juri filtering.
                reportId = reportId,
                action = actionHistory.action,
                type = ActionLogLevel.filter,
            )
        )
    }

    /**
     * Gets the applicable jurisdictional filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any juris filters configured for
     * this topic
     */
    internal fun getJurisFilters(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic == receiver.topic }?.jurisdictionalFilter
                ?: emptyList()
            ).plus(receiver.jurisdictionalFilter)
    }

    /**
     * Gets the applicable quality filters for a [receiver]. Gets applicable quality filters from the
     * parent organization and adds any quality filters from the receiver's settings. If there are no filters in that
     * result, returns the default filter instead.
     */
    internal fun getQualityFilters(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic == receiver.topic }?.qualityFilter
                ?: emptyList()
            ).plus(receiver.qualityFilter)
    }

    /**
     * Gets the applicable routing filters for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any routing filters configured for
     * this topic
     */
    internal fun getRoutingFilter(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic == receiver.topic }?.routingFilter
                ?: emptyList()
            ).plus(receiver.routingFilter)
    }

    /**
     * Gets the applicable processing mode filters for a [receiver]. Gets applicable processing mode
     * filters from the parent organization and adds any processing mode filters from the receiver's settings. If there
     * are no filters in that result, returns the default filter instead.
     */
    internal fun getProcessingModeFilter(
        receiver: Receiver,
        orgFilters: List<ReportStreamFilters>?,
    ): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic == receiver.topic }?.processingModeFilter
                ?: emptyList()
            ).plus(receiver.processingModeFilter)
    }

    /**
     * Gets the applicable condition filters for 'FULL_ELR' for a [receiver].
     */
    internal fun getConditionFilter(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic.isUniversalPipeline }?.conditionFilter
                ?: emptyList()
            ).plus(receiver.conditionFilter)
    }

    /**
     * Gets the applicable condition filters for 'FULL_ELR' for a [receiver].
     */
    internal fun getMappedConditionFilter(
        receiver: Receiver,
        orgFilters: List<ReportStreamFilters>?,
    ): List<ConditionFilter> {
        return (
            orgFilters?.firstOrNull { it.topic.isUniversalPipeline }?.mappedConditionFilter
                ?: emptyList()
            ).plus(receiver.mappedConditionFilter)
    }
}