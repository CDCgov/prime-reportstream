package gov.cdc.prime.reportstream.shared.azure

import java.time.OffsetDateTime

interface IEvent {
    val eventAction: EventAction
    val at: OffsetDateTime?

    fun toQueueMessage(): String

    enum class EventAction {
        PROCESS, // for when a message goes into a queue to be processed
        PROCESS_WARNING, // when an attempt at a process action fails, but will be retried
        PROCESS_ERROR, // when an attempt at a process action fails permanently
        DESTINATION_FILTER,
        RECEIVER_FILTER,
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
        REBATCH,
        OTHER, // a default/unknown
        ;

        fun toQueueName(): String? =
            when (this) {
                PROCESS,
                TRANSLATE,
                BATCH,
                SEND,
                WIPE,
                -> this.toString().lowercase()

                else -> null
            }

        companion object {
            fun parseQueueMessage(action: String): EventAction =
                when (action.lowercase()) {
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