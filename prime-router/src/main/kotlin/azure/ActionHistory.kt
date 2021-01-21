package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.transport.RetryToken
import java.time.OffsetDateTime

/**
 * This is a container class that holds information to be stored, about an action
 * that has happened, the reports that went into that Action, and were created by that Action.
 */
class ActionHistory(val actionName: ActionName, val at: OffsetDateTime?) {

    var reportsInput: List<Report> = listOf()
    var reportsOutput: List<Report> = listOf()

    constructor(
        actionName: ActionName,
        at: OffsetDateTime? = null,
        retryToken: RetryToken? = null
    ) : this(actionName, at) {
    }

    enum class ActionName {
        RECEIVE,
        TRANSLATE, // Deprecated
        BATCH,
        SEND,
        WIPE,
        NONE,
        BATCH_ERROR,
        SEND_ERROR,
        WIPE_ERROR;

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
            }
        }

        fun toQueueName(): String? {
            return when (this) {
                TRANSLATE,
                BATCH,
                SEND,
                WIPE -> this.toString().toLowerCase()
                else -> null
            }
        }

        companion object {
            fun parseQueueMessage(actionNameStr: String): ActionName {
                return when (actionNameStr.toLowerCase()) {
                    "receive" -> RECEIVE
                    "translate" -> TRANSLATE
                    "batch" -> BATCH
                    "send" -> SEND
                    "wipe" -> WIPE
                    "none" -> NONE
                    "batch_error" -> BATCH_ERROR
                    "send_error" -> SEND_ERROR
                    "wipe_error" -> WIPE_ERROR
                    else -> error("Internal Error: $actionNameStr does not match known action names")
                }
            }
        }
    }

    companion object {
        const val messageDelimiter = "&"

        fun parseQueueMessage(queueMsg: String): ActionHistory {
            val parts = queueMsg.split(messageDelimiter)
            if (parts.size < 3 || parts.size > 4) error("Internal Error: bad action format")
            val action = ActionName.parseQueueMessage(parts[1])
            val after = parts.getOrNull(3)?.let { OffsetDateTime.parse(it) }
            val actionName = ActionName.parseQueueMessage(parts[0])
            return ActionHistory(actionName, after, null)
        }
    }
}