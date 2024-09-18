package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.reportstream.shared.EventAction
import gov.cdc.prime.reportstream.shared.ReportOptions
import gov.cdc.prime.reportstream.shared.queue_message.BatchEventQueueMessage
import gov.cdc.prime.reportstream.shared.queue_message.ProcessEventQueueMessage
import gov.cdc.prime.reportstream.shared.queue_message.QueueMessage
import gov.cdc.prime.reportstream.shared.queue_message.ReportEventQueueMessage
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.transport.RetryToken
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * An event represents a function call, either one that has just happened or one that will happen in the future.
 * Events are sent to queues as messages or stored in a DB as columns of the Task table.
 */
const val messageDelimiter = "&"

abstract class Event(val eventAction: EventAction, val at: OffsetDateTime?) {
    abstract fun toQueueMessage(): String

    fun toTaskAction(): TaskAction {
        return when (eventAction) {
            EventAction.PROCESS -> TaskAction.process
            EventAction.PROCESS_WARNING -> TaskAction.process_warning
            EventAction.PROCESS_ERROR -> TaskAction.process_error
            EventAction.DESTINATION_FILTER -> TaskAction.destination_filter
            EventAction.RECEIVER_FILTER -> TaskAction.receiver_filter
            EventAction.RECEIVE -> TaskAction.receive
            EventAction.CONVERT -> TaskAction.convert
            EventAction.ROUTE -> TaskAction.route
            EventAction.TRANSLATE -> TaskAction.translate
            EventAction.BATCH -> TaskAction.batch
            EventAction.SEND -> TaskAction.send
            EventAction.WIPE -> TaskAction.wipe
            EventAction.NONE -> TaskAction.none
            EventAction.BATCH_ERROR -> TaskAction.batch_error
            EventAction.SEND_ERROR -> TaskAction.send_error
            EventAction.WIPE_ERROR -> TaskAction.wipe_error
            EventAction.RESEND -> TaskAction.resend
            EventAction.REBATCH -> TaskAction.rebatch
            // OTHER is not an expected value, more of a logical fallback/default used in BlobAccess.uploadBody
            EventAction.OTHER -> TaskAction.other
            else -> { TaskAction.other }
        }
    }

    companion object {
        fun parsePrimeRouterQueueMessage(event: String): Event {
            return when (val message = JacksonMapperUtilities.defaultMapper.readValue<QueueMessage>(event)) {
                is ReportEventQueueMessage -> {
                    val at = if (message.at.isNotEmpty()) {
                        OffsetDateTime.parse(message.at)
                    } else {
                        null
                    }
                    ReportEvent(
                        message.eventAction,
                        message.reportId,
                        message.emptyBatch,
                        at
                    )
                }
                is BatchEventQueueMessage -> {
                    val at = if (message.at.isNotEmpty()) {
                        OffsetDateTime.parse(message.at)
                    } else {
                        null
                    }
                    BatchEvent(
                        message.eventAction,
                        message.receiverName,
                        message.emptyBatch,
                        at
                    )
                }
                is ProcessEventQueueMessage -> {
                    val at = if (message.at.isNotEmpty()) {
                        OffsetDateTime.parse(message.at)
                    } else {
                        null
                    }
                    ProcessEvent(
                        message.eventAction,
                        message.reportId,
                        message.options,
                        message.defaults,
                        message.routeTo,
                        at
                    )
                }
                else -> error("Internal Error: invalid event type: $event")
            }
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
    val options: ReportOptions,
    val defaults: Map<String, String>,
    val routeTo: List<String>,
    at: OffsetDateTime? = null,
    val retryToken: RetryToken? = null,
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val queueMessage = ProcessEventQueueMessage(
            eventAction, reportId, options, defaults, routeTo, afterClause
        )
        return JacksonMapperUtilities.objectMapper.writeValueAsString(queueMessage)
    }

    override fun equals(other: Any?): Boolean {
        return other is ProcessEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken
    }

    override fun hashCode(): Int {
        // vars used in hashCode() must match those in equals()
        return (7 * eventAction.hashCode()) +
            (31 * reportId.hashCode()) +
            (17 * at.hashCode()) +
            (19 * retryToken.hashCode())
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
        val afterClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val queueMessage = ReportEventQueueMessage(
            eventAction, isEmptyBatch, reportId, afterClause
        )
        return JacksonMapperUtilities.objectMapper.writeValueAsString(queueMessage)
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
            (19 * retryToken.hashCode())
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
        val afterClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val queueMessage = BatchEventQueueMessage(
            eventAction, receiverName, isEmptyBatch, afterClause
        )
        return JacksonMapperUtilities.objectMapper.writeValueAsString(queueMessage)
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
    //  (toQueueMessage, parsePrimeRouterQueueMessage)
    companion object {
        const val eventType = "receiver"
    }
}