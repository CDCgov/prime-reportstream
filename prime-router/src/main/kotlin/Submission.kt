package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
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
    @JsonProperty("submittedAt")
    val createdAt: OffsetDateTime,
    @JsonProperty("submitter")
    val sendingOrg: String?,
    val httpStatus: Int?,
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

    fun enrich(relations: List<DetailedSubmissionHistory>) {
        relations.forEach { relation ->
            // TODO: cusotm handling per "action_name"
            destinations += relation.destinations
            errors += relation.errors
            warnings += relation.warnings
        }
    }
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
    val organization_id: String,
    val service: String,
    val filteredReportRows: List<String>?,
    val sending_at: String,
    val itemCount: Int,
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