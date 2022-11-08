package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilter
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
    queue: QueueAccess = QueueAccess,
) : FHIREngine(metadata, settings, db, blob, queue) {

    /**
     * Default Rules:
     *   Must have message ID, patient last name, patient first name, DOB, specimen type
     *   At least one of patient street, patient zip code, patient phone number, patient email
     *   At least one of order test date, specimen collection date/time, test result date
     */
    val qualityFilterDefault: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).id.exists()",
        "Bundle.entry.resource.ofType(Patient).name.family.exists()",
        "Bundle.entry.resource.ofType(Patient).name.given.count() > 0",
        "Bundle.entry.resource.ofType(Patient).birthDate.exists()",
        "Bundle.entry.resource.ofType(Specimen).type.exists()",
        "(Bundle.entry.resource.ofType(Patient).address.line.exists() or " +
            "Bundle.entry.resource.ofType(Patient).address.postalCode.exists() or " +
            "Bundle.entry.resource.ofType(Patient).telecom.exists())",
        "(" +
            "(Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or " +
            "Bundle.entry.resource.ofType(Specimen).collection.collected.exists()" +
            ") or " +
            "Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or " +
            "Bundle.entry.resource.ofType(Observation).effective.exists())"
    )

    /**
     * Default Rule:
     *  Must have a processing mode id of 'P'
     */
    val processingModeFilterDefault: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).meta" +
            ".extension('https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id')" +
            ".value.coding.code = 'P'"
    )

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

            // get the receivers that this bundle should go to
            val listOfReceivers = applyFilters(bundle)

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
     * FHIRPath expressions are run against the [bundle] to determine if the receiver should get this message\
     * @return list of receivers that should receive this bundle
     */
    internal fun applyFilters(bundle: Bundle): List<Receiver> {
        val listOfReceivers = mutableListOf<Receiver>()
        // find all receivers that have the full ELR topic and determine which applies
        val fullElrReceivers = settings.receivers.filter {
            it.customerStatus != CustomerStatus.INACTIVE &&
                it.topic == Topic.FULL_ELR.json_val
        }

        // get the quality filter default result for the bundle, but only if it is needed
        val qualFilterDefaultResult: Boolean by lazy {
            evaluateFilterCondition(
                qualityFilterDefault, bundle,
                false
            )
        }
        // get the processing mode (processing id) default result for the bundle, but only if it is needed
        val processingModeDefaultResult: Boolean by lazy {
            evaluateFilterCondition(
                processingModeFilterDefault, bundle,
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
            passes = passes &&
                evaluateFilterCondition(getQualityFilters(receiver, orgFilters), bundle, qualFilterDefaultResult)
            // ROUTING FILTER
            //  default: allowAll
            passes = passes &&
                evaluateFilterCondition(getRoutingFilter(receiver, orgFilters), bundle, true)
            // PROCESSING MODE FILTER
            //  default: allowAll
            passes = passes &&
                evaluateFilterCondition(
                    getProcessingModeFilter(receiver, orgFilters), bundle,
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
     * return [defaultResponse]
     * @return Boolean indicating if the bundle passes the filter or not
     */
    internal fun evaluateFilterCondition(
        filter: ReportStreamFilter?,
        bundle: Bundle,
        defaultResponse: Boolean
    ): Boolean {
        // the filter needs to check all expressions passed in, or if the filter is null or empty it will return the
        // default response
        return if (filter.isNullOrEmpty())
            defaultResponse
        else
            filter.all {
                FhirPathUtils.evaluateCondition(CustomContext(bundle, bundle), bundle, bundle, it)
            }
    }

    /**
     * Gets the applicable jurisdictional filters for 'FULL_ELR' for a [receiver]. Pulls from receiver configuration
     * first and looks at the parent organization if the receiver does not have any jurs filters configured for
     * this topic
     */
    internal fun getJurisFilters(receiver: Receiver, orgFilters: List<ReportStreamFilters>?): ReportStreamFilter {
        return (
            orgFilters?.firstOrNull { it.topic == Topic.FULL_ELR.json_val }?.jurisdictionalFilter
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
            orgFilters?.firstOrNull() { it.topic == Topic.FULL_ELR.json_val }?.qualityFilter
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
            orgFilters?.firstOrNull() { it.topic == Topic.FULL_ELR.json_val }?.routingFilter
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
            orgFilters?.firstOrNull() { it.topic == Topic.FULL_ELR.json_val }?.processingModeFilter
                ?: emptyList()
            ).plus(receiver.processingModeFilter)
    }
}