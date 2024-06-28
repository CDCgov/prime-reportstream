package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import org.apache.logging.log4j.kotlin.Logging

private const val azureQueueName = "process"
private const val azureFunctionName = "process"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class ProcessFunction : Logging {
    @FunctionName(azureFunctionName)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = azureQueueName)
        message: String,
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.info("Process message: $message")

        var workflowEngine: WorkflowEngine? = null
        var event: ProcessEvent? = null
        var actionHistory: ActionHistory? = null

        try {
            workflowEngine = WorkflowEngine()
            event = Event.parseQueueMessage(message) as ProcessEvent

            if (event.eventAction != Event.EventAction.PROCESS) {
                logger.error("Process function received a message of the incorrect type: $message")
                return
            }

            actionHistory = ActionHistory(event.eventAction.toTaskAction())
            actionHistory.trackActionParams(message)

            workflowEngine.handleProcessEvent(event, actionHistory)
        } catch (e: Exception) {
            // there is not much tracking we can do unless these three items are not null.
            //  workflowEngine.handleProcessEvent has the highest chance of an error, so mostly likely
            //  these three will be populated
            logger.error("Process function exception for event: $message", e)
            if (workflowEngine != null && event != null && actionHistory != null) {
                workflowEngine.handleProcessFailure(dequeueCount, actionHistory, message)
            }

            // we want to throw - it re-adds the process message to the queue and it will get re-processed by this func
            throw e
        }
    }
}