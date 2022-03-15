package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A `DetailedSubmissionHistory` represents the detailed life history of a submission of a message from a sender.
 *
 * @param actionId of the Submission is `action_id` from the `action` table
 * @param actionName of the Submission is `action_name` from the `action` table
 * @param createdAt of the Submission is `created_at` from the the `action` table
 * @param sendingOrg of the Submission is `sending_org` from the the `action` table
 * @param httpStatus of the Submission is `http_status` from the the `action` table
 * @param externalName of the Submission is `external_name` from the the `action` table
 * @param actionResponse of the Submission is the structured JSON from the `action` table
 * @param reports of the Submission are the Reports related to the action from the `report_file` table
 * @param logs of the Submission are the Logs produced by the submission from the `action_log` table
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DetailedSubmissionHistory(
    @JsonProperty("submissionId")
    val actionId: Long,
    @JsonIgnore
    val actionName: TaskAction,
    @JsonProperty("timestamp")
    val createdAt: OffsetDateTime,
    @JsonProperty("sender")
    val sendingOrg: String?,
    val httpStatus: Int?,
    val externalName: String? = null,
    @JsonIgnore
    val actionResponse: DetailedActionResponse?,
    @JsonIgnore
    var reports: MutableList<DetailReport>?,
    @JsonIgnore
    val logs: List<DetailActionLog>?
) {
    val id: String? = actionResponse?.id
    val destinations = mutableListOf<Destination>()

    /**
     * Error log for the submission.
     */
    val errors = mutableListOf<ConsolidatedActionLog>()

    /**
     * Warning log for the submission.
     */
    val warnings = mutableListOf<ConsolidatedActionLog>()

    val topic: String? = actionResponse?.topic

    val warningCount: Int
        get() = warnings.size

    val errorCount: Int
        get() = errors.size

    /**
     * Number of destinations that actually had/will have data sent to.
     */
    val destinationCount: Int get() {
        return destinations.filter { it.itemCount != 0 }.size
    }

    /**
     * Number of report items.
     */
    val reportItemCount: Int?
        get() = actionResponse?.reportItemCount

    init {
        reports?.forEach { report ->
            report.receivingOrg?.let {
                destinations.add(
                    Destination(
                        report.receivingOrg,
                        report.receivingOrgSvc!!,
                        logs?.filter {
                            it.type == ActionLogLevel.filter && it.reportId == report.reportId
                        }?.map { it.detail.message },
                        report.nextActionAt,
                        report.itemCount,
                    )
                )
            }
        }
        errors.addAll(consolidateLogs(ActionLogLevel.error))
        warnings.addAll(consolidateLogs(ActionLogLevel.warning))
    }

    fun enrichWithDescendants(descendants: List<DetailedSubmissionHistory>) {
        check(descendants.distinctBy { it.actionId }.size == descendants.size)
        descendants.forEach { descendant ->
            enrichWithDescendant(descendant)
        }
    }

    fun enrichWithDescendant(descendant: DetailedSubmissionHistory) {
        when (descendant.actionName) {
            TaskAction.process -> enrichWithProcessAction(descendant)
            // TaskAction.batch -> enrichWithBatchAction(descendant)
            TaskAction.send -> enrichWithSendAction(descendant)
            TaskAction.download -> enrichWithDownloadAction(descendant)
            else -> {}
        }
    }

    /**
     * Enrich a parent detailed history with details from the process action
     *
     * Add destinations, errors, and warnings, to the history details.
     */
    private fun enrichWithProcessAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.process) { "Must be a process action" }

        destinations += descendant.destinations
        errors += descendant.errors
        warnings += descendant.warnings
    }

    /*
    private fun enrichWithBatchAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.batch) { "Must be a process action" }
    }
    */

    /**
     * Enrich a parent detailed history with details from the send action
     *
     * Add sent report information to each destination present in the parent's historical details.
     */
    private fun enrichWithSendAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.send) { "Must be a send action" }
        descendant.reports?.let { it ->
            it.forEach { report ->
                destinations.find {
                    it.organizationId == report.receivingOrg && it.service == report.receivingOrgSvc
                }?.let {
                    it.sentReports.add(report)
                } ?: run {
                    if (report.receivingOrg != null && report.receivingOrgSvc != null) {
                        destinations.add(
                            Destination(
                                report.receivingOrg,
                                report.receivingOrgSvc,
                                listOf(),
                                null,
                                report.itemCount,
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Enrich a parent detailed history with details from the download action
     *
     * Add download report information to each destination present in the parent's historical details.
     */
    private fun enrichWithDownloadAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.download) { "Must be a download action" }

        descendant.reports?.let { it ->
            it.forEach { report ->
                destinations.find {
                    it.organizationId == report.receivingOrg && it.service == report.receivingOrgSvc
                }?.let {
                    it.downloadedReports.add(report)
                } ?: run {
                    if (report.receivingOrg != null && report.receivingOrgSvc != null) {
                        val dest = Destination(
                            report.receivingOrg,
                            report.receivingOrgSvc,
                            listOf(),
                            null,
                            report.itemCount,
                        )

                        destinations.add(dest)
                        dest.downloadedReports.add(report)
                    }
                }
            }
        }
    }

    /**
     * Consolidate the [logs] filtered by an optional [filterBy] action level, so to list similar messages once
     * with a list of items they relate to.
     * @return the consolidated list of logs
     */
    internal fun consolidateLogs(filterBy: ActionLogLevel? = null):
        List<ConsolidatedActionLog> {
        val consolidatedList = mutableListOf<ConsolidatedActionLog>()
        if (logs != null) {
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
        }
        return consolidatedList
    }
}

/**
 * Consolidated action log class to be output to the API JSON response.
 * @param log the base log message to be consolidated
 */
@JsonInclude(Include.NON_NULL)
class ConsolidatedActionLog(log: DetailActionLog) {
    /**
     * The scope of the log.
     */
    var scope: ActionLogScope

    /**
     * The list of indices for item logs. An index can be null if there was no index provided with the log.
     */
    var indices: MutableList<Int?>? = null

    /**
     * The list of tracking IDs for item logs. A tracking ID can be null if there was no ID provided with the log.
     */
    var trackingIds: MutableList<String?>? = null

    /**
     * The log level.
     */
    var type: ActionLogLevel

    /**
     * The field mapping for item logs.
     */
    var field: String? = null

    /**
     * The log message.
     */
    var message: String

    init {
        scope = log.scope
        type = log.type
        message = log.detail.message
        if (log.detail is ItemActionLogDetail) {
            field = log.detail.fieldMapping
            indices = mutableListOf()
            trackingIds = mutableListOf()
        }
        add(log)
    }

    /**
     * Add an action detail [log] to this consolidated log.
     */
    fun add(log: DetailActionLog) {
        check(message == log.detail.message)
        if (indices != null && trackingIds != null) {
            indices!!.add(log.index)
            trackingIds!!.add(log.trackingId)
        }
    }

    /**
     * Tests if a detail action log [other] can be consolidated into this existing consolidated log.
     * @return true if the log can be consolidated, false otherwise
     */
    fun canBeConsolidatedWith(other: DetailActionLog): Boolean {
        return this.message == other.detail.message && this.scope == other.scope && this.type == other.type
    }
}

/**
 * Detail action log class used to read the data from the database.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class DetailActionLog(
    val scope: ActionLogScope,
    @JsonIgnore
    val reportId: UUID?,
    val index: Int?,
    val trackingId: String?,
    val type: ActionLogLevel,
    val detail: ActionLogDetail,
)

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailReport(
    val reportId: UUID,
    @JsonIgnore
    val receivingOrg: String?,
    @JsonIgnore
    val receivingOrgSvc: String?,
    val externalName: String?,
    val createdAt: OffsetDateTime?,
    val nextActionAt: OffsetDateTime?,
    val itemCount: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedActionResponse(
    val id: String?,
    val topic: String?,
    val reportItemCount: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Destination(
    @JsonProperty("organization_id")
    val organizationId: String,
    val service: String,
    val filteredReportRows: List<String>?,
    @JsonProperty("sending_at")
    @JsonInclude(Include.NON_NULL)
    val sendingAt: OffsetDateTime?,
    val itemCount: Int,
    var sentReports: MutableList<DetailReport> = mutableListOf(),
    var downloadedReports: MutableList<DetailReport> = mutableListOf(),
) {
    val organization: String?
        get() = WorkflowEngine.settingsProviderSingleton.findOrganizationAndReceiver(
            "$organizationId.$service"
        )?.let { (org, _) ->
            org.description
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SubmissionHistory(
    @JsonProperty("taskId")
    val actionId: Long,
    val createdAt: OffsetDateTime,
    val sendingOrg: String,
    val httpStatus: Int,
    @JsonInclude(Include.NON_NULL)
    val externalName: String? = "",
    actionResponse: ActionResponse,
) {
    @JsonUnwrapped
    val actionReponse = actionResponse
}

/**
 * An `ActionResponse` represents the required information from the `action.action_reponse` column for one submission of a message from a sender.
 *
 * @param id of the Submission is `action_response.id` and represents a Report ID from the table `public.action`
 * @param topic of the Submission is `action_response.topic` from the table `public.action`
 * @param reportItemCount of the Submission is `action_response.reportItemCount` and represents a Report ID from the table `public.action`
 * @param warningCount of the Submission is `action_response.warningCount` from the table `public.action`
 * @param errorCount of the Submission is `action_response.errorCount` from the table `public.action`
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActionResponse(
    val id: String?,
    val topic: String?,
    val reportItemCount: Int?,
    val warningCount: Int?,
    val errorCount: Int?,
)