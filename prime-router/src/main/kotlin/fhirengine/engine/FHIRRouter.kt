package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
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
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import org.hl7.fhir.r4.model.Bundle

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
     * Default Rules:
     *   Must have message ID, patient last name, patient first name, DOB, specimen type
     *   At least one of patient street, patient zip code, patient phone number, patient email
     *   At least one of order test date, specimen collection date/time, test result date
     */
    val qualityFilterDefault: ReportStreamFilter = listOf(
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
            "%observation.effective.exists())"
    )

    /**
     * Default Rule:
     *  Must have a processing mode id of 'P'
     */
    val processingModeFilterDefault: ReportStreamFilter = listOf(
        "%processingId = 'P'"
    )

    /**
     * Lookup table `fhirpath_filter_shorthand` containing all of the shorthand fhirpath replacements for filtering.
     */
    private val shorthandLookupTable by lazy { loadFhirPathShorthandLookupTable() }

    /**
     * The regex used to locate shorthand variables in fhirpath filters
     */
    private val regexVariable = """%[`']?[A-Za-z][\w\-'`_]*""".toRegex()

    /**
     * Constants to make writing filter conditions shorter / more accessible. This will replace the shorthand
     * used in configuration filter expressions with the specified Fhir Path expression before the expression
     * is evaluated against the bundle. This allows for returning of collections, as well as handling 'exists()'
     * where needed. Format is %myVariable within the filters, and each variable used must be defined
     * as part of the shorthand collection of an exception will be raised. Make sure to verify if you need to use
     * 'exists' on your check - an 'unable to parse' will return 'false' but will not raise any other error due to the
     * underlying fhir library.
     *
     * The values used are located in the fhirpath_filter_shorthand lookup table.
     *
     * @returns A string that has all shorthand elements replaces with their mapped string from the lookup table.
     */
    internal fun replaceShorthand(input: String): String {
        var output = input
        regexVariable.findAll(input)
            .map { it.value }
            .sortedByDescending { it.length }
            .forEach {
                // remove %, ', and ` from start and end of string to be replaced
                val replacement = shorthandLookupTable[
                    it
                        .trimStart('%', '\'', '`')
                        .trimEnd('\'', '`')
                ]
                if (!replacement.isNullOrEmpty()) {
                    output = output.replace(it, replacement)
                }
            }
        return output
    }

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
                metadata = this.metadata
            )

            // get the receivers that this bundle should go to
            val listOfReceivers = applyFilters(bundle, report)

            // add the receivers, if any, to the fhir bundle
            if (listOfReceivers.isNotEmpty()) {
                FHIRBundleHelpers.addReceivers(bundle, listOfReceivers)
            }

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
     * Applies all filters to the list of all receivers with topic FULL_ELR that are not set as INACTIVE.
     * FHIRPath expressions are run against the [bundle] to determine if the receiver should get this message
     * As it goes through the filters, results are logged onto the provided [report]
     * @return list of receivers that should receive this bundle
     */
    internal fun applyFilters(bundle: Bundle, report: Report): List<Receiver> {
        val listOfReceivers = mutableListOf<Receiver>()
        // find all receivers that have the full ELR topic and determine which applies
        val fullElrReceivers = settings.receivers.filter {
            it.customerStatus != CustomerStatus.INACTIVE &&
                it.topic == Topic.FULL_ELR
        }

        // get the quality filter default result for the bundle, but only if it is needed
        val qualFilterDefaultResult: Boolean by lazy {
            evaluateFilterCondition(
                qualityFilterDefault,
                bundle,
                false
            )
        }

        // get the processing mode (processing id) default result for the bundle, but only if it is needed
        val processingModeDefaultResult: Boolean by lazy {
            evaluateFilterCondition(
                processingModeFilterDefault,
                bundle,
                false
            )
        }

        fullElrReceivers.forEach { receiver ->
            // get the receiver's organization, since we need to be able to find/combine the correct filters
            val orgFilters = settings.findOrganization(receiver.organizationName)!!.filters

            // Get the applicable filters, either receiver or organization level if there are no receiver filters

            // NOTE: these could all be combined into a single `if` statement, but that would reduce readability and
            // ability to debug this section so is being left split out like this

            // JURIS FILTER
            //  default: allowNone
            var passes = evaluateFilterCondition(getJurisFilters(receiver, orgFilters), bundle, false)

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
                qualFilterDefaultResult,
                receiver.reverseTheQualityFilter
            )

            // ROUTING FILTER
            //  default: allowAll
            passes = passes && evaluateFilterAndLogResult(
                getRoutingFilter(receiver, orgFilters),
                bundle,
                report,
                receiver,
                ReportStreamFilterType.ROUTING_FILTER,
                true
            )

            // PROCESSING MODE FILTER
            //  default: allowAll
            passes = passes && evaluateFilterAndLogResult(
                getProcessingModeFilter(receiver, orgFilters),
                bundle,
                report,
                receiver,
                ReportStreamFilterType.PROCESSING_MODE_FILTER,
                processingModeDefaultResult
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
     * that specific [filterType]
     * @return Boolean indicating if the bundle passes the filter or not
     *        Result will be negated if [reverseFilter] is true
     **/
    internal fun evaluateFilterAndLogResult(
        filters: ReportStreamFilter,
        bundle: Bundle,
        report: Report,
        receiver: Receiver,
        filterType: ReportStreamFilterType,
        defaultResponse: Boolean,
        reverseFilter: Boolean = false
    ): Boolean {
        val passes = evaluateFilterCondition(
            filters,
            bundle,
            defaultResponse,
            reverseFilter
        )
        if (!passes) {
            logFilterResults(filters, bundle, report, receiver, filterType)
        }

        return passes
    }

    /**
     * Takes a [bundle] and [filter], evaluates if the bundle passes the filter. If the filter is null,
     * return [defaultResponse]
     * @return Boolean indicating if the bundle passes the filter or not
     *          Result will be negated if [reverseFilter] is true
     */
    internal fun evaluateFilterCondition(
        filter: ReportStreamFilter?,
        bundle: Bundle,
        defaultResponse: Boolean,
        reverseFilter: Boolean = false
    ): Boolean {
        // the filter needs to check all expressions passed in, or if the filter is null or empty it will return the
        // default response
        val result = if (filter.isNullOrEmpty()) {
            defaultResponse
        } else {
            filter.all {
                FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, bundle, shorthandLookupTable),
                    bundle,
                    bundle,
                    replaceShorthand(it)
                )
            }
        }
        return if (reverseFilter) !result else result
    }

    /**
     * Log the results of running [filters] on items out of a [report] during the "route" step
     * for a [receiver], tracking the [filterType] and tying the results to a [receiver] and [bundle].
     */
    internal fun logFilterResults(
        filters: ReportStreamFilter,
        bundle: Bundle,
        report: Report,
        receiver: Receiver,
        filterType: ReportStreamFilterType
    ) {
        report.filteringResults.add(
            ReportStreamFilterResult(
                receiver.fullName,
                report.itemCount,
                filters.toString(),
                emptyList(),
                bundle.identifier.value ?: "",
                filterType
            )
        )
    }

    /**
     * Gets the applicable jurisdictional filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any jurs filters configured for
     * this topic
     */
    internal fun getJurisFilters(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic == Topic.FULL_ELR }?.jurisdictionalFilter
                ?: emptyList()
            ).plus(receiver.jurisdictionalFilter)
    }

    /**
     * Gets the applicable quality filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any quality filters configured for
     * this topic
     */
    internal fun getQualityFilters(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull() { it.topic == Topic.FULL_ELR }?.qualityFilter
                ?: emptyList()
            ).plus(receiver.qualityFilter)
    }

    /**
     * Gets the applicable routing filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any routing filters configured for
     * this topic
     */
    internal fun getRoutingFilter(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull() { it.topic == Topic.FULL_ELR }?.routingFilter
                ?: emptyList()
            ).plus(receiver.routingFilter)
    }

    /**
     * Gets the applicable processing mode filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any processing mode filters configured
     * for this topic
     */
    internal fun getProcessingModeFilter(receiver: Receiver, orgFilters: List<ReportStreamFilters>?):
        ReportStreamFilter {
        return (
            orgFilters?.firstOrNull() { it.topic == Topic.FULL_ELR }?.processingModeFilter
                ?: emptyList()
            ).plus(receiver.processingModeFilter)
    }
}