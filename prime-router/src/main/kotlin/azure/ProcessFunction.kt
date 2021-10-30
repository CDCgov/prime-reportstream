package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import java.util.logging.Level

const val process = "process"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class ProcessFunction {
    @FunctionName(process)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = process)
        message: String,
        context: ExecutionContext,
    ) {
        try {
            context.logger.info("Process message: $message")
            val workflowEngine = WorkflowEngine()
            val event = Event.parseQueueMessage(message) as ProcessEvent
            if (event.eventAction != Event.EventAction.PROCESS) {
                context.logger.warning("Process function received a $message")
                return
            }

            val actionHistory = ActionHistory(event.eventAction.toTaskAction(), context)
            actionHistory.trackActionParams(message)
            actionHistory.trackExistingInputReport(event.reportId)

            workflowEngine.handleProcessEvent(event, context, actionHistory)
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "Process function exception for event: $message", e)
        }
    }
}