package gov.cdc.prime.router

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

/*
 * TODO: see Github Issues #2314 for expected filename field
 */

open class SubmissionHistory(
    val taskId: Long,
    val createdAt: OffsetDateTime,
    val sendingOrg: String,
    val httpStatus: Int,
    val id: String?,
    val topic: String?,
    val reportItemCount: Int?,
    val warningCount: Int?,
    val errorCount: Int?,
) {
    constructor(copy: SubmissionHistory) : this(
        copy.taskId,
        copy.createdAt,
        copy.sendingOrg,
        copy.httpStatus,
        copy.id,
        copy.topic,
        copy.reportItemCount,
        copy.warningCount,
        copy.errorCount,

    )
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

open class ActionResponse(
    val id: String?,
    val topic: String?,
    val reportItemCount: Int?,
    val warningCount: Int?,
    val errorCount: Int?,
) {
    constructor(copy: ActionResponse) : this(
        copy.id,
        copy.topic,
        copy.reportItemCount,
        copy.warningCount,
        copy.errorCount,
    )
}