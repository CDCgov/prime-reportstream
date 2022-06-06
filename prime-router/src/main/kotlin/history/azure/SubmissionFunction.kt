package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.jooq.exception.DataAccessException

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */
class SubmissionFunction(
    val submissionsFacade: SubmissionsFacade = SubmissionsFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine(),
) : ReportFileFunction(
    workflowEngine,
) {
    override fun userOrgName(organization: String): String? {
        return workflowEngine.settings.findSender(organization)?.organizationName
    }

    override fun historyAsJson(request: HttpRequestMessage<String?>, userOrgName: String): String {
        val params = HistoryApiParameters(request.queryParameters)

        return submissionsFacade.findSubmissionsAsJson(
            userOrgName,
            null, // currently, sending org client is not used but the functionality is there
            params.sortDir,
            params.sortColumn,
            params.cursor,
            params.since,
            params.until,
            params.pageSize,
            params.showFailed,
        )
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
        return this.getListByOrg(request, organization)
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
            return if (submission != null)
                HttpUtilities.okJSONResponse(request, submission)
            else
                HttpUtilities.notFoundResponse(request, "Submission $submissionId was not found.")
        } catch (e: DataAccessException) {
            logger.error("Unable to fetch history for submission ID $id", e)
            return HttpUtilities.internalErrorResponse(request)
        } catch (ex: IllegalStateException) {
            logger.error(ex)
            // Errors above are actionId or UUID not found errors.
            return HttpUtilities.notFoundResponse(request, ex.message)
        }
    }
}