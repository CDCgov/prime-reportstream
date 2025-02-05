package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigestExtractor
import gov.cdc.prime.router.azure.observability.bundleDigest.FhirPathBundleDigestLabResultExtractorStrategy
import gov.cdc.prime.router.azure.observability.context.MDCUtils
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils.defaultHl7EncodingFiveChars
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils.defaultHl7EncodingFourChars
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.report.ReportService
import org.hl7.fhir.r4.model.Bundle
import org.jooq.Field
import java.time.OffsetDateTime

/**
 * Translate a full-ELR FHIR message into the formats needed by any receivers from the route step
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIRTranslator(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
    reportStreamEventService: IReportStreamEventService,
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService, reportStreamEventService) {
    /**
     * Accepts a [FhirTranslateQueueMessage] [message] and, based on its parameters, sends a report to the next pipeline
     * step containing either the first ancestor's blob or a new blob that has been translated per
     * the receiver's settings, pending the passed topic's (found in [message]) isSendOriginal property
     * [actionHistory] and [actionLogger] ensure all activities are recorded to the database and logged
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        when (message) {
            is FhirTranslateQueueMessage -> {
                val contextMap = mapOf(
                    MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
                    MDCUtils.MDCProperty.REPORT_ID to message.reportId,
                    MDCUtils.MDCProperty.TOPIC to message.topic,
                    MDCUtils.MDCProperty.BLOB_URL to message.blobURL
                )
                withLoggingContext(contextMap) {
                    logger.trace("Starting translate work")
                    actionHistory.trackExistingInputReport(message.reportId)
                    val receiver = settings.findReceiver(message.receiverFullName)
                        ?: throw RuntimeException("Receiver with name ${message.receiverFullName} was not found")
                    actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)
                    return if (message.topic.isSendOriginal) {
                        listOf(sendOriginal(message, receiver, actionHistory))
                    } else {
                        listOf(sendTranslated(message, receiver, actionHistory))
                    }
                }
            }
            else -> {
                // Handle the case where casting failed
                throw RuntimeException(
                    "Message was not a FhirTranslateQueueMessage and cannot be " +
                        "processed by FHIRTranslator: $message"
                )
            }
        }
    }

    /**
     * Get the greatest ancestor of the report and send the blob associated with it to the receiver. This is the
     * "original message pass through" feature, documented here:
     *  prime-router/docs/design/proposals/0024-original-message-passthrough.md
     * [message] defines reportId and topic
     * [receiver] the receiver to send the original message to
     * [actionHistory] ensures all activities are recorded to the database
     */
    internal fun sendOriginal(
        message: FhirTranslateQueueMessage,
        receiver: Receiver,
        actionHistory: ActionHistory,
    ): FHIREngineRunResult {
        logger.trace("Preparing to send original message")
        val originalReport = reportService.getRootReport(message.reportId)
        val bodyAsString =
            BlobAccess.downloadBlob(originalReport.bodyUrl, BlobUtils.digestToString(originalReport.blobDigest))

        // get a Report from the message
        val (report, event, blobInfo) = Report.generateReportAndUploadBlob(
            Event.EventAction.SEND,
            bodyAsString.toByteArray(),
            listOf(message.reportId),
            receiver,
            this.metadata,
            actionHistory,
            topic = message.topic,
            MimeFormat.valueOfFromExt(originalReport.bodyFormat),
        )

        return FHIREngineRunResult(
            event,
            report,
            blobInfo.blobUrl,
            ReportEventQueueMessage(Event.EventAction.SEND, false, report.id, OffsetDateTime.now().toString())
        )
    }

    /**
     * Translate the FHIR bundle associated with the report ID to the format the receiver specified and let the
     *  batch step pick it up.
     * [message] defines reportId, topic, and the FHIR bundle to translate
     * [receiver] the receiver to send the translated message to
     * [actionHistory] ensures all activities are recorded to the database
     */
    internal fun sendTranslated(
        message: FhirTranslateQueueMessage,
        receiver: Receiver,
        actionHistory: ActionHistory,
    ): FHIREngineRunResult {
        logger.trace("Preparing to send translated message")
        val originalReport = reportService.getRootReport(message.reportId)
        val bundle = FhirTranscoder.decode(BlobAccess.downloadBlob(message.blobURL, message.digest))
        val bodyBytes = getByteArrayFromBundle(receiver, bundle)

        val (report, event, blobInfo) = Report.generateReportAndUploadBlob(
            Event.EventAction.BATCH,
            bodyBytes,
            listOf(message.reportId),
            receiver,
            this.metadata,
            actionHistory,
            topic = message.topic
        )

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
            eventName = ReportStreamEventName.ITEM_TRANSFORMED,
            childReport = report,
            pipelineStepName = TaskAction.translate
        ) {
            parentReportId(message.reportId)
            params(
                mapOf(
                    ReportStreamEventProperties.RECEIVER_NAME to receiver.fullName,
                    ReportStreamEventProperties.BUNDLE_DIGEST
                        to bundleDigestExtractor.generateDigest(bundle),
                    ReportStreamEventProperties.ORIGINAL_FORMAT to originalReport.bodyFormat,
                    ReportStreamEventProperties.TARGET_FORMAT to receiver.translation.format.name,
                    ReportStreamEventProperties.ENRICHMENTS to listOf(receiver.translation.schemaName)
                )
            )
            trackingId(bundle)
        }

        return FHIREngineRunResult(
            event,
            report,
            blobInfo.blobUrl,
            null
        )
    }

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.TRANSLATED_AT
    override val engineType: String = "Translate"
    override val taskAction: TaskAction = TaskAction.translate

    /**
     * Returns a byteArray representation of the [bundle] in a format [receiver] expects, or throws an exception if the
     * expected format isn't supported.
     */
    internal fun getByteArrayFromBundle(
        receiver: Receiver,
        bundle: Bundle,
    ): ByteArray {
        when (receiver.format) {
            MimeFormat.FHIR -> {
                if (receiver.schemaName.isNotEmpty()) {
                    val transformer = FhirTransformer(
                        receiver.schemaName,
                    )
                    transformer.process(bundle)
                }
                return FhirTranscoder.encode(bundle, FhirContext.forR4().newJsonParser()).toByteArray()
            }

            MimeFormat.HL7, MimeFormat.HL7_BATCH -> {
                val hl7Message = getHL7MessageFromBundle(bundle, receiver)
                return hl7Message.encodePreserveEncodingChars().toByteArray()
            }

            else -> {
                error("Receiver format ${receiver.format} not supported.")
            }
        }
    }

    /**
     * Turn a fhir [bundle] into a hl7 message formatter for the [receiver] specified.
     * @return HL7 Message in the format required by the receiver
     */
    internal fun getHL7MessageFromBundle(bundle: Bundle, receiver: Receiver): Message {
        val config = (receiver.translation as? Hl7Configuration)?.let {
            HL7TranslationConfig(
                it,
                receiver
            )
        }

        val converter = FhirToHl7Converter(
            receiver.schemaName,
            BlobAccess.BlobContainerMetadata.build("metadata", Environment.get().storageEnvVar),
            context = FhirToHl7Context(CustomFhirPathFunctions(), config, CustomTranslationFunctions())
        )
        val hl7Message = converter.process(bundle)

        // if receiver is 'testing' or useTestProcessingMode is true, set to 'T', otherwise leave it as is
        if (receiver.customerStatus == CustomerStatus.TESTING ||
            (
                (receiver.translation is Hl7Configuration) &&
                    receiver.translation.useTestProcessingMode
                )
        ) {
            Terser(hl7Message).set("MSH-11-1", "T")
        }

        return hl7Message
    }
}

/**
 * Encodes a message while avoiding an error when MSH-2 is five characters long
 *
 * @return the encoded message as a string
 */
fun Message.encodePreserveEncodingChars(): String {
    // get encoding characters ...
    val msh = this.get("MSH") as Segment
    val encCharString = Terser.get(msh, 2, 0, 1, 1)
    val hasFiveEncodingChars = encCharString == defaultHl7EncodingFiveChars
    if (hasFiveEncodingChars) Terser.set(msh, 2, 0, 1, 1, defaultHl7EncodingFourChars)
    var encodedMsg = encode()
    if (hasFiveEncodingChars) {
        encodedMsg = encodedMsg.replace(defaultHl7EncodingFourChars, defaultHl7EncodingFiveChars)
        // Set MSH-2 back in the in-memory message to preserve original value
        Terser.set(msh, 2, 0, 1, 1, defaultHl7EncodingFiveChars)
    }
    return encodedMsg
}