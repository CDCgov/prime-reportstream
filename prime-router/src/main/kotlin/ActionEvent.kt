package gov.cdc.prime.router
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import java.time.Instant
import java.util.UUID

/**
 * ActionEvent models events that happen over the
 * execution of an Action
 *
 * TODO: Rename Event (in Event.kt) to message to better
 * represent it's purpose.
 *
 * @property scope of the event
 * @property details about the event
 * @property trackingId The ID of the item if applicable
 * @property index of the item related to message
 * @property reportId The ID of the report to which this event happened
 * @property action A reference to the action this event occured durring
 * @property type The type of even that happened, defaults to info
 * @property created_at The time the event happened durring execution
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
     * @property REPORT scope for the event
     * @property PARAMETER scope for the event
     * @property ITEM scope for the event
     * @property TRANSLATION scope for the event
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
         * @return The created event with report scope
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
         * @return The created event with report scope and InvalidReportMessage detail
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
         * @return The created event with item scope
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
         * @return The created event with InvalidParamMessage detail
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
         * @return The created event with InvalidParamMessage detail
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
 *
 * @property details are the events that occured to trigger this error
 * @property message text describing the error
 */
class ActionError(val details: List<ActionEvent>, message: String? = null) : Error(message) {
    constructor(detail: ActionEvent, message: String? = null) : this(listOf(detail), message)
    constructor(details: List<ActionEvent>) : this(details, null)
}