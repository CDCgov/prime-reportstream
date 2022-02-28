package gov.cdc.prime.router.common

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * Jackson Mapper Utilities.
 */
object JacksonMapperUtilities {
    /**
     * Mapper that does not fail on unknown properties and that converts dates to an ISO string.
     */
    val datesAsTextMapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * Mapper using library defaults.
     */
    val defaultMapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
    }
}