package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */

class SubmissionFunction(
    private val submissionsFacade: SubmissionsFacade = SubmissionsFacade.instance,
    private val oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.USER)
) : Logging {
    data class Parameters(
        val sort: String,
        val sortColumn: String,
        val cursor: OffsetDateTime?,
        val endCursor: OffsetDateTime?,
        val pageSize: Int,
        val showFailed: Boolean
    ) {
        constructor(query: Map<String, String>) : this (
            extractSortOrder(query),
            extractSortCol(query),
            extractCursor(query, "cursor"),
            extractCursor(query, "endcursor"),
            extractPageSize(query),
            extractShowFailed(query)
        )

        companion object {
            fun extractSortOrder(query: Map<String, String>): String {
                return query.getOrDefault("sort", "DESC")
            }

            fun extractSortCol(query: Map<String, String>): String {
                return query.getOrDefault("sortcol", "default")
            }

            fun extractCursor(query: Map<String, String>, name: String): OffsetDateTime? {
                val cursor = query.get(name)
                return if (cursor != null) {
                    try {
                        OffsetDateTime.parse(cursor)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("\"$name\" must be a valid datetime")
                    }
                } else null
            }

            fun extractPageSize(query: Map<String, String>): Int {
                val size = query.getOrDefault("pagesize", "10").toIntOrNull()
                require(size != null) { "pageSize must be a positive integer" }
                return size
            }

            fun extractShowFailed(query: Map<String, String>): Boolean {
                return when (query.getOrDefault("showfailed", "true")) {
                    "false" -> false
                    else -> true
                }
            }
        }
    }

    /**
     * This endpoint is meant for use by either an Admin or a User.
     * It does not assume the user belongs to a single Organization.  Rather, it uses
     * the organization in the URL path, after first confirming authorization to access that organization.
     */
    @FunctionName("getOrgSubmissions")
    fun getOrgSubmissions(
        @HttpTrigger(
            name = "getOrgSubmissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/org/{organization}/submissions"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organization, true) {
            try {
                val (qSortOrder, qSortColumn, resultsAfterDate, resultsBeforeDate, pageSize, showFailed) =
                    Parameters(request.queryParameters)
                val sortOrder = try {
                    SubmissionAccess.SortOrder.valueOf(qSortOrder)
                } catch (e: IllegalArgumentException) {
                    SubmissionAccess.SortOrder.DESC
                }

                val sortColumn = try {
                    SubmissionAccess.SortColumn.valueOf(qSortColumn)
                } catch (e: IllegalArgumentException) {
                    SubmissionAccess.SortColumn.CREATED_AT
                }
                val submissions = submissionsFacade.findSubmissionsAsJson(
                    organization,
                    sortOrder,
                    sortColumn,
                    resultsAfterDate,
                    resultsBeforeDate,
                    pageSize,
                    showFailed
                )
                HttpUtilities.okResponse(request, submissions)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            }
        }
    }

    /**
     * API endpoint to return history of a single report.
     * The [id] can be a valid UUID or a valid actionId (aka submissionId, to our users)
     */
    @FunctionName("getReportHistory")
    fun getReportHistory(
        @HttpTrigger(
            name = "getReportHistory",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{id}/history"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: String,
    ): HttpResponseMessage {
        try {
            val claims = OktaAuthentication.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, OktaAuthentication.authenticationFailure)
            logger.info("Authenticated request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")

            // actionHistory?.trackUsername(claims.userName)  todo

            // Figure out whether we're dealing with an action_id or a report_id.
            val submissionId = id.toLongOrNull() // toLong a sacrifice can make a Null of the heart
            val action = if (submissionId == null) {
                val reportId = toUuidOrNull(id) ?: error("Bad format: $id must be a num or a UUID")
                submissionsFacade.fetchActionForReportId(reportId) ?: error("No such reportId: $reportId")
            } else {
                submissionsFacade.fetchAction(submissionId) ?: error("No such submissionId $submissionId")
            }

            // Confirm this is actually a submission.
            if (action.sendingOrg == null || action.actionName != TaskAction.receive) {
                return HttpUtilities.notFoundResponse(request, "$id is not a submitted report")
            }

            // Confirm these claims allow access to this Action
            if (!submissionsFacade.checkSenderAccessAuthorization(action, claims)) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}"
                )
                return HttpUtilities.unauthorizedResponse(request, OktaAuthentication.authorizationFailure)
            }
            val submission = submissionsFacade.findDetailedSubmissionHistory(action.sendingOrg, action.actionId)
            if (submission != null)
                return HttpUtilities.okJSONResponse(request, submission)
            else
                return HttpUtilities.notFoundResponse(request, "Submission $submissionId was not found.")
        } catch (e: DataAccessException) {
            logger.error("Unable to fetch history for submission ID $id", e)
            return HttpUtilities.internalErrorResponse(request)
        } catch (ex: IllegalStateException) {
            logger.error(ex)
            // Errors above are actionId or UUID not found errors.
            return HttpUtilities.notFoundResponse(request, ex.message)
        }
    }

    /**
     * Utility function.  Mimic String.toLongOrNull()
     * @return a valid UUID, or null if this [str] cannot be parsed into a valid UUID.
     */
    fun toUuidOrNull(str: String): UUID? {
        return try {
            UUID.fromString(str)
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid format for report ID: $str", e)
            null
        }
    }
}