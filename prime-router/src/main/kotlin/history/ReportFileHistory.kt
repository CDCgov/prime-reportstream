package gov.cdc.prime.router.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ItemActionLogDetail
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This class provides a base structure for data reflected in the `report_file` table.
 * When a report is sent, received, processed or downloaded, one of these entries is created.
 * The small amount of data makes this ideal for lists.
 *
 * @property actionId reference to the `action` table for the action that created this file
 * @property createdAt when the file was created
 * @property httpStatus response code for the user fetching this report file
 * @property externalName actual filename of the file
 * @property reportId unique identifier for this specific report file
 * @property schemaTopic the kind of data contained in the report (e.g. "covid-19")
 * @property itemCount number of tests (data rows) contained in the report
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class ReportFileHistory(
    val actionId: Long,
    @JsonProperty("timestamp")
    val createdAt: OffsetDateTime,
    val httpStatus: Int,
    @JsonInclude(Include.NON_NULL)
    val externalName: String? = "",
    @JsonProperty("id")
    val reportId: String? = null,
    @JsonProperty("topic")
    val schemaTopic: String? = null,
    @JsonProperty("reportItemCount")
    val itemCount: Int? = null,
)

/**
 * This class provides a detailed view for data in the `report_file` table and data from other related sources.
 * Due to the large amount of data and logic used here, instead use ReportFileHistory for lists.
 *
 * @property actionId reference to the `action` table for the action that created this file
 * @property actionName the type of action that created this report file
 * @property createdAt when the file was created
 * @property httpStatus response code for the user fetching this report file
 * @property reports other reports that are related to this report file's action log
 * @property logs container for logs generated throughout the fetching of this report file
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
    var reports: MutableList<DetailedReport>?,
    @JsonIgnore
    var logs: List<DetailedActionLog> = emptyList()
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

/**
 * This is a container for various bits of report data used by DetailedReportFileHistory
 *
 * @property reportId unique identifier for this specific report
 * @property receivingOrg where is this report going?
 * @property receivingOrgSvc what service is receiving this report?
 * @property sendingOrg who sent this report?
 * @property sendingOrgClient what service did the sender use to send this report?
 * @property schemaTopic the kind of data contained in the report (e.g. "covid-19")
 * @property externalName actual filename of the report's file
 * @property createdAt when the report was created
 * @property nextActionAt when the report is next expected to send or process
 * @property itemCount number of tests (data rows) contained in the report
 * @property itemCountBeforeQualFilter number of tests that were submitted by the sender
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedReport(
    val reportId: UUID,
    @JsonIgnore
    val receivingOrg: String?,
    @JsonIgnore
    val receivingOrgSvc: String?,
    @JsonIgnore
    val sendingOrg: String?,
    @JsonIgnore
    val sendingOrgClient: String?,
    @JsonIgnore
    val schemaTopic: String?,
    val externalName: String?,
    val createdAt: OffsetDateTime?,
    val nextActionAt: OffsetDateTime?,
    val itemCount: Int,
    @JsonIgnore
    val itemCountBeforeQualFilter: Int?,
)

/**
 * Consolidated action log class to be output to the API JSON response.
 * @property log the base log message to be consolidated
 */
@JsonInclude(Include.NON_NULL)
class ConsolidatedActionLog(log: DetailedActionLog) {
    /**
     * The scope of the log.
     */
    val scope: ActionLogScope

    /**
     * The list of indices for item logs. An index can be null if there was no index provided with the log.
     */
    val indices: MutableList<Int?>?

    /**
     * The list of tracking IDs for item logs. A tracking ID can be null if there was no ID provided with the log.
     */
    val trackingIds: MutableList<String?>?

    /**
     * The log level.
     */
    @JsonIgnore
    val type: ActionLogLevel

    /**
     * The field mapping for item logs.
     */
    val field: String?

    /**
     * The log message.
     */
    val message: String

    init {
        scope = log.scope
        type = log.type
        message = log.detail.message
        if (log.detail.scope == ActionLogScope.item) {
            field = if (log.detail is ItemActionLogDetail) log.detail.fieldMapping else null
            indices = mutableListOf()
            trackingIds = mutableListOf()
        } else {
            indices = null
            trackingIds = null
            field = null
        }
        add(log)
    }

    /**
     * Add an action detail [log] to this consolidated log.
     */
    fun add(log: DetailedActionLog) {
        check(message == log.detail.message)
        if (indices != null && trackingIds != null) {
            indices.add(log.index)
            trackingIds.add(log.trackingId)
        }
    }

    /**
     * Tests if a detail action log [other] can be consolidated into this existing consolidated log.
     * @return true if the log can be consolidated, false otherwise
     */
    fun canBeConsolidatedWith(other: DetailedActionLog): Boolean {
        return this.message == other.detail.message && this.scope == other.scope && this.type == other.type
    }
}

/**
 * Detail action log class used to read the data from the database.
 *
 * @property scope the level in which this log ocurred (e.g. report, item...)
 * @property reportId unique identifier for the report that owns this log
 * @property index position in the report of the item that caused this log
 * @property trackingId id for identifying the test this log is related to
 * @property type what kind of log is this? (e.g. filter, warning...)
 * @property detail additional information for this log
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class DetailedActionLog(
    val scope: ActionLogScope,
    @JsonIgnore
    val reportId: UUID?,
    val index: Int?,
    val trackingId: String?,
    val type: ActionLogLevel,
    val detail: ActionLogDetail,
)