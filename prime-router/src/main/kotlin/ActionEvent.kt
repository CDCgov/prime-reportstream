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
        fun report(message: ActionEventDetail, type: ActionEventType, reportId: UUID? = null): ActionEvent {
            return ActionEvent(ActionEventScope.report, message, type = type, reportId = reportId)
        }

        fun report(message: String, type: ActionEventType = ActionEventType.error): ActionEvent {
            val reportMessage = InvalidReportMessage(message)
            return ActionEvent(ActionEventScope.report, reportMessage, type = type)
        }

        fun item(trackingId: String, message: ActionEventDetail, row: Int, type: ActionEventType): ActionEvent {
            return ActionEvent(ActionEventScope.item, message, trackingId, row, type = type)
        }

        fun param(
            trackingId: String,
            message: ActionEventDetail,
            type: ActionEventType = ActionEventType.error
        ): ActionEvent {
            return ActionEvent(ActionEventScope.parameter, message, trackingId, type = type)
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