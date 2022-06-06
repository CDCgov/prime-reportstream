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
    private val submissionsFacade: SubmissionsFacade = SubmissionsFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine(),
) : ReportFileFunction(
    workflowEngine,
) {
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
            val sender = workflowEngine.settings.findSender(organization) // err if no default sender in settings in org
                ?: return HttpUtilities.notFoundResponse(request, "$organization: unknown ReportStream sender")

            // Do authorization based on: org name in the path == org name in claim.  Or be a prime admin.
            if ((claims.organizationNameClaim != sender.organizationName) && !claims.isPrimeAdmin) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claim org is ${claims.organizationNameClaim} but client id is ${sender.organizationName}"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.organizationNameClaim}" +
                    " to $sender/submissions endpoint via client id ${sender.organizationName}. "
            )

            val params = HistoryApiParameters(request.queryParameters)
            val submissions = submissionsFacade.findSubmissionsAsJson(
                sender.organizationName,
                params.sortDir,
                params.sortColumn,
                params.cursor,
                params.since,
                params.until,
                params.pageSize,
                params.showFailed
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
}