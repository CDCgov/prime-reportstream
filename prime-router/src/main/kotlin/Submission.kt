package gov.cdc.prime.router

import org.jooq.JSONB
import java.time.OffsetDateTime

/**
 * A `Submission` represents one submission of a message from a sender.
 *
 * @param actionId of the submission
 */

/**
 * TODO: see Github Issues #2314 for expected filename field
 */

/**
[{
"id" : "0e47a10e-7d79-40a7-8bd4-8fca458efc1e", // may be null if report Ingest failed. AR
"taskId" : "123490", // this is the actionId.
"timestamp" : "2021-08-22T12:51:47.824560Z",
"filename": <see #2314 >
"sender" : "robust-tester-inc.default", // action.sending_org . action.sending_org_client. 'receive' task only.
"topic" : "covid-19", // report_file.topic 'receive' task only. AR
"itemCount": 10, AR
"http_status" : "201", // action.http_status 'receive' task only. AR
"warningCount" : 0, AR
"errorCount" : 0, AR
},
]
 */

open class Submission(
    val taskId: Long,
    val createdAt: OffsetDateTime,
    val sendingOrg: String,
    val httpStatus: Int,
) {
    constructor(copy: Submission) : this(
        copy.taskId,
        copy.createdAt,
        copy.sendingOrg,
        copy.httpStatus,
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