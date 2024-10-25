package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator.ParseFailureError
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.Streams
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.IProcessedItem
import fhirengine.engine.ProcessedFHIRItem
import fhirengine.engine.ProcessedHL7Item
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ErrorCode
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.UnmappableConditionMessage
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.ConditionStamper
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.LookupTableConditionMapper
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigestExtractor
import gov.cdc.prime.router.azure.observability.bundleDigest.FhirPathBundleDigestLabResultExtractorStrategy
import gov.cdc.prime.router.azure.observability.context.MDCUtils
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.fhirengine.utils.HL7Reader.Companion.parseHL7Message
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.logging.LogMeasuredTime
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.validation.IItemValidator
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
    reportService: ReportService = ReportService(),
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService) {

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.PROCESSED_AT

    override val engineType: String = "Convert"

    override val taskAction: TaskAction = TaskAction.convert

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
    ): List<FHIREngineRunResult> = when (message) {
        is FhirConvertQueueMessage -> {
            fhirEngineRunResults(message, message.schemaName, actionLogger, actionHistory)
        }
        else -> {
            throw RuntimeException(
                "Message was not a FhirConvert and cannot be processed: $message"
            )
        }
    }

    private fun fhirEngineRunResults(
        queueMessage: FhirConvertQueueMessage,
        schemaName: String,
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
            actionLogger.setReportId(queueMessage.reportId)
            actionHistory.trackExistingInputReport(queueMessage.reportId)
            val format = Report.getFormatFromBlobURL(queueMessage.blobURL)
            logger.info("Starting FHIR Convert step")

            // This line is a workaround for a defect in the hapi-fhir library
            // Specifically https://github.com/hapifhir/hapi-fhir/blob/b555498c9b7824af67b219e5b7b85f7992aec991/hapi-fhir-serviceloaders/hapi-fhir-caching-api/src/main/java/ca/uhn/fhir/sl/cache/CacheFactory.java#L32
            // which creates a static instance of ServiceLoader which the documentation indicates is not safe to use in a
            // concurrent setting https://arc.net/l/quote/hauavetq.  See also this closed issue https://github.com/jakartaee/jsonp-api/issues/26#issuecomment-364844610
            // for someone requesting a similar change in another library and the reasoning why it can't be done that way
            //
            // This line exists so that FhirPathUtils (an object) is instantiated before any of the multi-threaded code run
            // (kotlin objects are instantiated at first access https://arc.net/l/quote/tbvpqnlh)
            // TODO: https://github.com/CDCgov/prime-reportstream/issues/14287
            FhirPathUtils

            val processedItems = process(format, queueMessage, actionLogger)

            // processedItems can be empty in three scenarios:
            // - the blob had no contents, i.e. an empty file was submitted
            // - the format is HL7 and the contents were not parseable, so the number of items is unknown
            // - the format is unexpected like CSV
            if (processedItems.isNotEmpty()) {
                return LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                    "Applied sender transform and routed"
                ) {
                    val transformer = getTransformerFromSchema(
                        schemaName
                    )

                    maybeParallelize(
                        processedItems.size,
                        Streams.mapWithIndex(processedItems.stream()) { bundle, index ->
                            Pair(bundle, index)
                        },
                        "Applying sender transforms and routing"
                    ).map { (processedItem, itemIndex) ->
                        // conduct FHIR Transform
                        if (processedItem.bundle == null) {
                            val report = Report(
                                MimeFormat.FHIR,
                                emptyList(),
                                parentItemLineageData = listOf(
                                    Report.ParentItemLineageData(queueMessage.reportId, itemIndex.toInt() + 1)
                                ),
                                metadata = this.metadata,
                                topic = queueMessage.topic,
                                nextAction = TaskAction.none
                            )
                            val noneEvent = ProcessEvent(
                                Event.EventAction.NONE,
                                report.id,
                                Options.None,
                                emptyMap(),
                                emptyList()
                            )
                            actionHistory.trackCreatedReport(noneEvent, report)
                            if (processedItem.validationError != null) {
                                reportEventService.sendItemProcessingError(
                                    ReportStreamEventName.ITEM_FAILED_VALIDATION,
                                    report,
                                    TaskAction.convert,
                                    processedItem.validationError!!.message,
                                ) {
                                    parentReportId(queueMessage.reportId)
                                    parentItemIndex(itemIndex.toInt() + 1)
                                    params(
                                        mapOf(
                                            ReportStreamEventProperties.ITEM_FORMAT to format,
                                            ReportStreamEventProperties.VALIDATION_PROFILE
                                                to queueMessage.topic.validator.validatorProfileName
                                        )
                                    )
                                }
                            }
                            null
                        } else {
                            // We know from the null check above that this cannot be null
                            val bundle = processedItem.bundle!!
                            transformer?.process(bundle)

                            // make a 'report'
                            val report = Report(
                                MimeFormat.FHIR,
                                emptyList(),
                                parentItemLineageData = listOf(
                                    Report.ParentItemLineageData(queueMessage.reportId, itemIndex.toInt() + 1)
                                ),
                                metadata = this.metadata,
                                topic = queueMessage.topic,
                                nextAction = TaskAction.destination_filter
                            )

                            // create route event
                            val routeEvent = ProcessEvent(
                                Event.EventAction.DESTINATION_FILTER,
                                report.id,
                                Options.None,
                                emptyMap(),
                                emptyList()
                            )

                            // upload to blobstore
                            val bodyBytes = FhirTranscoder.encode(bundle).toByteArray()
                            val blobInfo = BlobAccess.uploadBody(
                                MimeFormat.FHIR,
                                bodyBytes,
                                report.id.toString(),
                                queueMessage.blobSubFolderName,
                                routeEvent.eventAction
                            )
                            report.bodyURL = blobInfo.blobUrl

                            // track created report
                            actionHistory.trackCreatedReport(routeEvent, report, blobInfo = blobInfo)

                            val bundleDigestExtractor = BundleDigestExtractor(
                                FhirPathBundleDigestLabResultExtractorStrategy(
                                    CustomContext(
                                        bundle,
                                        bundle,
                                        mutableMapOf(),
                                        CustomFhirPathFunctions()
                                    )
                                )
                            )
                            reportEventService.sendItemEvent(
                                ReportStreamEventName.ITEM_ACCEPTED,
                                report,
                                TaskAction.convert
                            ) {
                                parentReportId(queueMessage.reportId)
                                parentItemIndex(itemIndex.toInt() + 1)
                                trackingId(bundle)
                                params(
                                    mapOf(
                                        ReportStreamEventProperties.BUNDLE_DIGEST
                                            to bundleDigestExtractor.generateDigest(processedItem.bundle!!),
                                        ReportStreamEventProperties.ITEM_FORMAT to format
                                    )
                                )
                            }

                        FHIREngineRunResult(
                                routeEvent,
                                report,
                                blobInfo.blobUrl,
                                FhirDestinationFilterQueueMessage(
                                    report.id,
                                    blobInfo.blobUrl,
                                    BlobUtils.digestToString(blobInfo.digest),
                                    queueMessage.blobSubFolderName,
                                    queueMessage.topic
                                )
                            )
                        }
                    }.collect(Collectors.toList()).filterNotNull()
                }
            } else {
                val report = Report(
                    format,
                    emptyList(),
                    0,
                    metadata = this.metadata,
                    topic = queueMessage.topic,
                    nextAction = TaskAction.none
                )
                actionHistory.trackEmptyReport(report)
                reportEventService.sendReportProcessingError(
                    ReportStreamEventName.REPORT_NOT_PROCESSABLE,
                    report,
                    TaskAction.convert,
                    "Submitted report was either empty or could not be parsed into HL7"
                    ) {
                    parentReportId(queueMessage.reportId)
                    params(
                        mapOf(
                            ReportStreamEventProperties.ITEM_FORMAT to format
                        )
                    )
                }
                return emptyList()
            }
        }
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
     * - [routeReportWithInvalidItems] whether route to items through when the entire report is not valid
     *
     * @param format the format of the items in the report
     * @param queueMessage the message that contains the url for the raw report
     * @param actionLogger keeps track of any errors (parsing, validation, conversion) that occur for individual items
     * @param routeReportWithInvalidItems controls the behavior when some items encounter errors
     *
     * @return the bundles that should get routed
     */
    internal fun process(
        format: MimeFormat,
        queueMessage: FhirConvertQueueMessage,
        actionLogger: ActionLogger,
        routeReportWithInvalidItems: Boolean = true,
    ): List<IProcessedItem<*>> {
        val validator = queueMessage.topic.validator
        val rawReport = BlobAccess.downloadBlob(queueMessage.blobURL, queueMessage.digest)
        return if (rawReport.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
            emptyList()
        } else {
            val processedItems = when (format) {
                MimeFormat.HL7, MimeFormat.HL7_BATCH -> {
                    try {
                        LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                            "Processed raw message into items",
                            mapOf(
                                "format" to format.name
                            )
                        ) {
                            getBundlesFromRawHL7(rawReport, validator, queueMessage.topic.hl7ParseConfiguration)
                        }
                    } catch (ex: ParseFailureError) {
                        actionLogger.error(
                            InvalidReportMessage("Parse error while attempting to iterate over HL7 raw message")
                        )
                        emptyList()
                    }
                }

                MimeFormat.FHIR -> {
                    LogMeasuredTime.measureAndLogDurationWithReturnedValue(
                        "Processed raw message into items",
                        mapOf(
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
            val stamper = ConditionStamper(LookupTableConditionMapper(metadata))
            val bundles = processedItems.map { item ->
                val error = item.getError()
                if (error != null) {
                    actionLogger.getItemLogger(error.index + 1, item.getTrackingId()).error(error)
                }
                // 'stamp' observations with their condition code
                if (item.bundle != null) {
                    item.bundle!!.getObservations().forEach { observation ->
                        val result = stamper.stampObservation(observation)
                        if (!result.success) {
                            val logger = actionLogger.getItemLogger(item.index + 1, observation.id)
                            if (result.failures.isEmpty()) {
                                logger.warn(UnmappableConditionMessage())
                            } else {
                                logger.warn(
                                    result.failures.map {
                                    UnmappableConditionMessage(
                                        it.failures.map { it.code },
                                        it.source
                                    )
                                }
                                )
                            }
                        }
                    }
                }
                item
            }

            withLoggingContext(
                mapOf(
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
        validator: IItemValidator,
        hL7MessageParseAndConvertConfiguration: HL7Reader.Companion.HL7MessageParseAndConvertConfiguration?,
    ): List<IProcessedItem<Message>> {
        val itemStream =
            Hl7InputStreamMessageStringIterator(rawReport.byteInputStream()).asSequence()
                .mapIndexed { index, rawItem ->
                    ProcessedHL7Item(rawItem, index)
                }.toList()

        return maybeParallelize(itemStream.size, itemStream.stream(), "Generating FHIR bundles in").map { item ->
            parseHL7Item(item, hL7MessageParseAndConvertConfiguration)
        }.map { item ->
            validateAndConvertHL7Item(item, validator, hL7MessageParseAndConvertConfiguration)
        }.collect(Collectors.toList())
    }

    private fun parseHL7Item(
        item: ProcessedHL7Item,
        hL7MessageParseAndConvertConfiguration: HL7Reader.Companion.HL7MessageParseAndConvertConfiguration?,
    ) = try {
        val message = parseHL7Message(item.rawItem, hL7MessageParseAndConvertConfiguration)
        item.updateParsed(message)
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
        validator: IItemValidator,
        hL7MessageParseAndConvertConfiguration: HL7Reader.Companion.HL7MessageParseAndConvertConfiguration?,
    ): ProcessedHL7Item = if (item.parsedItem != null) {
        val validationResult = validator.validate(item.parsedItem)
        if (validationResult.isValid()) {
            try {
                val bundle = when (hL7MessageParseAndConvertConfiguration) {
                    null -> HL7toFhirTranslator.getHL7ToFhirTranslatorInstance().translate(item.parsedItem)
                    else ->
                        HL7toFhirTranslator
                            .getHL7ToFhirTranslatorInstance(
                                hL7MessageParseAndConvertConfiguration.hl7toFHIRMappingLocation
                            )
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
                    validationResult.getErrorsMessage(validator)
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
        validator: IItemValidator,
    ): MutableList<IProcessedItem<Bundle>> {
        val lines = rawReport.lines()
        return maybeParallelize(
            lines.size,
            Streams.mapWithIndex(lines.stream()) { rawItem, index ->
                ProcessedFHIRItem(rawItem, index.toInt())
            },
            "Generating FHIR bundles in"
        ).map { item ->
            parseFHIRItem(item)
        }.map { item ->
            validateFHIRItem(item, validator)
        }.collect(Collectors.toList())
    }

    private fun validateFHIRItem(
        item: ProcessedFHIRItem,
        validator: IItemValidator,
    ): ProcessedFHIRItem = if (item.parsedItem != null) {
        val validationResult = validator.validate(item.parsedItem)
        if (validationResult.isValid()) {
            item.setBundle(item.parsedItem)
        } else {
            item.updateValidation(
                InvalidItemActionLogDetail(
                    ErrorCode.INVALID_MSG_VALIDATION,
                    item.index,
                    validationResult.getErrorsMessage(validator)
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
    private fun <ProcessedItemType> maybeParallelize(
        streamSize: Int,
        stream: Stream<ProcessedItemType>,
        message: String,
    ): Stream<ProcessedItemType> =
        if (streamSize > sequentialLimit) {
            withLoggingContext(mapOf("numberOfItems" to streamSize.toString())) {
                logger.info("$message parallel")
            }
            stream.parallel()
        } else {
            withLoggingContext(mapOf("numberOfItems" to streamSize.toString())) {
                logger.info("$message serial")
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

        override val scope: ActionLogScope = ActionLogScope.item

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
    fun getTransformerFromSchema(schemaName: String): FhirTransformer? = if (schemaName.isNotBlank()) {
            withLoggingContext(mapOf("schemaName" to schemaName)) {
                logger.info("Apply a sender transform to the items in the report")
            }
            FhirTransformer(schemaName)
        } else {
            null
        }
}