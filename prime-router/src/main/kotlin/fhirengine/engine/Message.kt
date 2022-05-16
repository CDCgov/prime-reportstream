package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.azure.BlobAccess
import java.util.Base64

// This is a size limit dictated by our infrastructure in azure
// https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-azure-and-service-bus-queues-compared-contrasted
private const val messageSizeLimit = 64 * 1000

/**
 * An interface for Messages to be put on an Azure Queue
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
interface Message {

    fun serialize(): String {
        val bytes = mapper.writeValueAsBytes(this)
        check(bytes.size < messageSizeLimit) { "Message is too big for the queue." }
        val base64Message = String(Base64.getEncoder().encode(bytes))
        return base64Message
    }

    companion object {
        private val ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("gov.cdc.prime.router.engine")
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
 * The Message representation of a raw submission to the system
 *
 * Models the url where the blob is stored, an digest of the blob for validation and the sender name
 */
data class RawSubmission(
    val blobURL: String,
    val digest: String,
    val sender: String,
) : Message {
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