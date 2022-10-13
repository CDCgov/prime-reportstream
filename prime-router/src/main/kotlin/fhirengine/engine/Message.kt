package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.BlobAccess
import java.util.Base64

// This is a size limit dictated by our infrastructure in azure
// https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-azure-and-service-bus-queues-compared-contrasted
private const val messageSizeLimit = 64 * 1000

/**
 * An interface for Messages to be put on an Azure Queue
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(RawSubmission::class, name = "raw")
)
abstract class Message {

    fun serialize(): String {
        val bytes = mapper.writeValueAsBytes(this)
        check(bytes.size < messageSizeLimit) { "Message is too big for the queue." }
        return String(Base64.getEncoder().encode(bytes))
    }

    companion object {
        private val ptv = BasicPolymorphicTypeValidator.builder()
            .build()
        val mapper = jacksonMapperBuilder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .polymorphicTypeValidator(ptv)
            .activateDefaultTyping(ptv)
            .build()

        fun deserialize(s: String): Message {
            return mapper.readValue(s)
        }
    }
}

/**
 * The Message representation of a raw submission to the system, tracking the [reportId], [blobUrl],
 * and [sender]. A [digest] is also provided for checksum verification.
 *
 * TODO: Need to determine if options, defaults and routeTo need to be supported
 */
@JsonTypeName("raw")
data class RawSubmission(
    val reportId: ReportId,
    val blobURL: String,
    val digest: String,
    val blobSubFolderName: String,
//    val options: Options,
//    val defaults: Map<String, String>,
//    val routeTo: List<String>,
) : Message() {
    /**
     * Download the file associated with a RawSubmission message
     */
    fun downloadContent(): String {
        val blobContent = BlobAccess.downloadBlob(this.blobURL)
        val localDigest = BlobAccess.digestToString(BlobAccess.sha256Digest(blobContent))
        check(this.digest == localDigest) {
            "FHIR - Downloaded file does not match expected file\n${this.digest} | $localDigest"
        }
        return String(blobContent)
    }
}