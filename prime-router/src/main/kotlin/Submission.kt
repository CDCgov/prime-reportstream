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
 * A `Submission` represents one submission of a message from a sender.
 *
 * @param taskId of the Submission is `action_id` from table `public.action`
 * @param createdAt of the Submission is `created_at` from the table `public.action`
 * @param sendingOrg of the Submission is `sending_org` from the table `public.action`
 * @param httpStatus of the Submission is `http_status` from the table `public.action`
 * @param id of the Submission is `action_response.id` and represents a Report ID from the table `public.action`
 * @param topic of the Submission is `action_response.topic` from the table `public.action`
 * @param reportItemCount of the Submission is `action_response.reportItemCount` and represents a Report ID from the table `public.action`
 * @param warningCount of the Submission is `action_response.warningCount` from the table `public.action`
 * @param errorCount of the Submission is `action_response.errorCount` from the table `public.action`
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class DetailedSubmissionHistory(
    @JsonProperty("submissionId")
    val actionId: Long,
    @JsonIgnore
    val actionName: TaskAction,
    @JsonProperty("submittedAt")
    val createdAt: OffsetDateTime,
    @JsonProperty("submitter")
    val sendingOrg: String?,
    val httpStatus: Int?,
    @JsonInclude(Include.NON_NULL) val externalName: String? = "",
    actionResponse: DetailedActionResponse?,
    @JsonIgnore
    var reports: MutableList<DetailReport>?,
    @JsonIgnore
    val logs: List<DetailActionLog>?,
) {
    val receivedReportId: String? = actionResponse?.id
    val destinations = mutableListOf<Destination>()

    val errors = mutableListOf<DetailActionLog>()
    val warnings = mutableListOf<DetailActionLog>()

    val topic: String? = actionResponse?.topic

    val warningCount: Int
        get() = warnings.size

    val errorCount: Int
        get() = errors.size

    init {
        reports?.forEach { report ->
            report.receivingOrg?.let {
                destinations.add(
                    Destination(
                        report.receivingOrg,
                        report.receivingOrgSvc!!,
                        logs?.filter {
                            it.type == ActionLog.ActionLogType.filter && it.reportId == report.reportId
                        }?.map { it.message },
                        report.nextActionAt?.toString() ?: "",
                        report.itemCount,
                    )
                )
            }
        }
        errors.addAll(logs?.filter { it.type == ActionLog.ActionLogType.error } ?: emptyList())
        warnings.addAll(logs?.filter { it.type == ActionLog.ActionLogType.warning } ?: emptyList())
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
            TaskAction.batch -> enrichWithBatchAction(descendant)
            TaskAction.send -> enrichWithSendAction(descendant)
            // TaskAction.download -> enrichWithDownloadAction(descendant)
            else -> {}
        }
    }

    private fun enrichWithProcessAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.process) { "Must be a process action" }

        descendant.reports?.forEach { report ->
            report.receivingOrg?.let {
                destinations.add(
                    Destination(
                        report.receivingOrg,
                        report.receivingOrgSvc!!,
                        descendant.logs?.filter {
                            it.type == ActionLog.ActionLogType.filter && it.reportId == report.reportId
                        }?.map { it.message },
                        report.nextActionAt?.toString() ?: "",
                        report.itemCount,
                    )
                )
            }
        }
        errors += descendant.errors
        warnings += descendant.warnings
    }

    private fun enrichWithBatchAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.batch) { "Must be a process action" }
    }

    private fun enrichWithSendAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.send) { "Must be a send action" }
        descendant.reports?.let { it ->
            it.forEach { report ->
                destinations.find {
                    it.organizationId == report.receivingOrg && it.service == report.receivingOrgSvc
                }?.let {
                    it.sentReports.add(report)
                }
            }
        }
    }
    /*
    private fun enrichWithDownloadAction(descendant: DetailedSubmissionHistory) {

    }
    */
}

@JsonIgnoreProperties(ignoreUnknown = true)
class DetailActionLog(
    val scope: ActionLog.ActionLogScope,
    @JsonIgnore
    val reportId: UUID,
    val index: Int?,
    val trackingId: String?,
    val type: ActionLog.ActionLogType,
    detail: ActionLogDetail,
) {
    val message: String = detail.detailMsg()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailReport(
    val reportId: UUID,
    val receivingOrg: String?,
    val receivingOrgSvc: String?,
    val externalName: String?,
    val createdAt: OffsetDateTime?,
    val nextActionAt: OffsetDateTime?,
    val itemCount: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedActionResponse(
    val id: String?,
    val topic: String?,
    val destinations: List<Destination>?,
    val reportItemCount: Int?,
    val warningCount: Int?,
    val errorCount: Int?,
    val errors: List<Detail>?,
    val warnings: List<Detail>?,
)

data class Detail(
    val scope: String?,
    val message: String?,
    val itemNums: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Destination(
    @JsonProperty("organization_id")
    val organizationId: String,
    val service: String,
    val filteredReportRows: List<String>?,
    @JsonProperty("sending_at")
    val sendingAt: String,
    val itemCount: Int,
    var sentReports: MutableList<DetailReport> = mutableListOf(),
) {
    val organization: String?
        get() = WorkflowEngine.settingsProviderSingleton.findOrganizationAndReceiver(
            "$organizationId.$service"
        )?.let { (org, _) ->
            org.description
        }
}

/*
 * TODO: see Github Issues #2314 for expected filename field
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class SubmissionHistory(
    @JsonProperty("taskId")
    val actionId: Long,
    val createdAt: OffsetDateTime,
    val sendingOrg: String,
    val httpStatus: Int,
    @JsonInclude(Include.NON_NULL) val externalName: String? = "",
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