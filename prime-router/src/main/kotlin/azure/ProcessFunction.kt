package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import java.util.logging.Level

private const val azureQueueName = "process"
private const val azureFunctionName = "process"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class ProcessFunction {
    @FunctionName(azureFunctionName)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = azureQueueName)
        message: String,
        context: ExecutionContext,
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        context.logger.info("Process message: $message")

        var workflowEngine: WorkflowEngine? = null
        var event: ProcessEvent? = null
        var actionHistory: ActionHistory? = null

        try {
            workflowEngine = WorkflowEngine()
            event = Event.parseQueueMessage(message) as ProcessEvent

            if (event.eventAction != Event.EventAction.PROCESS) {
                context.logger.warning("Process function received a $message")
                return
            }

            actionHistory = ActionHistory(event.eventAction.toTaskAction())
            actionHistory.trackActionParams(message)

            workflowEngine.handleProcessEvent(event, context, actionHistory)
        } catch (e: Exception) {
            // there is not much tracking we can do unless these three items are not null.
            //  workflowEngine.handleProcessEvent has the highest chance of an error, so mostly likely
            //  these three will be populated
            if (workflowEngine != null && event != null && actionHistory != null) {
                workflowEngine.handleProcessFailure(dequeueCount, actionHistory)
            }

            context.logger.log(Level.SEVERE, "Process function exception for event: $message", e)
            // we want to throw - it re-adds the process message to the queue and it will get re-processed by this func
            throw e
        }
    }
}