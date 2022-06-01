package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

// for readability only
typealias SubmissionResultFilter = SubmissionAccess.SubmissionResultFilter

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */

class SubmissionFunction(
    private val submissionsFacade: SubmissionsFacade = SubmissionsFacade.instance,
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
) : Logging {

    data class Parameters(
        val sort: String,
        val sortColumn: String,
        val cursor: OffsetDateTime?,
        val endCursor: OffsetDateTime?,
        val pageSize: Int,
        val filterResult: SubmissionResultFilter,
    ) {
        constructor(query: Map<String, String>) : this(
            extractSortOrder(query),
            extractSortCol(query),
            extractCursor(query, "cursor"),
            extractCursor(query, "endcursor"),
            extractPageSize(query),
            extractResultFilter(query)
        )

        companion object ParamsParser { // ParamsParser name is for unit tests
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

            fun extractResultFilter(query: Map<String, String>): SubmissionResultFilter {
                val filterparam = query["resultfilter"]
                val oldshowfailed = query["showfailed"]
                return if (!filterparam.isNullOrBlank()) {
                    // map param back to enum
                    SubmissionResultFilter.fromCgiParam(filterparam)
                } else if (!oldshowfailed.isNullOrBlank()) {
                    when (oldshowfailed) {
                        "false" -> SubmissionResultFilter.ALL
                        else -> SubmissionResultFilter.ONLY_SUCCESSFUL
                    }
                } else {
                    // both params missing default to something reasonable
                    SubmissionResultFilter.ONLY_SUCCESSFUL
                }
            }
        }
    }

    /**
     * This endpoint is meant for use by either an Admin or a User.
     * It does not assume the user belongs to a single Organization.  Rather, it uses
     * the organization in the URL path, after first confirming authorization to access that organization.
     */
    @FunctionName("getOrgSubmissionsList")
    fun getOrgSubmissionsList(
        @HttpTrigger(
            name = "getOrgSubmissionsList",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/org/{organization}/submissions"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val claims = AuthenticationStrategy.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // Confirm the org name in the path is a sender in the system.
            // exception: if prime admin and organization is "*" (ALL ORGS)
            val organizationName = when (claims.isPrimeAdmin && organization == "*") {
                true -> "*" // means "all orgs"
                else ->
                    // err if no default sender in settings in org
                    workflowEngine.settings.findSender(organization)?.organizationName
                        ?: return HttpUtilities.notFoundResponse(
                            request,
                            "$organization: unknown ReportStream sender"
                        )
            }

            // Do authorization based on: org name in the path == org name in claim.  Or be a prime admin.
            if ((claims.organizationNameClaim != organizationName) && !claims.isPrimeAdmin) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claim org is ${claims.organizationNameClaim} but client id is $organizationName"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.organizationNameClaim}" +
                    " to sender/submissions endpoint via client id $organizationName. "
            )

            val (qSortOrder, qSortColumn, resultsAfterDate, resultsBeforeDate, pageSize, filterResult) =
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
                organizationName,
                sortOrder,
                sortColumn,
                resultsAfterDate,
                resultsBeforeDate,
                pageSize,
                filterResult
            )
            return HttpUtilities.okResponse(request, submissions)
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
        }
    }

    /**
     * API endpoint to return history of a single report.
     * The [id] can be a valid UUID or a valid actionId (aka submissionId, to our users)
     */
    @FunctionName("getReportDetailedHistory")
    fun getReportDetailedHistory(
        @HttpTrigger(
            name = "getReportDetailedHistory",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{id}/history"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val claims = AuthenticationStrategy.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
            logger.info("Authenticated request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")

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

            // Do Authorization.  Confirm these claims allow access to this Action
            if (!submissionsFacade.checkSenderAccessAuthorization(action, claims)) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by ${claims.organizationNameClaim} to read ${action.sendingOrg}/submissions"
            )

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