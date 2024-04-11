package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v27.message.ORU_R01
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator.ParseFailureError
import com.google.common.collect.Streams
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ErrorCode
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
import gov.cdc.prime.router.fhirengine.utils.addMappedConditions
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.validation.IMessageValidator
import org.hl7.fhir.r4.model.Bundle
import org.jooq.Field
import java.time.OffsetDateTime
import java.util.stream.Collectors
import java.util.stream.Stream

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
    private val useUpdatedDebatch: Boolean = true,
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

        // Parse batch
        // Per message
        // validation
        // pass to route if success
        // log to action $index: bad message
        // pass validation

        val fhirBundles = debatchFHIRBundles(format, queueMessage, actionLogger)
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
                    it.addMappedConditions(metadata).run {
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

    // TODO subclass for kind of item
    data class DebatchItemResult<ParsedType>(
        val rawItem: String,
        val index: Int,
        val parsedItem: ParsedType? = null,
        val parseError: ActionLogDetail? = null,
        val validationError: ActionLogDetail? = null,
        val bundle: Bundle? = null,
    ) {
        fun updateParsed(error: ActionLogDetail?): DebatchItemResult<ParsedType> {
            return this.copy(parseError = error)
        }

        fun updateParsed(parsed: ParsedType): DebatchItemResult<ParsedType> {
            return this.copy(parsedItem = parsed)
        }

        fun updateValidation(error: ActionLogDetail): DebatchItemResult<ParsedType> {
            if (parseError == null && parsedItem != null) {
                return this.copy(validationError = error)
            }
            throw RuntimeException("Validation should not be set since item was not parseable")
        }

        fun setBundle(bundle: Bundle): DebatchItemResult<ParsedType> {
            if (parseError == null && validationError == null) {
                return this.copy(bundle = bundle)
            }
            throw RuntimeException("Bundle should not be set if the item was not parseable or valid")
        }

        fun getError(): ActionLogDetail? {
            return parseError ?: validationError
        }
    }

    private fun debatchFHIRBundles(
        format: Report.Format,
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
        routeMessageWithInvalidItems: Boolean = true,
    ): List<Bundle> {
        val validator = queueMessage.topic.validator
        val fhirBundles = if (useUpdatedDebatch) {
            val rawMessage = queueMessage.downloadContent()
            if (rawMessage.isBlank()) {
                actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
                emptyList()
            } else {
                // TODO timing
                // TODO test limit
                val debatchItemResults = when (format) {
                    Report.Format.HL7, Report.Format.HL7_BATCH -> {
                        getBundlesFromRawHL7(rawMessage, validator)
                    }
                    Report.Format.FHIR -> {
                        getBundlesFromRawFHIR(rawMessage, validator)
                    }
                    else -> {
                        logger.error("Received unsupported report format: $format")
                        actionLogger.error(InvalidReportMessage("Received unsupported report format: $format"))
                        emptyList()
                    }
                }

                val allItemsParsedAndValid = debatchItemResults.all { it.getError() == null }
                val bundles = debatchItemResults.mapNotNull { item ->
                    val error = item.getError()
                    if (error != null) {
                        actionLogger.error(error)
                    }
                    item.bundle
                }
                if (!allItemsParsedAndValid && !routeMessageWithInvalidItems) {
                    emptyList()
                } else {
                    bundles
                }
            }
        } else {
            when (format) {
                Report.Format.HL7, Report.Format.HL7_BATCH -> getContentFromHL7(queueMessage, actionLogger)
                Report.Format.FHIR -> getContentFromFHIR(queueMessage, actionLogger)
                else -> throw NotImplementedError("Invalid format $format ")
            }
        }
        return fhirBundles
    }

    private fun getBundlesFromRawHL7(
        rawMessage: String,
        validator: IMessageValidator,
    ): MutableList<DebatchItemResult<Message>> {
        // TODO: this will be sourced from the profile
        val hl7profile = HL7Reader.getMessageProfile(rawMessage)
        val modelClass = ORU_R01::class.java
        context.modelClassFactory = CanonicalModelClassFactory(modelClass)
        context.validationContext = validationContext
        context.parserConfiguration.isValidating = false
        val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream(), context)

        // Ugly! But we want to keep around the raw HL7 string and this is how we get it
        // TODO consider just splitting with a look ahead regex on MSH
        val myWrappedField = Hl7InputStreamMessageIterator::class.java.getDeclaredField("myWrapped")
        myWrappedField.isAccessible = true
        val wrappedIterator = myWrappedField.get(iterator) as Hl7InputStreamMessageStringIterator
        val myNextField = Hl7InputStreamMessageStringIterator::class.java.getDeclaredField("myNext")
        myNextField.isAccessible = true

        var index = 1
        val debatchItems = mutableListOf<DebatchItemResult<Message>>()
        while (iterator.hasNext()) {
            val rawItem = myNextField.get(wrappedIterator) as String
            val item = DebatchItemResult<Message>(rawItem, index)
            try {
                val message = iterator.next()
                debatchItems.add(item.updateParsed(message))
            } catch (e: ParseFailureError) {
                debatchItems.add(
                    item.updateParsed(InvalidReportMessage("${item.index} was not parseable ${e.message}"))
                )
            }
            index++
        }

        return maybeParallelize(debatchItems.size, debatchItems.stream()).map { item ->
            if (item.parsedItem != null) {
                val validationResult = validator.validate(item.parsedItem)
                if (validationResult.isValid()) {
                    val bundle = // use fhir transcoder to turn hl7 into FHIR
                    // search hl7 profile map and create translator with config path if found
                        // TODO update converter library to optionally throw the exception
                        when (val configPath = HL7Reader.profileDirectoryMap[hl7profile]) {
                            null -> HL7toFhirTranslator().translate(item.parsedItem)
                            else -> HL7toFhirTranslator(configPath).translate(item.parsedItem)
                        }
                    item.setBundle(bundle)
                } else {
                    item.updateValidation(InvalidReportMessage("${item.index} was not valid"))
                }
            } else {
                item
            }
        }.collect(Collectors.toList())
    }

    private fun getBundlesFromRawFHIR(
        rawMessage: String,
        validator: IMessageValidator,
    ): MutableList<DebatchItemResult<Bundle>> {
        val lines = rawMessage.split("\n")
        return maybeParallelize(
            lines.size,
            Streams.mapWithIndex(lines.stream()) { rawItem, index ->
                DebatchItemResult<Bundle>(rawItem, index.toInt())
            }
        ).map { debatchItem ->
            try {
                val bundle = FhirTranscoder.decode(debatchItem.rawItem)
                debatchItem.updateParsed(bundle)
            } catch (ex: DataFormatException) {
                val error = InvalidReportMessage("${debatchItem.index} was invalid")
                debatchItem.updateParsed(error)
            }
        }.map { debatchItem ->
            if (debatchItem.parsedItem != null) {
                val validationResult = validator.validate(debatchItem.parsedItem)
                if (validationResult.isValid()) {
                    debatchItem.setBundle(debatchItem.parsedItem)
                } else {
                    debatchItem.updateValidation(InvalidReportMessage("${debatchItem.index} was invalid"))
                }
            } else {
                debatchItem
            }
        }.collect(Collectors.toList())
    }

    private fun <ParseType> maybeParallelize(
        streamSize: Int,
        it: Stream<DebatchItemResult<ParseType>>,
    ): Stream<DebatchItemResult<ParseType>> =
        if (streamSize > sequentialLimit) {
            it.parallel()
        } else {
            it
        }

    // TODO generated message from the error
    class InvalidReportMessage(override val message: String, override val errorCode: ErrorCode) : ActionLogDetail {
        override val scope: ActionLogScope = ActionLogScope.item
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