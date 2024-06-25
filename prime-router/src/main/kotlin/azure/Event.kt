package gov.cdc.prime.router.azure

import azure.IEvent
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.engine.BatchEventQueueMessage
import gov.cdc.prime.router.fhirengine.engine.ProcessEventQueueMessage
import gov.cdc.prime.router.fhirengine.engine.QueueMessage
import gov.cdc.prime.router.fhirengine.engine.ReportEventQueueMessage
import gov.cdc.prime.router.transport.RetryToken
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * An event represents a function call, either one that has just happened or one that will happen in the future.
 * Events are sent to queues as messages or stored in a DB as columns of the Task table.
 */
const val messageDelimiter = "&"

abstract class Event(override val eventAction: IEvent.EventAction, override val at: OffsetDateTime?) : IEvent {
    abstract override fun toQueueMessage(): String

//    enum class EventAction {
//        PROCESS, // for when a message goes into a queue to be processed
//        PROCESS_WARNING, // when an attempt at a process action fails, but will be retried
//        PROCESS_ERROR, // when an attempt at a process action fails permanently
//        DESTINATION_FILTER,
//        RECEIVER_FILTER,
//        RECEIVE,
//        CONVERT, // for universal pipeline converting to FHIR
//        ROUTE, // calculate routing for a submission
//        TRANSLATE,
//        BATCH,
//        SEND,
//        WIPE, // Deprecated
//        NONE,
//        BATCH_ERROR,
//        SEND_ERROR,
//        WIPE_ERROR, // Deprecated
//        RESEND,
//        REBATCH,
//        OTHER, // a default/unknown
//        ;

    fun toTaskAction(): TaskAction =
        when (eventAction) {
            IEvent.EventAction.PROCESS -> TaskAction.process
            IEvent.EventAction.PROCESS_WARNING -> TaskAction.process_warning
            IEvent.EventAction.PROCESS_ERROR -> TaskAction.process_error
            IEvent.EventAction.DESTINATION_FILTER -> TaskAction.destination_filter
            IEvent.EventAction.RECEIVER_FILTER -> TaskAction.receiver_filter
            IEvent.EventAction.RECEIVE -> TaskAction.receive
            IEvent.EventAction.CONVERT -> TaskAction.convert
            IEvent.EventAction.ROUTE -> TaskAction.route
            IEvent.EventAction.TRANSLATE -> TaskAction.translate
            IEvent.EventAction.BATCH -> TaskAction.batch
            IEvent.EventAction.SEND -> TaskAction.send
            IEvent.EventAction.WIPE -> TaskAction.wipe
            IEvent.EventAction.NONE -> TaskAction.none
            IEvent.EventAction.BATCH_ERROR -> TaskAction.batch_error
            IEvent.EventAction.SEND_ERROR -> TaskAction.send_error
            IEvent.EventAction.WIPE_ERROR -> TaskAction.wipe_error
            IEvent.EventAction.RESEND -> TaskAction.resend
            IEvent.EventAction.REBATCH -> TaskAction.rebatch
            // OTHER is not an expected value, more of a logical fallback/default used in BlobAccess.uploadBody
            IEvent.EventAction.OTHER -> TaskAction.other
        }

//        fun toQueueName(): String? {
//            return when (this) {
//                PROCESS,
//                TRANSLATE,
//                BATCH,
//                SEND,
//                WIPE,
//                -> this.toString().lowercase()
//                else -> null
//            }
//        }
//
//        companion object {
//            fun parseQueueMessage(action: String): EventAction {
//                return when (action.lowercase()) {
//                    "process" -> PROCESS
//                    "receive" -> RECEIVE
//                    "translate" -> TRANSLATE
//                    "batch" -> BATCH
//                    "send" -> SEND
//                    "wipe" -> WIPE
//                    "none" -> NONE
//                    "batch_error" -> BATCH_ERROR
//                    "send_error" -> SEND_ERROR
//                    "wipe_error" -> WIPE_ERROR
//                    else -> error("Internal Error: $action does not match known action names")
//                }
//            }
//        }
//    }

    companion object {
        fun parseQueueMessage(event: String): Event {
            val message = JacksonMapperUtilities.defaultMapper.readValue<QueueMessage>(event)

            return when (message) {
                is ReportEventQueueMessage -> {
                    val at =
                        if (message.at.isNotEmpty()) {
                            OffsetDateTime.parse(message.at)
                        } else {
                            null
                        }
                    ReportEvent(
                        message.eventAction,
                        message.reportId,
                        message.emptyBatch,
                        at,
                    )
                }

                is BatchEventQueueMessage -> {
                    val at =
                        if (message.at.isNotEmpty()) {
                            OffsetDateTime.parse(message.at)
                        } else {
                            null
                        }
                    BatchEvent(
                        message.eventAction,
                        message.receiverName,
                        message.emptyBatch,
                        at,
                    )
                }

                is ProcessEventQueueMessage -> {
                    val at =
                        if (message.at.isNotEmpty()) {
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
                        at,
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
    eventAction: IEvent.EventAction,
    val reportId: UUID,
    val options: Options,
    val defaults: Map<String, String>,
    val routeTo: List<String>,
    at: OffsetDateTime? = null,
    val retryToken: RetryToken? = null,
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val queueMessage =
            ProcessEventQueueMessage(
                eventAction,
                reportId,
                options,
                defaults,
                routeTo,
                afterClause,
            )
        return JacksonMapperUtilities.objectMapper.writeValueAsString(queueMessage)
    }

    override fun equals(other: Any?): Boolean =
        other is ProcessEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken

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
    eventAction: IEvent.EventAction,
    val reportId: UUID,
    val isEmptyBatch: Boolean,
    at: OffsetDateTime? = null,
    val retryToken: RetryToken? = null,
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val queueMessage =
            ReportEventQueueMessage(
                eventAction,
                isEmptyBatch,
                reportId,
                afterClause,
            )
        return JacksonMapperUtilities.objectMapper.writeValueAsString(queueMessage)
    }

    override fun equals(other: Any?): Boolean =
        other is ReportEvent &&
            eventAction == other.eventAction &&
            reportId == other.reportId &&
            at == other.at &&
            retryToken == other.retryToken

    override fun hashCode(): Int =
        (7 * eventAction.hashCode()) +
            (31 * reportId.hashCode()) +
            (17 * at.hashCode()) +
            (19 * retryToken.hashCode())

    companion object {
        const val eventType = "report"
    }
}

/**
 * Queue message for Batch
 */
class BatchEvent(
    eventAction: IEvent.EventAction,
    val receiverName: String,
    val isEmptyBatch: Boolean,
    at: OffsetDateTime? = null,
) : Event(eventAction, at) {
    override fun toQueueMessage(): String {
        val afterClause = if (at == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(at)
        val queueMessage =
            BatchEventQueueMessage(
                eventAction,
                receiverName,
                isEmptyBatch,
                afterClause,
            )
        return JacksonMapperUtilities.objectMapper.writeValueAsString(queueMessage)
    }

    override fun equals(other: Any?): Boolean =
        other is BatchEvent &&
            eventAction == other.eventAction &&
            receiverName == other.receiverName &&
            at == other.at

    override fun hashCode(): Int =
        (7 * eventAction.hashCode()) +
            (19 * receiverName.hashCode()) +
            (17 * at.hashCode())

    // this should say 'batch' but will break production on deploy if there is anything in the batch queue
    //  when it goes to prod. This value is used only to queue and dequeue message types
    //  (toQueueMessage, parseQueueMessage)
    companion object {
        const val eventType = "receiver"
    }
}