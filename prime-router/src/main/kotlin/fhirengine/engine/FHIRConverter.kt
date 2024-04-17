package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.ParserConfiguration
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator.ParseFailureError
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.Streams
import fhirengine.engine.IProcessedItem
import fhirengine.engine.ProcessedFHIRItem
import fhirengine.engine.ProcessedHL7Item
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
import gov.cdc.prime.router.logging.LogMeasuredTime
import gov.cdc.prime.router.validation.IMessageValidator
import io.github.oshai.kotlinlogging.withLoggingContext
import org.apache.commons.lang3.exception.ExceptionUtils
import org.hl7.fhir.r4.model.Bundle
import org.jooq.Field
import java.time.OffsetDateTime
import java.util.UUID
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
    // TODO delete
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
        actionLogger.setReportId(queueMessage.reportId)
        actionHistory.trackExistingInputReport(queueMessage.reportId)
        val format = Report.getFormatFromBlobURL(queueMessage.blobURL)
        logger.trace("Processing $format data for FHIR conversion.")

        val fhirBundles = process(format, queueMessage, actionLogger)

        // TODO add more logging here
        if (fhirBundles.isNotEmpty()) {
            val transformer = getTransformerFromSchema(
                schemaName
            )
            return fhirBundles.mapIndexed { bundleIndex, bundle ->
                // conduct FHIR Transform
                transformer?.process(bundle)

                // 'stamp' observations with their condition code
                bundle.getObservations().forEach {
                    // TODO this is not quite right since there might be less bundles than items because of parsing and validating
                    // TECH DEBT TICKET
                    it.addMappedConditions(metadata).run {
                        actionLogger.getItemLogger(bundleIndex + 1, it.id)
                            .setReportId(queueMessage.reportId)
                            .warn(this)
                    }
                }

                // TODO: create tech debt ticket to clean all of this up
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

                // TODO test that validate the lineage
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

    // TODO delete
    private fun measureProcessingItems(
        reportId: UUID,
        format: Report.Format,
        getProcessedItems: () -> List<IProcessedItem<*>>,
    ): List<IProcessedItem<*>> {
        return LogMeasuredTime.measureAndLogDurationWithReturnedValue(
            "Processed raw message into items",
            mapOf(
                "reportId" to reportId.toString(),
                "format" to format.name
            )
        ) {
            getProcessedItems()
        }
    }

    /**
     * TODO
     */
    internal fun process(
        format: Report.Format,
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
        // TODO should this get configured on the sender?
        routeMessageWithInvalidItems: Boolean = true,
    ): List<Bundle> {
        val validator = queueMessage.topic.validator
        val fhirBundles = if (useUpdatedDebatch) {
            val rawMessage = queueMessage.downloadContent()
            if (rawMessage.isBlank()) {
                actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
                emptyList()
            } else {
                // TODO test limit
                val processedItems = when (format) {
                    Report.Format.HL7, Report.Format.HL7_BATCH -> {
                        try {
                            measureProcessingItems(queueMessage.reportId, format) {
                                getBundlesFromRawHL7(rawMessage, validator)
                            }
                        } catch (ex: ParseFailureError) {
                            actionLogger.error(
                                InvalidReportMessage("Parse error while attempting to iterate over HL7 raw message")
                            )
                            emptyList()
                        }
                    }
                    Report.Format.FHIR -> {
                        measureProcessingItems(queueMessage.reportId, format) {
                            getBundlesFromRawFHIR(rawMessage, validator)
                        }
                    }
                    else -> {
                        logger.error("Received unsupported report format: $format")
                        actionLogger.error(InvalidReportMessage("Received unsupported report format: $format"))
                        emptyList()
                    }
                }

                val areAllItemsParsedAndValid = processedItems.all { it.getError() == null }
                val bundles = processedItems.mapNotNull { item ->
                    val error = item.getError()
                    if (error != null) {
                        actionLogger.error(error)
                    }
                    item.bundle
                }

                withLoggingContext(
                    mapOf(
                        "reportId" to queueMessage.reportId.toString(),
                        "format" to format.name,
                        "itemCount" to processedItems.size.toString(),
                        "bundlesProducedCount" to bundles.size.toString(),
                        "routeMessageWithInvalidItems" to routeMessageWithInvalidItems.toString()
                    )
                ) {
                    logger.info("Successfully processed raw report")
                }

                if (!areAllItemsParsedAndValid && !routeMessageWithInvalidItems) {
                    emptyList()
                } else {
                    bundles
                }
            }
        } else {
            // TODO delete
            when (format) {
                Report.Format.HL7, Report.Format.HL7_BATCH -> getContentFromHL7(queueMessage, actionLogger)
                Report.Format.FHIR -> getContentFromFHIR(queueMessage, actionLogger)
                else -> throw NotImplementedError("Invalid format $format ")
            }
        }
        return fhirBundles
    }

    /**
     * TODO
     */
    private fun getBundlesFromRawHL7(
        rawMessage: String,
        validator: IMessageValidator,
    ): MutableList<IProcessedItem<Message>> {
        val itemStream =
            Hl7InputStreamMessageStringIterator(rawMessage.byteInputStream()).asSequence()
                .mapIndexed { index, rawItem ->
                    ProcessedHL7Item(rawItem, index)
                }.toList()

        return maybeParallelize(itemStream.size, itemStream.stream()).map { item ->
            try {
                val hl7MessageType = HL7Reader.getMessageType(item.rawItem.split(Regex("[\n\r]"))[0])
                val parseConfiguration = HL7Reader.messageToConfigMap[hl7MessageType]
                val message = getHL7ParsingContext(item.parseConfiguration).pipeParser.parse(item.rawItem)
                item.updateParsed(message).setParseConfiguration(parseConfiguration)
            } catch (e: ParseFailureError) {
                item.updateParsed(
                    InvalidItemActionLogDetail(
                        ErrorCode.INVALID_MSG_PARSE,
                        item.index,
                        "exception while parsing HL7: ${ExceptionUtils.getRootCause(e).message}",
                    )
                )
            } catch (e: HL7Exception) {
                item.updateParsed(
                    InvalidItemActionLogDetail(
                        ErrorCode.INVALID_MSG_PARSE,
                        item.index,
                        "exception while parsing HL7: ${ExceptionUtils.getRootCause(e).message}",
                    )
                )
            }
        }.map { item ->
            if (item.parsedItem != null) {
                val validationResult = validator.validate(item.parsedItem)
                if (validationResult.isValid()) {
                    try {
                        val bundle = when (val parseConfiguration = item.parseConfiguration) {
                            null -> HL7toFhirTranslator().translate(item.parsedItem)
                            else -> HL7toFhirTranslator(parseConfiguration.HL7toFHIRMappingLocation)
                                .translate(item.parsedItem)
                        }
                        item.setBundle(bundle)
                    } catch (ex: Exception) {
                        item.setConversionError(
                            InvalidItemActionLogDetail(
                                ErrorCode.INVALID_MSG_CONVERSION,
                                item.index,
                                "exception while converting HL7: ${ex.message}"
                            )
                        )
                    }
                } else {
                    item.updateValidation(
                        InvalidItemActionLogDetail(
                            ErrorCode.INVALID_MSG_VALIDATION,
                            item.index,
                            validationResult.getErrorsMessage()
                        )
                    )
                }
            } else {
                item
            }
        }.collect(Collectors.toList())
    }

    private fun getHL7ParsingContext(
        messageParseConfiguration: HL7Reader.Companion.MessageParseConfiguration?,
    ): HapiContext {
        return if (messageParseConfiguration == null) {
            DefaultHapiContext(ValidationContextFactory.noValidation())
        } else {
            DefaultHapiContext(
                ParserConfiguration(),
                ValidationContextFactory.noValidation(),
                CanonicalModelClassFactory(messageParseConfiguration.messageModelClass),
            )
        }
    }

    /**
     * TODO
     */
    private fun getBundlesFromRawFHIR(
        rawMessage: String,
        validator: IMessageValidator,
    ): MutableList<IProcessedItem<Bundle>> {
        val lines = rawMessage.split("\n")
        return maybeParallelize(
            lines.size,
            Streams.mapWithIndex(lines.stream()) { rawItem, index ->
                ProcessedFHIRItem(rawItem, index.toInt())
            }
        ).map { item ->
            try {
                val bundle = FhirTranscoder.decode(item.rawItem)
                item.updateParsed(bundle)
            } catch (ex: DataFormatException) {
                item.updateParsed(
                    InvalidItemActionLogDetail(
                        ErrorCode.INVALID_MSG_PARSE,
                        item.index,
                        "exception while parsing FHIR: ${ex.message}"
                    )
                )
            }
        }.map { item ->
            if (item.parsedItem != null) {
                val validationResult = validator.validate(item.parsedItem)
                if (validationResult.isValid()) {
                    item.setBundle(item.parsedItem)
                } else {
                    item.updateValidation(
                        InvalidItemActionLogDetail(
                            ErrorCode.INVALID_MSG_VALIDATION,
                            item.index,
                            validationResult.getErrorsMessage()
                        )
                    )
                }
            } else {
                item
            }
        }.collect(Collectors.toList())
    }

    /**
     * TODO
     */
    private fun <ProcessedItemType : IProcessedItem<*>> maybeParallelize(
        streamSize: Int,
        it: Stream<ProcessedItemType>,
    ): Stream<ProcessedItemType> =
        if (streamSize > sequentialLimit) {
            withLoggingContext(mapOf("numberOfItems" to streamSize.toString())) {
                logger.info("Generating FHIR bundles in parallel")
            }
            it.parallel()
        } else {
            withLoggingContext(mapOf("numberOfItems" to streamSize.toString())) {
                logger.info("Generating FHIR bundles in serial")
            }
            it
        }

    /**
     * TODO
     */
    class InvalidItemActionLogDetail(
        override val errorCode: ErrorCode,
        val index: Int,
        @JsonProperty
        private val errorDetail: String,
    ) : ActionLogDetail {

        override val scope: ActionLogScope = ActionLogScope.report

        override val message: String =
            """Item ${index + 1} in the report was not ${
                when (errorCode) {
                    ErrorCode.INVALID_MSG_PARSE -> "parseable"
                    ErrorCode.INVALID_MSG_VALIDATION -> "valid"
                    ErrorCode.INVALID_MSG_CONVERSION -> "convertible"
                    else -> "processed"
                }
            }. Reason: $errorDetail""".trimIndent()
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

    // TODO: delete
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

    // TODO delete
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
}