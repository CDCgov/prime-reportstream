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
        PROCESS_WARNING, // when an attempt at a process action fails, but will be retried
        PROCESS_ERROR, // when an attempt at a process action fails permanently
        RECEIVE,
        CONVERT, // for universal pipeline converting to FHIR
        ROUTE, // calculate routing for a submission
        TRANSLATE,
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
                PROCESS_WARNING -> TaskAction.process_warning
                PROCESS_ERROR -> TaskAction.process_error
                RECEIVE -> TaskAction.receive
                CONVERT -> TaskAction.convert
                ROUTE -> TaskAction.route
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
            // validate incoming queue message is in the expected format. This will error out with an
            //  IllegalStateException and message if it is not valid
            val parts = parseAndValidateEvent(event)

            val action = EventAction.parseQueueMessage(parts[1])
            return when (parts[0]) {
                ReportEvent.eventType -> {
                    val after = parts.getOrNull(4)?.let { OffsetDateTime.parse(it) }
                    val reportId = UUID.fromString(parts[2])
                    val isEmpty = parts[3] == "true"
                    ReportEvent(action, reportId, isEmpty, after)
                }
                BatchEvent.eventType -> {
                    val after = parts.getOrNull(4)?.let { OffsetDateTime.parse(it) }
                    val isEmpty = parts[3] == "true"
                    BatchEvent(action, parts[2], isEmpty, after)
                }
                ProcessEvent.eventType -> {
                    // since process event type has multiple optional parameters, they will be either populated
                    //  or a blank string
                    val after = if (parts[6].isNotEmpty()) OffsetDateTime.parse(parts[6]) else null
                    val reportId = UUID.fromString(parts[2])
                    val options = Options.valueOfOrNone(parts[3])

                    // convert incoming serialized routeTo string into List<String>
                    val routeTo = if (parts[5].isNotEmpty())
                        parts[5].split(ROUTE_TO_SEPARATOR)
                    else
                        emptyList()

                    // convert incoming defaults serialized string to Map<String,String>
                    val defaults = if (parts[4].isNotEmpty())
                        parts[4].split(',').associate { pair ->
                            val defaultParts = pair.split(DEFAULT_SEPARATOR)
                            Pair(defaultParts[0], defaultParts[1])
                        }
                    else emptyMap()

                    ProcessEvent(action, reportId, options, defaults, routeTo, after)
                }
                else -> error("Internal Error: invalid event type: $event")
            }
        }

        /**
         * Receives incoming [event] string from the queue, breaks it into segments, verifies the correct number
         * of segments are present based on event type, and returns the list of parts.
         */
        private fun parseAndValidateEvent(event: String): List<String> {
            val parts = event.split(messageDelimiter)
            // verify the action has at least event type, action, and report id
            if (parts.size < 3) error("Internal Error: Queue events require eventType, action, and reportId.")
            when (parts[0]) {
                // Report event requires 'event type', 'action', and 'report id'. 'at' is optional
                ReportEvent.eventType -> {
                    if (parts.size > 5) error("Internal Error: Report events can have no more than 4 parts.")
                }
                // Receiver event requires 'event type', 'action', 'receiver name'. 'at' is optional
                BatchEvent.eventType -> {
                    if (parts.size > 5) error("Internal Error: Batch events can have no more than 5 parts.")
                }
                // Process event requires 'event type', 'action', 'report id', and 'options'.
                //  'route to', 'default' and 'at are optional but must be present (even if a blank string).
                ProcessEvent.eventType -> {
                    if (parts.size != 7) error(
                        "Internal Error: Process event requires 7 parts."
                    )
                }
                else -> error("Internal Error: invalid event type: $event")
            }
            return parts
        }
    }
}

/**
 * An event that goes on and is read off of an azure queue.
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
        val atClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val defaultClause = if (defaultsQueueParam.isEmpty()) "" else defaultsQueueParam
        val routeToClause = if (routeToQueueParam.isEmpty()) "" else routeToQueueParam

        // generate the process queue message
        return "$eventType$messageDelimiter$eventAction$messageDelimiter$reportId$messageDelimiter$options" +
            "$messageDelimiter$defaultClause$messageDelimiter$routeToClause$messageDelimiter$atClause"
    }

    override fun equals(other: Any?): Boolean {
        return other is ProcessEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken
    }

    override fun hashCode(): Int {
        return (7 * eventAction.hashCode()) +
            (31 * reportId.hashCode()) +
            (17 * at.hashCode()) +
            (19 * retryDuration.hashCode())
    }

    companion object {
        const val eventType = "process"
    }
}

class ReportEvent(
    eventAction: EventAction,
    val reportId: UUID,
    val isEmptyBatch: Boolean,
    at: OffsetDateTime? = null,
    val retryToken: RetryToken? = null,
) : Event(eventAction, at) {

    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else "$messageDelimiter${DateTimeFormatter.ISO_DATE_TIME.format(at)}"
        return "$eventType$messageDelimiter$eventAction$messageDelimiter$reportId" +
            "$messageDelimiter$isEmptyBatch$afterClause"
    }

    override fun equals(other: Any?): Boolean {
        return other is ReportEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken
    }

    override fun hashCode(): Int {
        return (7 * eventAction.hashCode()) +
            (31 * reportId.hashCode()) +
            (17 * at.hashCode()) +
            (19 * retryDuration.hashCode())
    }

    companion object {
        const val eventType = "report"
    }
}

/**
 * Queue message for Batch
 */
class BatchEvent(
    eventAction: EventAction,
    val receiverName: String,
    val isEmptyBatch: Boolean,
    at: OffsetDateTime? = null,
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else "$messageDelimiter${DateTimeFormatter.ISO_DATE_TIME.format(at)}"
        return "$eventType$messageDelimiter$eventAction$messageDelimiter$receiverName" +
            "$messageDelimiter$isEmptyBatch$afterClause"
    }

    override fun equals(other: Any?): Boolean {
        return other is BatchEvent &&
            eventAction == other.eventAction &&
            receiverName == other.receiverName &&
            at == other.at
    }

    override fun hashCode(): Int {
        return (7 * eventAction.hashCode()) +
            (19 * receiverName.hashCode()) +
            (17 * at.hashCode())
    }

    // this should say 'batch' but will break production on deploy if there is anything in the batch queue
    //  when it goes to prod. This value is used only to queue and dequeue message types
    //  (toQueueMessage, parseQueueMessage)
    companion object {
        const val eventType = "receiver"
    }
}