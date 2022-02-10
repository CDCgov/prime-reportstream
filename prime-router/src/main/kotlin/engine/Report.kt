package gov.cdc.prime.router.engine

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Base64

private const val messageSizeLimit = 64 * 1000

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
interface Message {

    fun serialize(): String {
        val bytes = Message.mapper.writeValueAsBytes(this)
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

data class RawSubmission(
    val blobURL: String,
    val digest: String,
    val sender: String,
) : Message