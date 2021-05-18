package gov.cdc.prime.router.azure

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
            if (parts.size < 3 || parts.size > 4) error("Internal Error: bad event format")
            val action = EventAction.parseQueueMessage(parts[1])
            val after = parts.getOrNull(3)?.let { OffsetDateTime.parse(it) }
            return when (parts[0]) {
                ReportEvent.eventType -> {
                    val reportId = UUID.fromString(parts[2])
                    ReportEvent(action, reportId, after)
                }
                ReceiverEvent.eventType -> {
                    ReceiverEvent(action, parts[2], after)
                }
                else -> error("Internal Error: invalid event type: $event")
            }
        }
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