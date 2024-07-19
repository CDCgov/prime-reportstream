package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.fhirengine.engine.elrConvertQueueName
import gov.cdc.prime.router.fhirengine.engine.elrDestinationFilterQueueName
import gov.cdc.prime.router.fhirengine.engine.elrReceiverFilterQueueName
import gov.cdc.prime.router.fhirengine.engine.elrRoutingQueueName
import gov.cdc.prime.router.fhirengine.engine.elrSendQueueName
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.fhirengine.azure.FHIRFunctions
import java.util.Base64
import java.util.UUID

// This is a size limit dictated by our infrastructure in azure
// https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-azure-and-service-bus-queues-compared-contrasted
private const val MESSAGE_SIZE_LIMIT = 64 * 1000

/**
 * An interface for Messages to be put on an Azure Queue
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(FhirConvertQueueMessage::class, name = "convert"),
    JsonSubTypes.Type(FhirRouteQueueMessage::class, name = "route"),
    JsonSubTypes.Type(FhirDestinationFilterQueueMessage::class, name = "destination-filter"),
    JsonSubTypes.Type(FhirReceiverFilterQueueMessage::class, name = "receiver-filter"),
    JsonSubTypes.Type(FhirTranslateQueueMessage::class, name = "translate"),
    JsonSubTypes.Type(BatchEventQueueMessage::class, name = "batch"),
    JsonSubTypes.Type(ProcessEventQueueMessage::class, name = "process"),
    JsonSubTypes.Type(ReportEventQueueMessage::class, name = "report")
)
abstract class QueueMessage {

    //abstract fun getClass(): Class<in QueueMessage>

    fun send(queueAccess: QueueAccess): Unit {

        when (this.javaClass) {
            FhirConvertQueueMessage::class.java -> {
                queueAccess.sendMessage(
                    elrConvertQueueName,
                    serialize()
                )
            }
            FhirRouteQueueMessage::class.java -> {
                queueAccess.sendMessage(
                    elrRoutingQueueName,
                    serialize()
                )
            }
            FhirDestinationFilterQueueMessage::class.java -> {
                queueAccess.sendMessage(
                    elrDestinationFilterQueueName,
                    serialize()
                )
            }
            FhirReceiverFilterQueueMessage::class.java -> {
                queueAccess.sendMessage(
                    elrReceiverFilterQueueName,
                    serialize()
                )
            }
            FhirTranslateQueueMessage::class.java -> {
                queueAccess.sendMessage(
                    elrTranslationQueueName,
                    serialize()
                )
            }
        }

    }

    fun serialize(): String {
        val bytes = mapper.writeValueAsBytes(this)
        check(bytes.size < MESSAGE_SIZE_LIMIT) { "Message is too big for the queue." }
        return String(Base64.getEncoder().encode(bytes))
    }

    companion object {
        private val ptv = BasicPolymorphicTypeValidator.builder()
            .build()
        val mapper: JsonMapper = jacksonMapperBuilder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .polymorphicTypeValidator(ptv)
            .activateDefaultTyping(ptv)
            .build()

        fun deserialize(s: String): QueueMessage {
            return mapper.readValue(s)
        }
    }

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }
}

interface WithDownloadableReport {
    val blobURL: String
    val digest: String

    /**
     * Download the file associated with a RawSubmission message
     */
    fun downloadContent(): String {
        val blobContent = BlobAccess.downloadBlobAsByteArray(this.blobURL)
        val localDigest = BlobAccess.digestToString(BlobAccess.sha256Digest(blobContent))
        check(this.digest == localDigest) {
            "FHIR - Downloaded file does not match expected file\n${this.digest} | $localDigest"
        }
        return String(blobContent)
    }
}

interface ReportIdentifyingInformation {
    val blobSubFolderName: String
    val reportId: ReportId
    val topic: Topic
}

abstract class ReportPipelineMessage :
    ReportIdentifyingInformation,
    WithDownloadableReport,
    QueueMessage()

@JsonTypeName("convert")
data class FhirConvertQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val schemaName: String = "",
) : ReportPipelineMessage()

@JsonTypeName("route")
data class FhirRouteQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
) : ReportPipelineMessage()

@JsonTypeName("destination-filter")
data class FhirDestinationFilterQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
) : ReportPipelineMessage()

@JsonTypeName("receiver-filter")
data class FhirReceiverFilterQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val receiverFullName: String,
) : ReportPipelineMessage()

@JsonTypeName("translate")
data class FhirTranslateQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val receiverFullName: String,
) : ReportPipelineMessage()

abstract class WithEventAction : QueueMessage() {
    abstract val eventAction: Event.EventAction
}

@JsonTypeName("batch")
data class BatchEventQueueMessage(
    override val eventAction: Event.EventAction,
    val receiverName: String,
    val emptyBatch: Boolean,
    val at: String,
) : WithEventAction()

@JsonTypeName("report")
data class ReportEventQueueMessage(
    override val eventAction: Event.EventAction,
    val emptyBatch: Boolean,
    val reportId: UUID,
    val at: String,
) : WithEventAction()

@JsonTypeName("process")
data class ProcessEventQueueMessage(
    override val eventAction: Event.EventAction,
    val reportId: UUID,
    val options: Options,
    val defaults: Map<String, String>,
    val routeTo: List<String>,
    val at: String,
) : WithEventAction()