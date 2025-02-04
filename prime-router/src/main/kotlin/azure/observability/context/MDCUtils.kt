package gov.cdc.prime.router.azure.observability.context

import gov.cdc.prime.router.azure.observability.event.AzureCustomEvent
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.slf4j.MDC

/**
 * A few helpful functions for adding complex objects to the MDC
 *
 * Unfortunately, there does not seem to be a way to add a "static" extension
 * method on a Java class
 */
object MDCUtils {
    /**
     * Declare the names of the properties so the same ones are used throughout the codebase
     */
    enum class MDCProperty {
        ACTION_ID,
        ACTION_NAME,
        USERNAME,
        SENDING_ORGANIZATION,
        REPORT_ID,
        TOPIC,
        BLOB_URL,
        OBSERVATION_ID,
    }

    /**
     * Add all fields in a data class as a separate MDC map entry
     *
     * example:
     * ```kotlin
     * data class HelloWorld(val prop1: String, val prop2: Int)
     * MDCUtils.putAll(HelloWorld("abc", 5))
     *
     * // what happens under the hood
     * MDC.put("prop1", "abc")
     * MDC.put("prop2", "5")
     *```
     */
    fun putAll(context: AzureLoggingContext) {
        context.serialize().forEach {
            MDC.put(it.key, it.value)
        }
    }

    /**
     * Add a serializable object to the MDC as a JSON string
     *
     * example:
     * ```kotlin
     * data class HelloWorld(val prop1: String, val prop2: Int)
     * MDCUtils.put("HelloWorld", HelloWorld("abc", 5))
     *
     * // what happens under the hood
     * MDC.put("HelloWorld", "{\"prop1\":\"abc\",\"prop2\":5}")
     * ```
     */
    fun put(key: String, value: Any) {
        val json = JacksonMapperUtilities.jacksonObjectMapper.writeValueAsString(value)
        MDC.put(key, json)
    }
}

/**
 * Add all fields in a data class as a separate MDC map entry for the duration of the lambda
 *
 * example:
 * ```kotlin
 * data class HelloWorld(val prop1: String, val prop2: Int)
 * withLoggingContext(HelloWorld("abc", 5)) {
 *   logger.info("Hello world!") // MDC == "prop1" -> "abc", "prop2" -> "5"
 * }
 * // MDC is empty
 * ```
 */
inline fun <T> withLoggingContext(context: AzureLoggingContext, body: () -> T): T {
    val serialized = context.serialize()
    return io.github.oshai.kotlinlogging.withLoggingContext(
        map = serialized,
        body = body
    )
}

/**
 * Adds a map of String -> serializable object to the MDC as a JSON string for the duration of the lambda
 *
 * example:
 * ```kotlin
 * data class HelloWorld(val prop1: String, val prop2: Int)
 * withLoggingContext(mapOf("HelloWorld" to HelloWorld("abc", 5))) {
 *   logger.info("Hello world!") // MDC == "HelloWorld -> "{\"prop1\":\"abc\",\"prop2\":5}"
 * }
 * // MDC is empty
 * ```
 */
inline fun <T> withLoggingContext(contextMap: Map<MDCUtils.MDCProperty, Any>, body: () -> T): T {
    val mapper = JacksonMapperUtilities.jacksonObjectMapper
    val serializedMap = contextMap.entries.associate { it.key.toString() to mapper.writeValueAsString(it.value) }
    return io.github.oshai.kotlinlogging.withLoggingContext(
        map = serializedMap,
        body = body
    )
}

inline fun <T> withLoggingContext(event: AzureCustomEvent, body: () -> T): T {
    val serializedMap = event.serialize()
    return io.github.oshai.kotlinlogging.withLoggingContext(
        map = serializedMap,
        body = body
    )
}

/**
 * vararg convenience function for `withLoggingContext(contextMap, body)`
 */
inline fun <T> withLoggingContext(
    vararg pairs: Pair<MDCUtils.MDCProperty, Any>,
    body: () -> T,
): T = withLoggingContext(pairs.toMap(), body)