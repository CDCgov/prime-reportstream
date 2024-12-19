package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Base64
import java.util.UUID

// This is a size limit dictated by our infrastructure in azure
// https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-azure-and-service-bus-queues-compared-contrasted
private const val MESSAGE_SIZE_LIMIT = 64 * 1000

/**
 * Interface representing a message that can be placed on an Azure Queue.
 * This interface supports serialization and deserialization for handling
 * different types of queue messages.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(QueueMessage.ReceiveQueueMessage::class, name = "receive")
)
interface QueueMessage {

    /**
     * Name of the Azure Queue where the message should be placed.
     */
    val messageQueueName: String

    /**
     * Serializes the message object into a Base64 encoded JSON string.
     * Throws an exception if the message exceeds the predefined size limit.
     *
     * @return Base64 encoded string of the message.
     */
    fun serialize(): String {
        val bytes = mapper.writeValueAsBytes(this)
        check(bytes.size < MESSAGE_SIZE_LIMIT) { "Message is too big for the queue." }
        return String(Base64.getEncoder().encode(bytes))
    }

    companion object {
        /**
         * Jackson JSON mapper configured to handle polymorphic types.
         */
        val mapper: JsonMapper = ObjectMapperProvider.mapper

        /**
         * Deserializes a Base64 encoded JSON string into a gov.cdc.prime.reportstream.shared.QueueMessage object.
         *
         * @param s Base64 encoded string representing the message.
         * @return Deserialized gov.cdc.prime.reportstream.shared.QueueMessage object.
         */
        fun deserialize(s: String): QueueMessage = mapper.readValue(s)

        /**
         * Returns a JSON string representation of the gov.cdc.prime.reportstream.shared.QueueMessage object.
         *
         * @return JSON string representation.
         */
        override fun toString(): String = mapper.writeValueAsString(this)

        /**
         * Constant for receive queue on UP
         */
        const val elrSubmissionConvertQueueName = "elr-fhir-convert-submission"

        /**
         * Constant for convert queue on UP
         */
        const val elrConvertQueueName = "elr-fhir-convert"

        /**
         * Constant for destination filter queue on UP
         */
        const val elrDestinationFilterQueueName = "elr-fhir-destination-filter"

        /**
         * Constant for receiver filter queue on UP
         */
        const val elrReceiverFilterQueueName = "elr-fhir-receiver-filter"

        /**
         * Constant for translation queue on UP
         */
        const val elrTranslationQueueName = "elr-fhir-translate"

        /**
         * Constant for receiver enrichment queue on UP.
         */
        const val elrReceiverEnrichmentQueueName = "elr-fhir-receiver-enrichment-queue"

        /**
         * Constant for send queue
         */
        const val elrSendQueueName = "send"
    }

    /**
     * Interface representing information about a report to be processed.
     */
    interface ReportInformation {
        /**
         * URL of the blob storage containing the report.
         */
        val blobURL: String

        /**
         * Digest (hash) of the report for integrity verification.
         */
        val digest: String

        /**
         * Subfolder name in the blob storage where the report is stored.
         */
        val blobSubFolderName: String

        /**
         * Unique identifier of the report.
         */
        val reportId: UUID
    }

    /**
     * Interface representing additional information required for receiving a message.
     */
    interface ReceiveInformation {
        /**
         * Additional headers associated with the message.
         */
        val headers: Map<String, String>
    }

    /**
     * Data class representing a specific type of gov.cdc.prime.reportstream.shared.QueueMessage meant for receiving
     * FHIR (Fast Healthcare Interoperability Resources) data. It implements both
     * ReportInformation and ReceiveInformation interfaces.
     *
     * @property blobURL The URL of the blob storage containing the report.
     * @property digest The digest (hash) of the report.
     * @property blobSubFolderName The subfolder name in the blob storage.
     * @property reportId The unique identifier of the report.
     * @property headers Additional headers associated with the message.
     */
    @JsonTypeName("receive-fhir")
    data class ReceiveQueueMessage(
        override val blobURL: String,
        override val digest: String,
        override val blobSubFolderName: String,
        override val reportId: UUID,
        override val headers: Map<String, String>,
    ) : QueueMessage,
        ReportInformation,
        ReceiveInformation {
        override val messageQueueName = elrSubmissionConvertQueueName
    }

    /**
     * Singleton object responsible for providing and configuring the Jackson ObjectMapper
     * used for serializing and deserializing QueueMessages. The ObjectMapper is configured
     * to support polymorphic types.
     */
    object ObjectMapperProvider {

        /**
         * Polymorphic Type Validator to allow base and subtypes for gov.cdc.prime.reportstream.shared.QueueMessage.
         */
        private val ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(QueueMessage::class.java)
            .allowIfSubType(LinkedHashMap::class.java)
            .build()

        /**
         * Configured Jackson JSON mapper with polymorphic type handling.
         */
        val mapper: JsonMapper = jacksonMapperBuilder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .polymorphicTypeValidator(ptv)
            .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
            .build()

        /**
         * Registers additional subtypes for gov.cdc.prime.reportstream.shared.QueueMessage deserialization.
         *
         * @param subtypes Additional subtypes to be registered.
         */
        fun registerSubtypes(vararg subtypes: Class<out QueueMessage>) {
            mapper.registerSubtypes(*subtypes)
        }

        init {
            // Register common subtypes here. In this case, registering ReceiveQueueMessage.
            mapper.registerSubtypes(ReceiveQueueMessage::class.java)
        }
    }
}