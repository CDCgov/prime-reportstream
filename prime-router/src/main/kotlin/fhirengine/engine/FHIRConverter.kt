package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator.ParseFailureError
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
import gov.cdc.prime.router.fhirengine.utils.HL7Reader.Companion.parseHL7Message
import gov.cdc.prime.router.fhirengine.utils.addMappedConditions
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.logging.LogMeasuredTime
import gov.cdc.prime.router.validation.IMessageValidator
import io.github.oshai.kotlinlogging.withLoggingContext
import org.apache.commons.lang3.exception.ExceptionUtils
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

        // TODO ticket to add more logs below
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

    /**
     * Processes the raw report on the queue message by splitting it into individual items
     * and then parsing, validating and in the case of the HL7 format converting the item to FHIR.
     *
     * The function will record for each item where it fails in the processing and finally log errors to the passed
     * [actionLogger].
     *
     * Two important configurations for this function are:
     * - the [BaseEngine.sequentialLimit] that determines whether the items will be processed in parallel
     * - [routeReportWithInvalidItems] whether to items through when the entire report is not valid
     *
     * @param format the format of the items in the report
     * @param queueMessage the message that contains the url for the raw report
     * @param actionLogger keeps track of any errors (parsing, validation, conversion) that occur for individual items
     * @param routeReportWithInvalidItems controls the behavior when some items encounter errors
     *
     * @return the bundles that should get routed
     */
    internal fun process(
        format: Report.Format,
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
        // TODO should this get configured on the sender?
        routeReportWithInvalidItems: Boolean = true,
    ): List<Bundle> {
        val validator = queueMessage.topic.validator
        val rawReport = queueMessage.downloadContent()
        return if (rawReport.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
            emptyList()
        } else {
            // TODO test limit
            val processedItems = when (format) {
                Report.Format.HL7, Report.Format.HL7_BATCH -> {
                    try {
                        LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                            "Processed raw message into items",
                            mapOf(
                                "reportId" to queueMessage.reportId.toString(),
                                "format" to format.name
                            )
                        ) {
                            getBundlesFromRawHL7(rawReport, validator)
                        }
                    } catch (ex: ParseFailureError) {
                        actionLogger.error(
                            InvalidReportMessage("Parse error while attempting to iterate over HL7 raw message")
                        )
                        emptyList()
                    }
                }
                Report.Format.FHIR -> {
                    LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                        "Processed raw message into items",
                        mapOf(
                            "reportId" to queueMessage.reportId.toString(),
                            "format" to format.name
                        )
                    ) {
                        getBundlesFromRawFHIR(rawReport, validator)
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
                    actionLogger.getItemLogger(error.index + 1).error(error)
                }
                item.bundle
            }

            withLoggingContext(
                mapOf(
                    "reportId" to queueMessage.reportId.toString(),
                    "format" to format.name,
                    "itemCount" to processedItems.size.toString(),
                    "bundlesProducedCount" to bundles.size.toString(),
                    "routeMessageWithInvalidItems" to routeReportWithInvalidItems.toString()
                )
            ) {
                logger.info("Successfully processed raw report")
            }

            if (!areAllItemsParsedAndValid && !routeReportWithInvalidItems) {
                emptyList()
            } else {
                bundles
            }
        }
    }

    /**
     * Converts a raw HL7 string into validated FHIR bundles
     *
     * @param rawReport the string to convert into FHIR bundles
     * @param validator instance of a validator to use for each parsed FHIR bundle
     *
     * @return the [IProcessedItem] produced from the raw report
     */
    private fun getBundlesFromRawHL7(
        rawReport: String,
        validator: IMessageValidator,
    ): List<IProcessedItem<Message>> {
        val itemStream =
            Hl7InputStreamMessageStringIterator(rawReport.byteInputStream()).asSequence()
                .mapIndexed { index, rawItem ->
                    ProcessedHL7Item(rawItem, index)
                }.toList()

        return maybeParallelize(itemStream.size, itemStream.stream()).map { item ->
            parseHL7Item(item)
        }.map { item ->
            validateAndConvertHL7Item(item, validator)
        }.collect(Collectors.toList())
    }

    private fun parseHL7Item(item: ProcessedHL7Item) = try {
        // TODO validate that this works with ELIMS messages
        val (
            message,
            parseConfiguration,
        ) = parseHL7Message(item.rawItem)
        item.updateParsed(message).setParseConfiguration(parseConfiguration)
    } catch (e: HL7Exception) {
        item.updateParsed(
            InvalidItemActionLogDetail(
                ErrorCode.INVALID_MSG_PARSE,
                item.index,
                "exception while parsing HL7: ${ExceptionUtils.getRootCause(e).message}",
            )
        )
    }

    private fun validateAndConvertHL7Item(
        item: ProcessedHL7Item,
        validator: IMessageValidator,
    ): ProcessedHL7Item = if (item.parsedItem != null) {
        val validationResult = validator.validate(item.parsedItem)
        if (validationResult.isValid()) {
            try {
                val bundle = when (val parseConfiguration = item.parseConfiguration) {
                    null -> HL7toFhirTranslator.getHL7ToFhirTranslatorInstance().translate(item.parsedItem)
                    else ->
                        HL7toFhirTranslator
                            .getHL7ToFhirTranslatorInstance(parseConfiguration.hl7toFHIRMappingLocation)
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

    /**
     * Converts a raw ndjson FHIR string into FHIR bundles
     * @param rawReport the string to convert into FHIR bundles
     * @param validator instance of a validator to use for each parsed FHIR bundle
     *
     * @return the [IProcessedItem] produced from the raw report
     */
    private fun getBundlesFromRawFHIR(
        rawReport: String,
        validator: IMessageValidator,
    ): MutableList<IProcessedItem<Bundle>> {
        val lines = rawReport.lines()
        return maybeParallelize(
            lines.size,
            Streams.mapWithIndex(lines.stream()) { rawItem, index ->
                ProcessedFHIRItem(rawItem, index.toInt())
            }
        ).map { item ->
            parseFHIRItem(item)
        }.map { item ->
            validateFHIRItem(item, validator)
        }.collect(Collectors.toList())
    }

    private fun validateFHIRItem(
        item: ProcessedFHIRItem,
        validator: IMessageValidator,
    ): ProcessedFHIRItem = if (item.parsedItem != null) {
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

    private fun parseFHIRItem(item: ProcessedFHIRItem) = try {
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

    /**
     * Returns a parallelized stream when the number of items being processed is greater
     * than the [sequentialLimit]
     *
     * @param streamSize the number of items in the stream
     * @param stream the stream to optionally parallelize
     *
     */
    private fun <ProcessedItemType : IProcessedItem<*>> maybeParallelize(
        streamSize: Int,
        stream: Stream<ProcessedItemType>,
    ): Stream<ProcessedItemType> =
        if (streamSize > sequentialLimit) {
            withLoggingContext(mapOf("numberOfItems" to streamSize.toString())) {
                logger.info("Generating FHIR bundles in parallel")
            }
            stream.parallel()
        } else {
            withLoggingContext(mapOf("numberOfItems" to streamSize.toString())) {
                logger.info("Generating FHIR bundles in serial")
            }
            stream
        }

    /**
     * Action log detail for tracking an error while processing an HL7 or FHIR item
     *
     * @param errorCode the kind of error that occurred
     * @param errorDetail additional details on the error
     * @param index the index of the item in the report
     */
    class InvalidItemActionLogDetail(
        override val errorCode: ErrorCode,
        val index: Int,
        @JsonProperty
        val errorDetail: String,
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
            withLoggingContext(mapOf("schemaName" to schemaName)) {
                logger.info("Apply a sender transform to the items in the report")
            }
            FhirTransformer(schemaName)
        } else {
            null
        }
    }
}