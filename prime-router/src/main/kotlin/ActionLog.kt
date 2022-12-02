package gov.cdc.prime.router
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import java.time.Instant
import java.util.UUID

/**
 * The scope of an action log.
 */
enum class ActionLogScope {
    parameter, report, item, translation
}

/**
 * The log level of an action log.
 */
enum class ActionLogLevel {
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
    val created_at: Instant = Instant.now()
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

/**
 * Details for a given action log.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
@JsonIgnoreProperties(ignoreUnknown = true)
interface ActionLogDetail {
    /**
     * The scope of the log.
     */
    val scope: ActionLogScope

    /**
     * The log message.
     */
    val message: String

    /**
     * The error code used to translate the error in the UI.
     */
    val errorCode: String
}

/**
 * A logger for action logs.
 * @property logs the logs
 */
class ActionLogger(val logs: MutableList<ActionLog> = mutableListOf()) {

    /**
     * Create an item logger to add logs to [logs] for the given [itemIndex] and [trackingId].
     */
    private constructor(logs: MutableList<ActionLog>, itemIndex: Int, trackingId: String? = null) : this(logs) {
        this.itemIndex = itemIndex
        this.trackingId = trackingId
    }
    /**
     * The current item index being tracked, or null if no index is tracked.
     */
    private var itemIndex: Int? = null

    /**
     * The current item tracking ID being tracked, or null if no tracking ID is tracked.
     */
    private var trackingId: String? = null

    /**
     * The report ID of the logs if available.
     */
    private var reportId: UUID? = null

    /**
     * Get a logger used to log item scoped logs for the given [itemIndex] and [trackingId].
     * @return the item action logger
     */
    fun getItemLogger(itemIndex: Int, trackingId: String? = null): ActionLogger {
        check(itemIndex > 0) { "Item index must be a positive number" }
        return ActionLogger(this.logs, itemIndex, trackingId)
    }

    /**
     * Set the [reportId] for the logs.  All previously logged messages are associated with this report ID.
     * @return the logger instance
     */
    fun setReportId(reportId: UUID?) = apply {
        this.reportId = reportId
        // Apply the report ID to all the exiting logs.
        logs.forEach { it.reportId = reportId }
    }

    /**
     * Check if the logger has logged any errors.
     * @return true if there are errors logged, false otherwise
     */
    fun hasErrors(): Boolean {
        return logs.any { it.type == ActionLogLevel.error }
    }

    /**
     * Check if any logs have been logged.
     * @return true if any log has been logged, false otherwise.
     */
    fun isEmpty(): Boolean {
        return logs.isEmpty()
    }

    /**
     * Log a given [actionDetail] and a [level] log level.
     */
    private fun log(
        actionDetail: ActionLogDetail,
        level: ActionLogLevel
    ) {
        if (actionDetail.scope == ActionLogScope.item) check(itemIndex != null) {
            "Index is required for item logs.  Use the item action logger"
        }
        logs.add(ActionLog(actionDetail, trackingId, itemIndex, reportId, type = level))
    }

    /**
     * Log an [actionDetail] as a warning log.
     */
    fun warn(actionDetail: ActionLogDetail) {
        log(actionDetail, ActionLogLevel.warning)
    }

    /**
     * Log a list of [actionDetails] as warning logs.
     */
    fun warn(actionDetails: List<ActionLogDetail>) {
        actionDetails.forEach { warn(it) }
    }

    /**
     * Log an [actionDetail] as an error log.
     */
    fun error(actionDetail: ActionLogDetail) {
        log(actionDetail, ActionLogLevel.error)
    }

    /**
     * Log a list of [actionDetails] as error logs.
     */
    fun error(actionDetails: List<ActionLogDetail>) {
        actionDetails.forEach { error(it) }
    }

    /**
     * Get an exception to be thrown that includes all the logs.
     */
    val exception get() = ActionError(logs)

    /**
     * The logged errors.
     */
    val errors get() = logs.filter { it.type == ActionLogLevel.error }

    /**
     * The logged warnings.
     */
    val warnings get() = logs.filter { it.type == ActionLogLevel.warning }
}