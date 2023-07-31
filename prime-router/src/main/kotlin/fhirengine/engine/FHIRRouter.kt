package gov.cdc.prime.router.fhirengine.engine

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.EvaluateFilterConditionErrorMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
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
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation

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
    queue: QueueAccess = QueueAccess
) : FHIREngine(metadata, settings, db, blob, queue) {

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
     * Default Rules for quality filter on FULL_ELR topic:
     *   Must have message ID, patient last name, patient first name, DOB, specimen type
     *   At least one of patient street, patient zip code, patient phone number, patient email
     *   At least one of order test date, specimen collection date/time, test result date
     */
    private val fullElrQualityFilterDefault: ReportStreamFilter = listOf(
        "%messageId.exists()",
        "%patient.name.family.exists()",
        "%patient.name.given.count() > 0",
        "%patient.birthDate.exists()",
        "%specimen.type.exists()",
        "(%patient.address.line.exists() or " +
            "%patient.address.postalCode.exists() or " +
            "%patient.telecom.exists())",
        "(" +
            "(%specimen.collection.collectedPeriod.exists() or " +
            "%specimen.collection.collected.exists()" +
            ") or " +
            "%serviceRequest.occurrence.exists() or " +
            "%observation.effective.exists())",
    )

    /**
     * Default Rules for quality filter on ETOR_TI topic:
     *   Must have message ID
     */
    private val etorTiQualityFilterDefault: ReportStreamFilter = listOf(
        "%messageId.exists()",
    )

    /**
     * Default Rules for quality filter on ELR_ELIMS topic:
     *   no rules; completely open
     */
    private val elrElimsQualityFilterDefault: ReportStreamFilter = listOf(
        "true",
    )

    /**
     * Maps topics to default quality filters so that topic-dependent defaults can be used
     */
    val qualityFilterDefaults = mapOf(
        Pair(Topic.FULL_ELR, fullElrQualityFilterDefault),
        Pair(Topic.ETOR_TI, etorTiQualityFilterDefault),
        Pair(Topic.ELR_ELIMS, elrElimsQualityFilterDefault)
    )

    /**
     * Default Rule (used for ETOR_TI and FULL_ELR):
     *  Must have a processing mode id of 'P'
     */
    private val processingModeFilterDefault: ReportStreamFilter = listOf(
        "%processingId.exists() and %processingId = 'P'"
    )

    /**
     * Maps topics to default processing mode filters so that topic-dependent defaults can be used
     */
    val processingModeDefaults = mapOf(
        Pair(Topic.FULL_ELR, processingModeFilterDefault),
        Pair(Topic.ETOR_TI, processingModeFilterDefault)
    )

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
    private fun loadFhirPathShorthandLookupTable(): MutableMap<String, String> {
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
    override fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory
    ) {
        logger.trace("Processing HL7 data for FHIR conversion.")
        this.actionLogger = actionLogger
        try {
            // track input report
            actionHistory.trackExistingInputReport(message.reportId)

            // pull fhir document and parse FHIR document
            val bundle = FhirTranscoder.decode(message.downloadContent())

            // create report object
            val sources = emptyList<Source>()
            val report = Report(
                Report.Format.FHIR,
                sources,
                1,
                metadata = this.metadata,
                topic = message.topic,
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

            // get the receivers that this bundle should go to
            val listOfReceivers = applyFilters(bundle, report, message.topic)

            // check if there are any receivers
            if (listOfReceivers.isNotEmpty()) {
                // this bundle has receivers; send message to translate function
                // add the receivers to the fhir bundle
                FHIRBundleHelpers.addReceivers(bundle, listOfReceivers, shorthandLookupTable)

                // create translate event
                val nextEvent = ProcessEvent(
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
                    nextEvent.eventAction
                )

                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, report, blobInfo)

                // insert translate task into Task table
                this.insertTranslateTask(
                    report,
                    report.bodyFormat.toString(),
                    blobInfo.blobUrl,
                    nextEvent
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
                        message.blobSubFolderName,
                        message.topic,
                    ).serialize(),
                    this.queueVisibilityTimeout
                )
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

                // ensure tracking is set
                actionHistory.trackCreatedReport(nextEvent, report, null)

                // nullify the previous task next_action
                db.updateTask(
                    message.reportId,
                    TaskAction.none,
                    null,
                    null,
                    finishedField = Tables.TASK.ROUTED_AT,
                    null
                )
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }

    /**
     * Inserts a 'translate' task into the task table for the [report] in question. This is just a pass-through
     * function but is present here for proper separation of layers and testing. This may need to be modified in
     * the future.
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
     * Applies all filters to the list of all receivers with topic with a topic matching [topic] that are not set as
     * INACTIVE. FHIRPath expressions are run against the [bundle] to determine if the receiver should get this message
     * As it goes through the filters, results are logged onto the provided [report]
     * @return list of receivers that should receive this bundle
     */
    internal fun applyFilters(bundle: Bundle, report: Report, topic: Topic): List<Receiver> {
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
                report,
                receiver,
                ReportStreamFilterType.QUALITY_FILTER,
                false,
                receiver.reverseTheQualityFilter,
            )

            // ROUTING FILTER
            //  default: allowAll
            passes = passes && evaluateFilterAndLogResult(
                getRoutingFilter(receiver, orgFilters),
                bundle,
                report,
                receiver,
                ReportStreamFilterType.ROUTING_FILTER,
                defaultResponse = true,
            )

            // PROCESSING MODE FILTER
            //  default: allowAll
            passes = passes && evaluateFilterAndLogResult(
                getProcessingModeFilter(receiver, orgFilters),
                bundle,
                report,
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
                        report,
                        receiver,
                        ReportStreamFilterType.CONDITION_FILTER,
                        defaultResponse = true,
                        reverseFilter = false,
                        focusResource = observation,
                        useOr = true
                    )
                }
                )

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
        report: Report,
        receiver: Receiver,
        filterType: ReportStreamFilterType,
        defaultResponse: Boolean,
        reverseFilter: Boolean = false,
        focusResource: Base = bundle,
        useOr: Boolean = false
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
            val filterToLog = "${
            if (isDefaultFilter(filterType, filters)) "(default filter) "
            else ""
            }${failingFilterName ?: "unknown"}"
            logFilterResults(filterToLog, bundle, report, receiver, filterType, focusResource)
        }
        return passes
    }

    /**
     * With a given [filterType], returns whether the [filter] is the default filter for that type. If [filter] is
     * an equivalent filter to the default, but does not point to the actual default, this function will still return
     * false.
     */
    internal fun isDefaultFilter(filterType: ReportStreamFilterType, filter: ReportStreamFilter): Boolean {
        // The usage of === (referential equality operator) below is intentional and necessary; we only want to
        // return true if the filter references a default filter, not if it only happens to be equivalent to the default
        return when (filterType) {
            ReportStreamFilterType.QUALITY_FILTER -> qualityFilterDefaults.values.any { filter === it }
            ReportStreamFilterType.PROCESSING_MODE_FILTER -> processingModeDefaults.values.any { filter === it }
            else -> false
        }
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
            if (reverseFilter) Pair(false, "(reversed) $filter")
            else Pair(true, null)
        } else {
            if (reverseFilter) Pair(true, null)
            else Pair(false, failingFilters.toString())
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
                    filterElement
                )
                if (!filterElementResult) failingFilters += filterElement
                else successfulFilters += filterElement
            } catch (e: SchemaException) {
                actionLogger?.warn(EvaluateFilterConditionErrorMessage(e.message))
                exceptionFilters += filterElement
            }
        }

        return if (exceptionFilters.isNotEmpty()) {
            Pair(false, "(exception found) $exceptionFilters")
        } else if (successfulFilters.isNotEmpty()) {
            if (reverseFilter) Pair(false, "(reversed) $successfulFilters")
            else Pair(true, null)
        } else {
            if (reverseFilter) Pair(true, null)
            else Pair(false, failingFilters.toString())
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
        report: Report,
        receiver: Receiver,
        filterType: ReportStreamFilterType,
        focusResource: Base
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
        report.filteringResults.add(
            ReportStreamFilterResult(
                receiver.fullName,
                report.itemCount,
                filterName,
                emptyList(),
                filteredTrackingElement,
                filterType
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
        val receiverFilters = (
            orgFilters?.firstOrNull { it.topic == receiver.topic }?.qualityFilter
                ?: emptyList()
            ).plus(receiver.qualityFilter)
        return receiverFilters.ifEmpty { qualityFilterDefaults[receiver.topic] ?: emptyList() }
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
        val receiverFilters = (
            orgFilters?.firstOrNull { it.topic == receiver.topic }?.processingModeFilter
                ?: emptyList()
            ).plus(receiver.processingModeFilter)
        return receiverFilters.ifEmpty { processingModeDefaults[receiver.topic] ?: emptyList() }
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
}