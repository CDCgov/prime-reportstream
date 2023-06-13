package gov.cdc.prime.router

import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction

/**
 * Receiver for validation requests with a specific topic, contains all logic to parse and validate
 * a topic'd submission - without actually sending the file or saving anything to the database
 */
class ValidationReceiver(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.receive)
) {

    fun validateAndRoute(
        sender: Sender,
        content: String,
        defaults: Map<String, String>,
        routeTo: List<String>,
        allowDuplicates: Boolean,
    ): List<Translator.RoutedReport> {
        // parse, check for parse errors
        // todo: if we want this to work for UP validation, we will need to do some other changes since this
        //  uses the legacy parser, not the UP parser

        val (report, actionLogs) = workflowEngine.parseTopicReport(sender as LegacyPipelineSender, content, defaults)

        // prevent duplicates if configured to not allow them
        if (!allowDuplicates) {
            SubmissionReceiver.doDuplicateDetection(
                workflowEngine,
                report,
                actionLogs
            )
        }

        if (actionLogs.hasErrors()) {
            throw actionLogs.exception
        }

        val (_, emptyReports, preparedReports) =
            workflowEngine.translateAndRouteReport(report, defaults, routeTo)

        actionHistory.trackLogs(actionLogs.logs)

        return preparedReports.plus(emptyReports)
    }
}