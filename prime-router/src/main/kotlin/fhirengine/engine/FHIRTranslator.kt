package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils.defaultHl7EncodingFiveChars
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils.defaultHl7EncodingFourChars
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
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
) : FHIREngine(metadata, settings, db, blob, azureEventService) {

    /**
     * Accepts a FHIR [message], parses it, and generates translated output files for each item in the destinations
     *  element.
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        message as ReportPipelineMessage

        logger.trace("Translating FHIR file for receivers.")
        // pull fhir document and parse FHIR document
        val bundle = FhirTranscoder.decode(message.downloadContent())

        // track input report
        actionHistory.trackExistingInputReport(message.reportId)

        when (message) {
            is FhirTranslateQueueMessage -> {
                val receiver = settings.findReceiver(message.receiverFullName)
                    ?: throw RuntimeException("Receiver with name ${message.receiverFullName} was not found")
                actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)

                var nextAction = Event.EventAction.BATCH
                var externalName: String? = null
                var queueMessage: ReportEventQueueMessage? = null
                val bodyBytes = if (message.topic.isSendOriginal) {
                    nextAction = Event.EventAction.SEND
                    val originalReport = getOriginalReport(message.reportId)
                    externalName = originalReport.externalName
                    queueMessage = ReportEventQueueMessage(nextAction, false, message.reportId, "")
                    BlobAccess.downloadBlobAsByteArray(originalReport.bodyUrl)
                } else {
                    getByteArrayFromBundle(receiver, bundle)
                }

                // get a Report from the message
                val (report, event, blobInfo) = Report.generateReportAndUploadBlob(
                    nextAction,
                    bodyBytes,
                    listOf(message.reportId),
                    receiver,
                    this.metadata,
                    actionHistory,
                    topic = message.topic,
                    externalName
                )

                if (queueMessage != null) {
                    queueMessage = queueMessage.copy(reportId = report.id)
                }

                return listOf(
                    FHIREngineRunResult(
                        event,
                        report,
                        blobInfo.blobUrl,
                        queueMessage
                    )
                )
            }
            else -> {
                throw RuntimeException(
                    "Message was not a FhirTranslateQueueMessage and cannot be processed by FHIRTranslator: $message"
                )
            }
        }
    }

    /**
     * Takes a [reportId] and returns the content of the first ancestor as submitted by the sender as a ByteArray
     */
    internal fun getOriginalReport(
        reportId: ReportId,
    ): ReportFile {
        val rootReportId = findRootReportId(reportId)
        return db.fetchReportFile(rootReportId)
    }

    /**
     * Takes a [reportId] and returns the ReportId of the original message that was sent
     */
    fun findRootReportId(reportId: ReportId): ReportId {
        val itemLineages = db.fetchItemLineagesForReport(reportId, 1)
        return if (itemLineages != null && itemLineages[0].parentReportId != null) {
            findRootReportId(itemLineages[0].parentReportId)
        } else {
            return reportId
        }
    }

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.TRANSLATED_AT
    override val engineType: String = "Translate"

    /**
     * Returns a byteArray representation of the [bundle] in a format [receiver] expects, or throws an exception if the
     * expected format isn't supported.
     */
    internal fun getByteArrayFromBundle(
        receiver: Receiver,
        bundle: Bundle,
    ): ByteArray {
        if (receiver.enrichmentSchemaNames.isNotEmpty()) {
            receiver.enrichmentSchemaNames.forEach { enrichmentSchemaName ->
                logger.info("Applying enrichment schema $enrichmentSchemaName")
                val transformer = FhirTransformer(
                    convertRelativeSchemaPathToUri(enrichmentSchemaName),
                    ""
                )
                transformer.transform(bundle)
            }
        }
        when (receiver.format) {
            Report.Format.FHIR -> {
                if (receiver.schemaName.isNotEmpty()) {
                    val transformer = FhirTransformer(
                        convertRelativeSchemaPathToUri(receiver.schemaName),
                        ""
                    )
                    transformer.transform(bundle)
                }
                return FhirTranscoder.encode(bundle, FhirContext.forR4().newJsonParser()).toByteArray()
            }

            Report.Format.HL7, Report.Format.HL7_BATCH -> {
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
        // TODO: #10510
        val converter = FhirToHl7Converter(
            convertRelativeSchemaPathToUri(receiver.schemaName),
            "",
            context = FhirToHl7Context(CustomFhirPathFunctions(), config, CustomTranslationFunctions())
        )
        val hl7Message = converter.convert(bundle)

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