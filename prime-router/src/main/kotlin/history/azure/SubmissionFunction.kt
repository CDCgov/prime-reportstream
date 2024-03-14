package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.transport.RESTTransport
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

/**
 * Submissions API
 * Returns a list of Actions from `public.action`. combined with `public.report_file`.
 *
 * @property reportFileFacade Facade class containing business logic to handle the data.
 * @property workflowEngine Container for helpers and accessors used when dealing with the workflow.
 */
class SubmissionFunction(
    val submissionsFacade: SubmissionsFacade = SubmissionsFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine(),
) : ReportFileFunction(
    submissionsFacade,
    workflowEngine
) {
    /**
     * We store the service name here to pass to the facade
     */
    var sendingOrgSvc: String? = null

    /**
     * Verify the correct name for an organization based on the name.
     *
     * @param organization Name of organization and optionally a sender in the format {orgName}.{sender}
     * @return Name for the organization
     */
    override fun validateOrgSvcName(organization: String): String? {
        return workflowEngine.settings.findSender(organization).also {
            if (organization.contains(Sender.fullNameSeparator)) sendingOrgSvc = it?.name
        }?.organizationName
    }

    /**
     * Verify that the action being checked has the correct data/parameters
     * for the type of report being viewed.
     *
     * @param action DB Action that we are reviewing
     * @return true if action is valid, else false
     */
    override fun actionIsValid(action: Action): Boolean {
        return action.sendingOrg != null && action.actionName == TaskAction.receive
    }

    /**
     * Get a list of submission history
     *
     * @param queryParams Parameters extracted from the HTTP Request
     * @param userOrgName Name of the organization
     * @return json list of submissions
     */
    override fun historyAsJson(queryParams: MutableMap<String, String>, userOrgName: String): String {
        val params = HistoryApiParameters(queryParams)

        return submissionsFacade.findSubmissionsAsJson(
            userOrgName,
            sendingOrgSvc,
            params.sortDir,
            params.sortColumn,
            params.cursor,
            params.since,
            params.until,
            params.pageSize,
            params.showFailed
        )
    }

    /**
     * Get expanded details for a single report
     *
     * @param queryParams Parameters extracted from the HTTP Request
     * @param action Action from which the data for the submission is loaded
     * @return
     */
    override fun singleDetailedHistory(
        queryParams: MutableMap<String, String>,
        action: Action,
    ): DetailedSubmissionHistory? {
        return submissionsFacade.findDetailedSubmissionHistory(action)
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
        return this.getDetailedView(request, id)
    }

    /**
     * API endpoint to return history of a single report.
     * The [id] can be a valid UUID or a valid actionId (aka submissionId, to our users)
     */
    @FunctionName("getTiMetadata")
    fun getTiMetadata(
        @HttpTrigger(
            name = "getTiMetadata",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{id}/tiMetadata"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: String,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var response: HttpResponse? = null

        val receiver = workflowEngine.settings.findReceiver("flexion.etor-service-receiver-orders")
        val client = HttpClient()
        val restTransport = RESTTransport(client)
        val restTransportInfo = receiver?.transport as RESTTransportType
        val (credential, jksCredential) = restTransport.getCredential(restTransportInfo, receiver)
        val logger: Logger = context.logger
        var httpHeaders: Map<String, String>? = null
        var bearerToken: io.ktor.client.plugins.auth.providers.BearerTokens? = null
        var authPair: Pair<Map<String, String>?, io.ktor.client.plugins.auth.providers.BearerTokens?> =
            Pair(httpHeaders, bearerToken)
        // Was wondering if having 2 suspend functions in the same blocking section of code was causing the issue.  It was not

        //
        runBlocking {
            launch {
                authPair = restTransport.getOAuthToken(
                    restTransportInfo,
                    id,
                    jksCredential,
                    credential,
                    logger
                )
            }
        }

        runBlocking {
            launch {
                // val reportClient = httpClient ?: RESTTransport.createDefaultHttpClient(jksCredential, bearerTokens)

                response = client.get("http://host.docker.internal:8080/v1/etor/metadata/" + id) {
                    authPair.first?.forEach {
                        entry ->
                        headers.append(entry.key, entry.value)
                    }
                }
            }
        }
//
//        val client  = HttpClient()
//
//        val response = client.get("http://localhost:8080/v1/etor/metadata/"+id)
//        logger.info(response)
        return HttpUtilities.okJSONResponse(request, response.toString())
    }
}