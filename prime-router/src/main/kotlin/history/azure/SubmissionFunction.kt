package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.db.ReportGraph
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

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
    override fun validateOrgSvcName(
        organization: String,
    ): String? = workflowEngine.settings.findSender(organization).also {
            if (organization.contains(Sender.fullNameSeparator)) sendingOrgSvc = it?.name
        }?.organizationName

    /**
     * Verify that the action being checked has the correct data/parameters
     * for the type of report being viewed.
     *
     * @param action DB Action that we are reviewing
     * @return true if action is valid, else false
     */
    override fun actionIsValid(
        action: Action,
    ): Boolean = action.sendingOrg != null && action.actionName == TaskAction.receive

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
        id: String,
        txn: DataAccessTransaction,
        action: Action,
    ): DetailedSubmissionHistory? {
        val report = try {
            val reportId = UUID.fromString(id)
            submissionsFacade.findDetailedSubmissionHistory(txn, reportId, action)
        } catch (ex: IllegalArgumentException) {
            // We cannot consistently use this logic because the covid pipeline can process reports
            // synchronously such that the intial action has multiple reports associated (i.e. receive and send)
            val report = submissionsFacade.fetchReportForActionId(action.actionId, txn)
            submissionsFacade.findDetailedSubmissionHistory(txn, report?.reportId, action)
        }
        return report
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
    ): HttpResponseMessage = this.getListByOrg(request, organization)

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
    ): HttpResponseMessage = this.getDetailedView(request, id)

    /**
     * Endpoint for intermediary senders to verify status of messages. It passes
     * a null engine to the retrieveMetadata function because Azure gets upset if there
     * are any non-annotated parameters in the method signature other than ExecutionContext
     * and we needed the engine to be a parameter so it can be mocked for tests
     *
     */
    @FunctionName("getEtorMetadataForHistory")
    fun getEtorMetadata(
        @HttpTrigger(
            name = "getEtorMetadataForHistory",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{reportId}/history/etorMetadata"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: UUID,
        context: ExecutionContext,
    ): HttpResponseMessage = this.retrieveETORIntermediaryMetadata(request, reportId, context, null)

    /**
     * Function for finding the associated report ID that the intermediary knows about given the report ID that the sender is
     * given from report stream
     */
    override fun getLookupId(reportId: UUID): UUID? {
        val reportGraph = ReportGraph(workflowEngine.db)
        var descendants: List<ReportFile> = emptyList()
        workflowEngine.db.transactReturning { txn ->
            // looking for the descendant batch report because RS sends the batch report ID, not the send report ID, to ReST receivers
            descendants = reportGraph.getDescendantReports(txn, reportId, setOf(TaskAction.batch))
        }

        return descendants.stream().filter {
            it.receivingOrg == "flexion"
        }.findFirst().getOrNull()?.reportId
    }
}