import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DeserializationFeature
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
 * An interface for Messages to be put on an Azure Queue
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(QueueMessage.ConvertQueueMessage::class, name = "convert"),
)
interface QueueMessage {
    fun serialize(): String {
        val bytes = mapper.writeValueAsBytes(this)
        check(bytes.size < MESSAGE_SIZE_LIMIT) { "Message is too big for the queue." }
        return String(Base64.getEncoder().encode(bytes))
    }

    companion object {
        val mapper: JsonMapper = ObjectMapperProvider.mapper

        fun deserialize(s: String): QueueMessage {
            return mapper.readValue(s)
        }

        override fun toString(): String {
            return mapper.writeValueAsString(this)
        }
    }

    interface ReportInformation {
        val blobURL: String
        val digest: String
        val blobSubFolderName: String
        val reportId: UUID
    }

    interface ConvertInformation {
        val headers: Map<String, String>
    }

    @JsonTypeName("convert")
    data class ConvertQueueMessage(
        override val blobURL: String,
        override val digest: String,
        override val blobSubFolderName: String,
        override val reportId: UUID,
        override val headers: Map<String, String>,
    ) : QueueMessage, ReportInformation, ConvertInformation

    object ObjectMapperProvider {

        private val ptv = BasicPolymorphicTypeValidator.builder()
            .build()
        val mapper: JsonMapper = jacksonMapperBuilder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .polymorphicTypeValidator(ptv)
            .activateDefaultTyping(ptv)
            .build()

        fun registerSubtypes(vararg subtypes: Class<out QueueMessage>) {
            mapper.registerSubtypes(*subtypes)
        }

        init {
            // Register common subtypes here if necessary
            mapper.registerSubtypes(ConvertQueueMessage::class.java)
        }
    }
}