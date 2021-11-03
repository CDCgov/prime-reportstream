package gov.cdc.prime.router.azure

import gov.cdc.prime.router.DEFAULT_SEPARATOR
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ROUTE_TO_SEPARATOR
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.transport.RetryToken
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * A event represents a function call, either one that has just happened or on that will happen in the future.
 * Events are sent to queues as messages or stored in a DB as columns of the Task table.
 */
const val messageDelimiter = "&"

abstract class Event(val eventAction: EventAction, val at: OffsetDateTime?) {
    abstract fun toQueueMessage(): String

    enum class EventAction {
        PROCESS, // for when a message goes into a queue to be processed
        RECEIVE,
        TRANSLATE, // Deprecated
        BATCH,
        SEND,
        WIPE, // Deprecated
        NONE,
        BATCH_ERROR,
        SEND_ERROR,
        WIPE_ERROR, // Deprecated
        RESEND,
        REBATCH;

        fun toTaskAction(): TaskAction {
            return when (this) {
                PROCESS -> TaskAction.process
                RECEIVE -> TaskAction.receive
                TRANSLATE -> TaskAction.translate
                BATCH -> TaskAction.batch
                SEND -> TaskAction.send
                WIPE -> TaskAction.wipe
                NONE -> TaskAction.none
                BATCH_ERROR -> TaskAction.batch_error
                SEND_ERROR -> TaskAction.send_error
                WIPE_ERROR -> TaskAction.wipe_error
                RESEND -> TaskAction.resend
                REBATCH -> TaskAction.rebatch
            }
        }

        fun toQueueName(): String? {
            return when (this) {
                PROCESS,
                TRANSLATE,
                BATCH,
                SEND,
                WIPE -> this.toString().lowercase()
                else -> null
            }
        }

        companion object {
            fun parseQueueMessage(action: String): EventAction {
                return when (action.lowercase()) {
                    "process" -> PROCESS
                    "receive" -> RECEIVE
                    "translate" -> TRANSLATE
                    "batch" -> BATCH
                    "send" -> SEND
                    "wipe" -> WIPE
                    "none" -> NONE
                    "batch_error" -> BATCH_ERROR
                    "send_error" -> SEND_ERROR
                    "wipe_error" -> WIPE_ERROR
                    else -> error("Internal Error: $action does not match known action names")
                }
            }
        }
    }

    companion object {
        fun parseQueueMessage(event: String): Event {
            val parts = event.split(messageDelimiter)
            if (parts.size < 3 || parts.size > 7) error("Internal Error: bad event format")
            val action = EventAction.parseQueueMessage(parts[1])
            return when (parts[0]) {
                ReportEvent.eventType -> {
                    val after = parts.getOrNull(3)?.let { OffsetDateTime.parse(it) }
                    val reportId = UUID.fromString(parts[2])
                    ReportEvent(action, reportId, after)
                }
                ReceiverEvent.eventType -> {
                    val after = parts.getOrNull(3)?.let { OffsetDateTime.parse(it) }
                    ReceiverEvent(action, parts[2], after)
                }
                ProcessEvent.eventType -> {
                    val after = parts.getOrNull(6)?.let { OffsetDateTime.parse(it) }
                    val reportId = UUID.fromString(parts[2])
                    val options = Options.valueOf(parts[3])

                    // convert incoming serialized routeTo string into List<String>
                    val routeTo = parts.getOrNull(5)?.let {
                        it.split(ROUTE_TO_SEPARATOR)
                    } ?: emptyList()

                    // convert incoming defaults serialized string to Map<String,String>
                    val defaults = parts.getOrNull(4)?.let {
                        it.split(',').associate { pair ->
                            val defaultParts = pair.split(DEFAULT_SEPARATOR)
                            Pair(defaultParts[0], defaultParts[1])
                        }
                    } ?: emptyMap()

                    ProcessEvent(action, reportId, options, defaults, routeTo, after)
                }
                else -> error("Internal Error: invalid event type: $event")
            }
        }
    }
}

/**
 * An event that goes on and is read off of the 'process' queue.
 * @param reportId The report ID to be processed
 * @param options Options passed in on the initial call
 * @param defaults Defaults passed in on the initial call
 * @param routeTo Route to overrides passed in on the initial call
 * @param at Allows future scheduling of the queue message
 * @param retryToken For retrying a process queue message
 * @return Returns the event representing the 'process' queue message
 */
class ProcessEvent(
    eventAction: EventAction,
    val reportId: UUID,
    val options: Options,
    val defaults: Map<String, String>,
    val routeTo: List<String>,
    at: OffsetDateTime? = null,
    val retryToken: RetryToken? = null
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        // turn the defaults and route to into strings that can go on the process queue
        val routeToQueueParam = routeTo.joinToString(",")
        val defaultsQueueParam = defaults.map { pair -> "${pair.key}:${pair.value}" }.joinToString(",")

        // determine if these parts of the queue message are present
        val afterClause = if (at == null) "" else "$messageDelimiter${DateTimeFormatter.ISO_DATE_TIME.format(at)}"
        val defaultClause = if (defaultsQueueParam.isEmpty()) "" else "$messageDelimiter$defaultsQueueParam"
        val routeToClause = if (routeToQueueParam.isEmpty()) "" else "$messageDelimiter$routeToQueueParam"

        // generate the process queue message
        return "$eventType$messageDelimiter$eventAction$messageDelimiter$reportId$messageDelimiter$options" +
            "$defaultClause$routeToClause$afterClause"
    }

    override fun equals(other: Any?): Boolean {
        return other is ReportEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken
    }

    companion object {
        const val eventType = "process"
    }
}

class ReportEvent(
    eventAction: EventAction,
    val reportId: UUID,
    at: OffsetDateTime? = null,
    val retryToken: RetryToken? = null
) : Event(eventAction, at) {

    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else "$messageDelimiter${DateTimeFormatter.ISO_DATE_TIME.format(at)}"
        return "$eventType$messageDelimiter$eventAction$messageDelimiter$reportId$afterClause"
    }

    override fun equals(other: Any?): Boolean {
        return other is ReportEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken
    }

    companion object {
        const val eventType = "report"
    }
}

class ReceiverEvent(
    eventAction: EventAction,
    val receiverName: String,
    at: OffsetDateTime? = null,
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else "$messageDelimiter${DateTimeFormatter.ISO_DATE_TIME.format(at)}"
        return "$eventType$messageDelimiter$eventAction$messageDelimiter$receiverName$afterClause"
    }

    override fun equals(other: Any?): Boolean {
        return other is ReceiverEvent &&
            eventAction == other.eventAction &&
            receiverName == other.receiverName &&
            at == other.at
    }

    companion object {
        const val eventType = "receiver"
    }
}