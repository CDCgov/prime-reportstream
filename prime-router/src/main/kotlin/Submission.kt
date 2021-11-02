package gov.cdc.prime.router

import java.time.OffsetDateTime

/**
 * A `Submission` represents one submission of a message from a sender.
 *
 * @param actionId of the submission
 */

/**
 * TODO: see Github Issues #2314 for expected filename field
 */

open class Submission(
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
    constructor(copy: Submission) : this(
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