package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
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
import gov.cdc.prime.router.azure.observability.event.ReportCreatedEvent
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.fhirengine.utils.addMappedCondition
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Bundle
import org.jooq.Field
import java.time.OffsetDateTime

/**
 * Process a message off of the raw-elr azure queue, convert it into FHIR, and store for next step.
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRConverter(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
) : FHIREngine(metadata, settings, db, blob, azureEventService) {

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.PROCESSED_AT

    override val engineType: String = "Convert"

    /**
     * Accepts a [message] in either HL7 or FHIR format
     * HL7 messages will be converted into FHIR.
     * FHIR messages will be decoded and saved
     *
     * [message] is the incoming message to be turned into FHIR and saved
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        return when (message) {
            is FhirConvertQueueMessage -> {
                fhirEngineRunResults(message, message.schemaName, actionLogger, actionHistory)
            }
            else -> {
                throw RuntimeException(
                    "Message was not a FhirConvert and cannot be processed: $message"
                )
            }
        }
    }

    private fun fhirEngineRunResults(
        queueMessage: ReportPipelineMessage,
        schemaName: String,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        val format = Report.getFormatFromBlobURL(queueMessage.blobURL)
        logger.trace("Processing $format data for FHIR conversion.")
        val fhirBundles = when (format) {
            Report.Format.HL7, Report.Format.HL7_BATCH -> getContentFromHL7(queueMessage, actionLogger)
            Report.Format.FHIR -> getContentFromFHIR(queueMessage, actionLogger)
            else -> throw NotImplementedError("Invalid format $format ")
        }
        if (fhirBundles.isNotEmpty()) {
            logger.debug("Generated ${fhirBundles.size} FHIR bundles.")
            actionHistory.trackExistingInputReport(queueMessage.reportId)
            val transformer = getTransformerFromSchema(
                schemaName
            )
            return fhirBundles.mapIndexed { bundleIndex, bundle ->
                // conduct FHIR Transform
                transformer?.process(bundle)

                // 'stamp' observations with their condition code
                bundle.getObservations().forEach {
                    it.addMappedCondition(metadata).run {
                        actionLogger.getItemLogger(bundleIndex + 1, it.id)
                            .setReportId(queueMessage.reportId)
                            .warn(this)
                    }
                }

                // make a 'report'
                val report = Report(
                    Report.Format.FHIR,
                    emptyList(),
                    1,
                    itemLineage = listOf(
                        ItemLineage()
                    ),
                    metadata = this.metadata,
                    topic = queueMessage.topic,
                )

                // create item lineage
                report.itemLineages = listOf(
                    ItemLineage(
                        null,
                        queueMessage.reportId,
                        bundleIndex,
                        report.id,
                        1,
                        null,
                        null,
                        null,
                        report.getItemHashForRow(1)
                    )
                )

                // create route event
                val routeEvent = ProcessEvent(
                    Event.EventAction.ROUTE,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                )

                // upload to blobstore
                val bodyBytes = FhirTranscoder.encode(bundle).toByteArray()
                val blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyBytes,
                    report.name,
                    queueMessage.blobSubFolderName,
                    routeEvent.eventAction
                )

                // track created report
                actionHistory.trackCreatedReport(routeEvent, report, blobInfo = blobInfo)
                azureEventService.trackEvent(
                    ReportCreatedEvent(
                        report.id,
                        queueMessage.topic
                    )
                )

                FHIREngineRunResult(
                    routeEvent,
                    report,
                    blobInfo.blobUrl,
                    FhirRouteQueueMessage(
                        report.id,
                        blobInfo.blobUrl,
                        BlobAccess.digestToString(blobInfo.digest),
                        queueMessage.blobSubFolderName,
                        queueMessage.topic
                    )
                )
            }
        }
        return emptyList()
    }

    /**
     * Loads a transformer schema with [schemaName] and returns it.
     * Returns null if [schemaName] is the empty string.
     * Using this function instead of calling the constructor directly simplifies the process of mocking the
     * transformer in tests.
     */
    fun getTransformerFromSchema(schemaName: String): FhirTransformer? {
        return if (schemaName.isNotBlank()) {
            FhirTransformer(schemaName)
        } else {
            null
        }
    }

    /**
     * Converts an incoming HL7 [queueMessage] into FHIR bundles and keeps track of any validation
     * errors when reading the message into [actionLogger]
     *
     * @return one or more FHIR bundles
     */
    internal fun getContentFromHL7(
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
    ): List<Bundle> {
        // create the hl7 reader
        val hl7Reader = HL7Reader(actionLogger)
        // get the hl7 from the blob store
        val hl7rawmessages = queueMessage.downloadContent()
        val hl7profile = HL7Reader.getMessageProfile(hl7rawmessages)
        val hl7messages = hl7Reader.getMessages(hl7rawmessages)

        val bundles = if (actionLogger.hasErrors()) {
            val errMessage = actionLogger.errors.joinToString("\n") { it.detail.message }
            logger.error(errMessage)
            actionLogger.error(InvalidReportMessage(errMessage))
            emptyList()
        } else {
            // use fhir transcoder to turn hl7 into FHIR
            // search hl7 profile map and create translator with config path if found
            when (val configPath = HL7Reader.profileDirectoryMap[hl7profile]) {
                null -> hl7messages.map {
                    HL7toFhirTranslator().translate(it)
                }
                else -> hl7messages.map {
                    HL7toFhirTranslator(configPath).translate(it)
                }
            }
        }

        return bundles
    }

    /**
     * Decodes a FHIR [queueMessage] into FHIR bundles and keeps track of any validation
     * errors when reading the message into [actionLogger]
     * @return a list containing a FHIR bundle
     */
    internal fun getContentFromFHIR(
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
    ): List<Bundle> {
        return FhirTranscoder.getBundles(queueMessage.downloadContent(), actionLogger)
    }

    /**
     * Inserts a 'route' task into the task table for the [report] in question. This is just a pass-through function
     * but is present here for proper separation of layers and testing. This may need to be modified in the future.
     * The task will track the [report] in the [format] specified and knows it is located at [reportUrl].
     * [nextAction] specifies what is going to happen next for this report
     *
     */
    private fun insertRouteTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event,
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }
}