package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime

/**
 * This class provides a detailed view for data in the `report_file` table and data from other related sources.
 * Due to the large amount of data and logic used here, instead use ReportFileHistory for lists.
 *
 * @param actionId reference to the `action` table for the action that created this file
 * @param actionName the type of action that created this report file
 * @param createdAt when the file was created
 * @param httpStatus response code for the user fetching this report file
 * @param reports other reports that are related to this report file's action log
 * @param logs container for logs generated throughout the fetching of this report file
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class DetailedReportFileHistory(
    val actionId: Long,
    @JsonIgnore
    val actionName: TaskAction,
    @JsonProperty("timestamp")
    val createdAt: OffsetDateTime,
    val httpStatus: Int? = null,
    @JsonIgnore
    var reports: MutableList<DetailReport>?,
    @JsonIgnore
    var logs: List<DetailActionLog> = emptyList()
) {
    /**
     * The report ID.
     */
    var id: String? = null

    /**
     * Errors logged for this Report File.
     */
    val errors = mutableListOf<ConsolidatedActionLog>()

    /**
     * Warnings logged for this Report File.
     */
    val warnings = mutableListOf<ConsolidatedActionLog>()

    /**
     * The schema topic.
     */
    var topic: String? = null

    /**
     * The input report's external name.
     */
    var externalName: String? = null

    /**
     * The number of warnings.  Note this is not the number of consolidated warnings.
     */
    val warningCount = logs.count { it.type == ActionLogLevel.warning }

    /**
     * The number of errors.  Note this is not the number of consolidated errors.
     */
    val errorCount = logs.count { it.type == ActionLogLevel.error }

    /**
     * Number of report items.
     */
    var reportItemCount: Int? = null

    init {
        errors.addAll(consolidateLogs(ActionLogLevel.error))
        warnings.addAll(consolidateLogs(ActionLogLevel.warning))
    }

    /**
     * Consolidate the [logs] filtered by an optional [filterBy] action level, so to list similar messages once
     * with a list of items they relate to.
     * @return the consolidated list of logs
     */
    internal fun consolidateLogs(filterBy: ActionLogLevel? = null):
        List<ConsolidatedActionLog> {
        val consolidatedList = mutableListOf<ConsolidatedActionLog>()

        // First filter the logs and sort by the message.  This first sorting can take care of sorting old messages
        // that contain index numbers like "Report 3: xxxx"
        val filteredList = when (filterBy) {
            null -> logs
            else -> logs.filter { it.type == filterBy }
        }.sortedBy { it.detail.message }
        // Now order the list so that logs contain first non-item messages, then item messages, and item messages
        // are sorted by index.
        val orderedList = (
            filteredList.filter { it.scope != ActionLogScope.item }.sortedBy { it.scope } +
                filteredList.filter { it.scope == ActionLogScope.item }.sortedBy { it.index }
            ).toMutableList()
        // Now consolidate the list
        while (orderedList.isNotEmpty()) {
            // Grab the first log.
            val consolidatedLog = ConsolidatedActionLog(orderedList.first())
            orderedList.removeFirst()
            // Now find similar logs and consolidate it.  Note by using the iterator we can delete on the fly.
            with(orderedList.iterator()) {
                forEach { log ->
                    if (consolidatedLog.canBeConsolidatedWith(log)) {
                        consolidatedLog.add(log)
                        remove()
                    }
                }
            }
            consolidatedList.add(consolidatedLog)
        }

        return consolidatedList
    }
}