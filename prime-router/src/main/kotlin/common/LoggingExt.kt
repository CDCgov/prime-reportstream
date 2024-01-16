package gov.cdc.prime.router.common

import org.apache.logging.log4j.ThreadContext
import org.apache.logging.log4j.kotlin.KotlinLogger

/**
 * Collection of logger extension functions that accept a context map along with the message
 *
 * The context map fields will show up in the "customDimensions" collection in azure in the "traces"
 * or "exceptions" table (depending on if an exception was passed)
 */
fun KotlinLogger.trace(msg: Any, context: Map<String, String>) = withContext(context) {
    this.trace(msg)
}

fun KotlinLogger.debug(msg: Any, context: Map<String, String>) = withContext(context) {
    this.debug(msg)
}

fun KotlinLogger.info(msg: Any, context: Map<String, String>) = withContext(context) {
    this.info(msg)
}

fun KotlinLogger.warn(msg: Any, context: Map<String, String>) = this.warn(msg, null, context)
fun KotlinLogger.warn(msg: Any, t: Throwable?, context: Map<String, String>) = withContext(context) {
    this.warn(msg, t)
}

fun KotlinLogger.error(msg: Any, context: Map<String, String>) = this.error(msg, null, context)
fun KotlinLogger.error(msg: Any, t: Throwable?, context: Map<String, String>) = withContext(context) {
    this.error(msg, t)
}

fun KotlinLogger.fatal(msg: Any, t: Throwable?, context: Map<String, String>) = withContext(context) {
    this.fatal(msg, t)
}

/**
 * Logger function wrapper to push the context map to MDC and clear it after running the log function
 */
private fun withContext(context: Map<String, String>, logFn: () -> Unit) {
    ThreadContext.putAll(context)
    logFn()
    ThreadContext.clearAll()
}