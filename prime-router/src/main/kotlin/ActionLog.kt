package gov.cdc.prime.router
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import java.time.Instant
import java.util.UUID

enum class ActionLogScope {
    // TODO Need a way to change these to uppercase, but keep the values in the DB in lowercase for compatibility
    parameter, report, item, translation
}

enum class ActionLogLevel {
    // TODO Need a way to change these to uppercase, but keep the values in the DB in lowercase for compatibility
    info, warning, error, filter
}

/**
 * ActionLog models events that happen over the
 * execution of an Action
 *
 * TODO: Rename Event (in Event.kt) to message to better
 * represent it's purpose.
 *
 * @property scope of the event
 * @property detail about the event
 * @property trackingId The ID of the item if applicable
 * @property index of the item related to message
 * @property reportId The ID of the report to which this event happened
 * @property action A reference to the action this event occured durring
 * @property type The type of even that happened, defaults to info
 * @property created_at The time the event happened durring execution
 */
data class ActionLog(
    val detail: ActionLogDetail,
    val trackingId: String? = null,
    val index: Int? = null,
    var reportId: UUID? = null,
    var action: Action? = null,
    val type: ActionLogLevel = ActionLogLevel.info,
    val created_at: Instant = Instant.now(),
) {
    val scope = detail.scope

    fun getActionId(): Long {
        return action!!.actionId
    }
}

/**
 * ActionError is a throwable for cases where an event during an action
 * is a true error that prevents subsequent behavior.
 *
 * @property details are the events that occured to trigger this error
 * @property message text describing the error
 */
class ActionError(val details: List<ActionLog>, message: String? = null) : Error(message) {
    constructor(detail: ActionLog, message: String? = null) : this(listOf(detail), message)
    constructor(details: List<ActionLog>) : this(details, null)

    override val message: String
        get() {
            // Include the log details in the exception message
            var message = super.message ?: ""
            if (details.isNotEmpty()) {
                message += System.lineSeparator() +
                    details.joinToString(System.lineSeparator()) { it.detail.message }
            }
            return message
        }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
@JsonIgnoreProperties(ignoreUnknown = true)
interface ActionLogDetail {
    val scope: ActionLogScope
    val message: String
    val groupingId: String
}

class ActionLogs() {
    private val rawLogs = mutableListOf<ActionLog>()
    private var itemIndex: Int? = null
    private var trackingId: String? = null
    private var reportId: UUID? = null

    fun startItemLogging(itemIndex: Int, trackingId: String? = null) = apply {
        check(itemIndex > 0) { "Item index must be a positive number" }
        this.itemIndex = itemIndex
        this.trackingId = trackingId
    }

    fun stopItemLogging() = apply {
        this.itemIndex = null
        this.trackingId = null
    }

    fun setReportId(reportId: UUID?) = apply {
        this.reportId = reportId
        // Apply the report ID to all the exiting logs.
        rawLogs.forEach { it.reportId = reportId }
    }

    fun hasErrors(): Boolean {
        return rawLogs.any { it.type == ActionLogLevel.error }
    }

    fun isEmpty(): Boolean {
        return rawLogs.isEmpty()
    }

    fun log(
        actionDetail: ActionLogDetail,
        type: ActionLogLevel
    ) {
        rawLogs.add(ActionLog(actionDetail, trackingId, itemIndex, reportId, type = type))
    }

    fun warn(actionDetail: ActionLogDetail) {
        log(actionDetail, ActionLogLevel.warning)
    }

    fun warn(actionDetails: List<ActionLogDetail>) {
        actionDetails.forEach { warn(it) }
    }

    fun error(actionDetail: ActionLogDetail) {
        log(actionDetail, ActionLogLevel.error)
    }

    fun error(actionDetails: List<ActionLogDetail>) {
        actionDetails.forEach { error(it) }
    }

    val exception get() = ActionError(rawLogs)

    val errors get() = rawLogs.filter { it.type == ActionLogLevel.error }
    val warnings get() = rawLogs.filter { it.type == ActionLogLevel.warning }
    val logs get() = rawLogs
}