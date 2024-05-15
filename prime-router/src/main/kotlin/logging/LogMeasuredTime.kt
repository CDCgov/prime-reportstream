package gov.cdc.prime.router.logging

import io.github.oshai.kotlinlogging.withLoggingContext
import org.apache.logging.log4j.kotlin.Logging
import kotlin.time.measureTimedValue

object LogMeasuredTime : Logging {
    fun <ReturnedValue> measureAndLogDurationWithReturnedValue(
        logMessage: String,
        loggingContext: Map<String, String> = emptyMap(),
        returnedValueFun: () -> ReturnedValue,
    ): ReturnedValue {
        val (items, timeTaken) = measureTimedValue {
            returnedValueFun()
        }

        val finalLoggingContext = when (items is Collection<*>) {
            true -> mapOf(
                "durationInMilliSecs" to timeTaken.inWholeMilliseconds.toString(),
                "size" to items.size.toString()
            ) + loggingContext

            false -> mapOf(
                "durationInMilliSecs" to timeTaken.inWholeMilliseconds.toString()
            ) + loggingContext
        }

        withLoggingContext(
            finalLoggingContext
        ) {
            logger.info(logMessage)
        }
        return items
    }
}