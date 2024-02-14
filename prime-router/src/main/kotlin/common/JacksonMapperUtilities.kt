package gov.cdc.prime.router.common

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.OrganizationSerializer
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.azure.ReceiverSerializer
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * Jackson Mapper Utilities.
 */
object JacksonMapperUtilities {
    /**
     * ISO 8601 Date/time formatter for serializing date times for API responses.
     * Example output using the default UTC timezone: 2022-02-22T10:31:57.000Z
     */
    val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")

    /**
     * Custom serializer module.
     */
    private val customSerializerModule: SimpleModule = SimpleModule().run {
        /**
         * OffsetDateTime serializer that does not include fractions of seconds.
         */
        class NoOptionalFieldsOffsetDateTimeSerializer : OffsetDateTimeSerializer(
            INSTANCE, false, false,
            timestampFormatter
        )

        // Serialize (object->JSON) date/times with no second fraction.
        addSerializer(OffsetDateTime::class.java, NoOptionalFieldsOffsetDateTimeSerializer())
    }

    /**
     * Mapper that does not fail on unknown properties and that converts dates to an ISO string.
     */
    val allowUnknownsMapper: ObjectMapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
        addModule(customSerializerModule)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        enable(SerializationFeature.INDENT_OUTPUT) // Pretty print JSON output
    }.run { setTimeZone(TimeZone.getTimeZone(Environment.rsTimeZone)) }

    private val customSettingsSerializerModule: SimpleModule = SimpleModule().run {
        addSerializer(OrganizationSerializer(OrganizationAPI::class.java))
        addSerializer(ReceiverSerializer(ReceiverAPI::class.java))
    }

    val customSerializersMapper: ObjectMapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
        addModule(customSerializerModule)
        addModule(customSettingsSerializerModule)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        enable(SerializationFeature.INDENT_OUTPUT) // Pretty print JSON output
    }.run { setTimeZone(TimeZone.getTimeZone(Environment.rsTimeZone)) }

    /**
     * Mapper using library defaults (no kotlin, java modules).
     */
    val objectMapper: ObjectMapper = ObjectMapper()

    /**
     * jacksonObjectMapper using library defaults.
     */
    val jacksonObjectMapper: ObjectMapper = jacksonObjectMapper()

    /**
     * Mapper using library defaults.
     */
    val defaultMapper: ObjectMapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
        addModule(customSerializerModule)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        enable(SerializationFeature.INDENT_OUTPUT) // Pretty print JSON output
    }.run { setTimeZone(TimeZone.getTimeZone(Environment.rsTimeZone)) }
}