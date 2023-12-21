package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import java.util.Base64

// This is a size limit dictated by our infrastructure in azure
// https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-azure-and-service-bus-queues-compared-contrasted
private const val MESSAGE_SIZE_LIMIT = 64 * 1000

/**
 * An interface for Messages to be put on an Azure Queue
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(FhirConvertMessage::class, name = "convert"),
    JsonSubTypes.Type(FhirRouteMessage::class, name = "route"),
    JsonSubTypes.Type(FhirTranslateMessage::class, name = "translate")
)
abstract class Message {

    abstract val reportId: ReportId
    abstract val blobURL: String
    abstract val digest: String
    abstract val blobSubFolderName: String
    abstract val topic: Topic

    /**
     * Download the file associated with a Fhir Convert/Translate/Route message
     */
    fun downloadContent(): String {
        val blobContent = BlobAccess.downloadBlobAsByteArray(this.blobURL)
        val localDigest = BlobAccess.digestToString(BlobAccess.sha256Digest(blobContent))
        check(this.digest == localDigest) {
            "FHIR - Downloaded file does not match expected file\n${this.digest} | $localDigest"
        }
        return String(blobContent)
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

        fun deserialize(s: String): Message {
            return mapper.readValue(s)
        }
    }

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }
}

@JsonTypeName("convert")
data class FhirConvertMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val schemaName: String = "",
) : Message()

@JsonTypeName("route")
data class FhirRouteMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
) : Message()

@JsonTypeName("translate")
data class FhirTranslateMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val topic: Topic,
    val receiverFullName: String,
) : Message()