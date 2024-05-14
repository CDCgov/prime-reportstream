package gov.cdc.prime.router.fhirengine.engine

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.EvaluateFilterConditionErrorMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
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
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.report.ReportService
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation
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
     * The name of the lookup table to load the shorthand replacement key/value pairs from
     */
    private val fhirPathFilterShorthandTableName = "fhirpath_filter_shorthand"

    /**
     * The name of the column in the shorthand replacement lookup table that will be used as the key.
     */
    private val fhirPathFilterShorthandTableKeyColumnName = "variable"

    /**
     * The name of the column in the shorthand replacement lookup table that will be used as the value.
     */
    private val fhirPathFilterShorthandTableValueColumnName = "fhirPath"

    /**
     * Lookup table `fhirpath_filter_shorthand` containing all the shorthand fhirpath replacements for filtering.
     */
    private val shorthandLookupTable by lazy { loadFhirPathShorthandLookupTable() }

    /**
     * Adds logs for reports that pass through various methods in the FHIRRouter
     */
    private var actionLogger: ActionLogger? = null

    /**
     * Load the fhirpath_filter_shorthand lookup table into a map if it can be found and has the expected columns,
     * otherwise log warnings and return an empty lookup table with the correct columns. This is valid since having
     * a populated lookup table is not required to run the universal pipeline routing
     *
     * @returns Map containing all the values in the fhirpath_filter_shorthand lookup table. Empty map if the
     * lookup table was not found, or it does not contain the expected columns. If an empty map is returned, a
     * warning indicating why will be logged.
     */
    internal fun loadFhirPathShorthandLookupTable(): MutableMap<String, String> {
        val lookup = metadata.findLookupTable(fhirPathFilterShorthandTableName)
        // log a warning and return an empty table if either lookup table is missing or has incorrect columns
        return if (lookup != null &&
            lookup.hasColumn(fhirPathFilterShorthandTableKeyColumnName) &&
            lookup.hasColumn(fhirPathFilterShorthandTableValueColumnName)
        ) {
            lookup.table.associate {
                it.getString(fhirPathFilterShorthandTableKeyColumnName) to
                    it.getString(fhirPathFilterShorthandTableValueColumnName)
            }.toMutableMap()
        } else {
            if (lookup == null) {
                logger.warn("Unable to find $fhirPathFilterShorthandTableName lookup table")
            } else {
                logger.warn(
                    "$fhirPathFilterShorthandTableName does not contain " +
                        "expected columns 'variable' and 'fhirPath'"
                )
            }
            emptyMap<String, String>().toMutableMap()
        }
    }

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

        // get the receivers that this bundle should go to
        val listOfReceivers = findReceiversForBundle(bundle, message.reportId, actionHistory, message.topic)

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

        // check if there are any receivers
        if (listOfReceivers.isNotEmpty()) {
            return listOfReceivers.flatMap { receiver ->
                val sources = emptyList<Source>()
                val report = Report(
                    Report.Format.FHIR,
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

                val nextEvent = ProcessEvent(
                    Event.EventAction.RECEIVER_FILTER,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )

                // upload new copy to blobstore
                val bodyString = FhirTranscoder.encode(bundle)
                val blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyString.toByteArray(),
                    report.name,
                    message.blobSubFolderName,
                    nextEvent.eventAction
                )
                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, report, blobInfo = blobInfo)

                val receiverObservationSummary = AzureEventUtils.getObservations(bundle)
                azureEventService.trackEvent(
                    ReportRouteEvent(
                        message.reportId,
                        report.id,
                        message.topic,
                        sender,
                        receiver.fullName,
                        receiverObservationSummary,
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
            val receiverObservationSummary = AzureEventUtils.getObservations(bundle)
            azureEventService.trackEvent(
                ReportRouteEvent(
                    message.reportId,
                    report.id,
                    message.topic,
                    sender,
                    null,
                    receiverObservationSummary,
                    fhirJson.length
                )
            )

            return emptyList()
        }
    }

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.DESTINATION_FILTERED_AT
    override val engineType: String = "DestinationFilter"

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
     * @param report Report object passed for logging purposes
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
     * @param report Report object passed for logging purposes
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
        var filteredTrackingElement = bundle.identifier.value ?: ""
        if (focusResource != bundle) {
            filteredTrackingElement += " at " + focusResource.idBase

            if (focusResource is Observation) {
                // for Observation-type elements, we use the code property when available
                // if more elements need specific logic, consider extending the FHIR libraries
                // instead of adding more if/else statements
                val coding = focusResource.code.coding.firstOrNull()
                if (coding != null) filteredTrackingElement += " with " + coding.system + " code: " + coding.code
            }
        }
        val filterResult = ReportStreamFilterResult(
            receiver.fullName,
            // The FHIR router will only ever process a single item
            1,
            filterName,
            emptyList(),
            filteredTrackingElement,
            filterType
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
}