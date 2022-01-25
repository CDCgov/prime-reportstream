package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime

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

@JsonIgnoreProperties(ignoreUnknown = true)
class DetailedSubmissionHistory(
    @JsonProperty("id")
    val actionId: Long,
    @JsonIgnore
    val actionName: TaskAction,
    @JsonProperty("submittedAt")
    val createdAt: OffsetDateTime,
    @JsonProperty("submitter")
    val sendingOrg: String?,
    val httpStatus: Int?,
    @JsonIgnore
    val receivingOrg: String?,
    @JsonIgnore
    val receivingOrgSvc: String?,
    @JsonInclude(Include.NON_NULL) val externalName: String? = "",
    actionResponse: DetailedActionResponse?,
) {
    val submissionId: String? = actionResponse?.id
    val destinations = mutableListOf<Destination>()
    // TODO: these should just be getters

    val errors = mutableListOf<Detail>()
    val warnings = mutableListOf<Detail>()
    val topic: String? = actionResponse?.topic

    val warningCount: Int
        get() = warnings.size

    val errorCount: Int
        get() = errors.size

    init {
        destinations.addAll(actionResponse?.destinations ?: emptyList())
        errors.addAll(actionResponse?.errors ?: emptyList())
        warnings.addAll(actionResponse?.warnings ?: emptyList())
    }

    fun enrichWithDescendants(descendants: List<DetailedSubmissionHistory>) {
        descendants.forEach { descendant ->
            enrichWithDescendant(descendant)
        }
    }

    fun enrichWithDescendant(descendant: DetailedSubmissionHistory) {
        when (descendant.actionName) {
            TaskAction.process -> enrichWithProcessAction(descendant)
            // TaskAction.batch -> enrichWithBatchAction(descendant)
            TaskAction.send -> enrichWithSendAction(descendant)
            // TaskAction.download -> enrichWithDownloadAction(descendant)
            else -> {}
        }
    }

    private fun enrichWithProcessAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.process) { "Must be a process action" }

        destinations += descendant.destinations
        errors += descendant.errors
        warnings += descendant.warnings
    }

    /*
    private fun enrichWithBatchAction(descendant: DetailedSubmissionHistory) {

    }
    */

    private fun enrichWithSendAction(descendant: DetailedSubmissionHistory) {
        require(descendant.actionName == TaskAction.send) { "Must be a send action" }
        destinations.find {
            it.organizationId == descendant.receivingOrg && it.service == descendant.receivingOrgSvc
        }?.let {
            it.sentAt = descendant.createdAt
        }
    }
    /*
    private fun enrichWithDownloadAction(descendant: DetailedSubmissionHistory) {

    }
    */
}

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
    val organization: String,
    @JsonProperty("organization_id")
    val organizationId: String,
    val service: String,
    val filteredReportRows: List<String>?,
    @JsonProperty("sending_at")
    val sendingAt: String,
    val itemCount: Int,
    var sentAt: OffsetDateTime?,
)

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