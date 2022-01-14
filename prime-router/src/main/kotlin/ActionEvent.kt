package gov.cdc.prime.router
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import java.time.Instant
import java.util.UUID

/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 * @property row of csv related to message (set to -1 if not applicable)
 */
data class ActionEvent(
    val scope: ActionEventScope,
    val detail: ActionEventDetail,
    val trackingId: String? = null,
    val index: Int? = null,
    var reportId: UUID? = null,
    var action: Action? = null,
    val type: ActionEventType = ActionEventType.info,
    val created_at: Instant = Instant.now(),
) {

    fun getActionId(): Long {
        return action!!.actionId
    }

    /**
     * @property REPORT scope for the detail
     * @property ITEM scope for the detail
     */
    enum class ActionEventScope { parameter, report, item, translation }

    enum class ActionEventType { info, warning, error, filter }

    companion object {
        /**
         * Record events that occure at the report level
         *
         * These can either relate directly to a report, or not if a report failed to be created.
         *
         * @param message The detailed information about the event
         * @param type Usually error
         * @param reportId The identifier for the report this event happened to
         */
        fun report(message: ActionEventDetail, type: ActionEventType, reportId: UUID? = null): ActionEvent {
            return ActionEvent(ActionEventScope.report, message, type = type, reportId = reportId)
        }

        /**
         * Record events that occur at the report level
         *
         * These can either relate directly to a report, or not if a report failed to be created.
         *
         * @param message The detailed information about the event
         * @param type Usually error
         */
        fun report(message: String, type: ActionEventType = ActionEventType.error): ActionEvent {
            val reportMessage = InvalidReportMessage(message)
            return ActionEvent(ActionEventScope.report, reportMessage, type = type)
        }

        /**
         * Record events that occur at the item level
         *
         * @param trackingId The tracking ID for the item
         * @param message The detailed information about the event
         * @param index The index of the item within the report
         * @param type The type of the event
         */
        fun item(trackingId: String, message: ActionEventDetail, index: Int, type: ActionEventType): ActionEvent {
            return ActionEvent(ActionEventScope.item, message, trackingId, index, type = type)
        }

        /**
         * Record events that occur at the request parameter level
         *
         * @param httpParameter the http parameter that caused the event
         * @param detail The detailed information about the event
         * @param type The type of the event
         */
        fun param(
            httpParameter: String,
            detail: ActionEventDetail,
            type: ActionEventType = ActionEventType.error
        ): ActionEvent {
            return ActionEvent(
                ActionEventScope.parameter,
                InvalidParamMessage(
                    httpParameter,
                    "",
                    detail,
                ),
                type = type
            )
        }

        /**
         * Record events that occur at the request parameter level
         *
         * @param httpParameter the http parameter that caused the event
         * @param message A detailed message about the event
         * @param type The type of the event
         */
        fun param(
            httpParameter: String,
            message: String,
            type: ActionEventType = ActionEventType.error
        ): ActionEvent {
            return ActionEvent(
                ActionEventScope.parameter,
                InvalidParamMessage(
                    httpParameter,
                    message,
                ),
                type = type
            )
        }
    }
}

/**
 * ActionError is a throwable for cases where an event during an action
 * is a true error that prevents subsequent behavior.
 */
class ActionError(message: String?, val details: List<ActionEvent>) : Error(message) {
    constructor(detail: ActionEvent, message: String? = null) : this(message, listOf(detail))
    constructor(details: List<ActionEvent>) : this(null, details)
}