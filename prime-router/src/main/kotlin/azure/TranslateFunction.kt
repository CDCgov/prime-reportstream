package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Report
import org.jooq.Configuration
import java.util.logging.Level

/**
 * Translate will take a report and filter and transform it to the appropriate services in our list
 */
const val translate = "translate"

class TranslateFunction {
    @FunctionName(translate)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = translate)
        message: String,
        context: ExecutionContext,
    ) {
        try {
            context.logger.info("Translate message: $message")
            val workflowEngine = WorkflowEngine()

            val event = Event.parse(message) as ReportEvent
            workflowEngine.handleReportEvent(event) { header, txn ->
                val parentReport = workflowEngine.createReport(header)
                val sendingCount = routeReport(parentReport, workflowEngine, txn, context)
                // TODO: Do we need tracking for reports that are not delivered? 
                if (sendingCount < parentReport.itemCount) {
                    context.logger.warning(
                        "Translated report ${parentReport.id} but dropped " +
                            "${parentReport.itemCount - sendingCount} items"
                    )
                } else {
                    context.logger.info("Translated report: ${parentReport.id} with ${parentReport.itemCount} items")
                }
                // TODO: Next action should be WIPE when implemented
                return@handleReportEvent ReportEvent(Event.Action.NONE, header.task.reportId)
            }
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "Translate exception", e)
        }
    }

    private fun routeReport(
        parentReport: Report,
        workflowEngine: WorkflowEngine,
        txn: Configuration,
        context: ExecutionContext,
    ): Int {
        return workflowEngine
            .translator
            .filterAndTranslateByService(parentReport)
            .map { (report, service) ->
                val event = if (service.batch == null) {
                    ReportEvent(Event.Action.SEND, report.id)
                } else {
                    ReceiverEvent(Event.Action.BATCH, service.fullName, service.batch.nextBatchTime())
                }
                workflowEngine.dispatchReport(event, report, txn)
                context.logger.info("Queued: ${event.toMessage()}")
                report.itemCount
            }.sum()
    }
}