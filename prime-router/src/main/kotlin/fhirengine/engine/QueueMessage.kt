package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
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
    JsonSubTypes.Type(RawSubmission::class, name = "raw"),
    JsonSubTypes.Type(FhirConvertQueueMessage::class, name = "convert"),
    JsonSubTypes.Type(FhirRouteQueueMessage::class, name = "route"),
    JsonSubTypes.Type(FhirTranslateQueueMessage::class, name = "translate"),
    JsonSubTypes.Type(FhirTranslateQueueMessage::class, name = "batch"),
    JsonSubTypes.Type(FhirTranslateQueueMessage::class, name = "process"),
    JsonSubTypes.Type(FhirTranslateQueueMessage::class, name = "report")
)
abstract class QueueMessage {
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

abstract class UniversalPipelineQueueMessage : QueueMessage() {
    abstract val reportId: ReportId
    abstract val blobURL: String
    abstract val digest: String
    abstract val blobSubFolderName: String
    abstract val topic: Topic

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

/**
 * The Message representation of a raw submission to the system, tracking the [reportId], [blobURL],
 * [blobSubFolderName] (which is derived from the sender name), and [schemaName] from the sender settings.
 * A [digest] is also provided for checksum verification.
 */
@JsonTypeName("raw")
data class RawSubmission(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val schemaName: String = "",
) : UniversalPipelineQueueMessage()

@JsonTypeName("convert")
data class FhirConvertQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val schemaName: String = "",
) : UniversalPipelineQueueMessage()

@JsonTypeName("route")
data class FhirRouteQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
) : UniversalPipelineQueueMessage()

@JsonTypeName("translate")
data class FhirTranslateQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val receiverFullName: String,
) : UniversalPipelineQueueMessage()

abstract class MixedPipelineQueueMessage : QueueMessage() {
    abstract val eventAction: Event.EventAction
}

abstract class CovidPipelineQueueMessage : QueueMessage() {
    abstract val eventAction: Event.EventAction
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class BatchEventQueueMessage(
    @JsonProperty("eventAction") override val eventAction: Event.EventAction,
    @JsonProperty("receiverName") val receiverName: String,
    @JsonProperty("emptyBatch") val emptyBatch: Boolean,
    @JsonProperty("at") val at: String,
) : MixedPipelineQueueMessage()

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ReportEventQueueMessage(
    @JsonProperty("eventAction") override val eventAction: Event.EventAction,
    @JsonProperty("emptyBatch") val emptyBatch: Boolean,
    @JsonProperty("reportId") val reportId: UUID,
    @JsonProperty("at") val at: String,
) : CovidPipelineQueueMessage()

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ProcessEventQueueMessage(
    @JsonProperty("eventAction") override val eventAction: Event.EventAction,
    @JsonProperty("reportId") val reportId: UUID,
    @JsonProperty("options") val options: Options,
    @JsonProperty("defaults") val defaults: Map<String, String>,
    @JsonProperty("routeTo") val routeTo: List<String>,
    @JsonProperty("at") val at: String,
) : CovidPipelineQueueMessage()