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

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */

class SubmissionFunction(
    submissionsFacade: SubmissionsFacade = SubmissionsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.USER)
) : Logging {
    private val facade = submissionsFacade
    private val oktaAuthentication = oktaAuthentication

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
                val submissions = facade.findSubmissionsAsJson(
                    organization,
                    qSortOrder,
                    qSortColumn,
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
        return oktaAuthentication.checkAccess(request, oktaSender = true) { claims ->
            try {
                var submissionId = id.toLongOrNull() // toLong a sacrifice can make a Null of the heart
                val action = if (submissionId == null) {
                    val reportId = HttpUtilities.toUuidOrNull(id) ?: error("Bad format: $id must be a num or a UUID")
                    facade.fetchActionForReportId(reportId) ?: error("No such reportId: $reportId")
                } else {
                    facade.fetchAction(submissionId) ?: error("No such submissionId $submissionId")
                }

                // Confirm this is actually a submission.
                if (action.sendingOrg == null || action.actionName != TaskAction.receive) {
                    HttpUtilities.notFoundResponse(request, "$id is not a submitted report")
                }

                // Confirm these claims allow access to this Action
                if (!facade.checkActionAccessAuthorization(action, claims)) {
                    error("Access to $id denied")
                }
                val submission = facade.findDetailedSubmissionHistory(action.sendingOrg, action.actionId)
                if (submission != null) HttpUtilities.okJSONResponse(request, submission)
                else HttpUtilities.notFoundResponse(request, "Submission $submissionId was not found.")
            } catch (e: DataAccessException) {
                logger.error("Unable to fetch history for submission ID $id", e)
                HttpUtilities.internalErrorResponse(request)
            } catch (ex: IllegalStateException) {
                logger.error(ex.message ?: "IllegalStateException", ex)
                HttpUtilities.notFoundResponse(request, ex.message)
            }
        }
    }
}