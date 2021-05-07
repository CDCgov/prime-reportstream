package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.util.logging.Level

const val BATCH = "batch"
const val DEFAULT_BATCH_SIZE = 100

/**
 * Batch will find all the reports waiting to with a next "batch" action for a receiver name.
 * It will either send the reports directly or merge them together.
 */
class BatchFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) {
    @FunctionName(BATCH)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = BATCH)
        message: String,
        context: ExecutionContext,
    ) {
        context.logger.info("Batch message: $message")
        val event = Event.parseQueueMessage(message) as ReceiverEvent
        if (event.eventAction != Event.EventAction.BATCH) {
            context.logger.warning("Batch function received a $message")
            return
        }
        val actionHistory = ActionHistory(event.eventAction.toTaskAction(), context)
        actionHistory.trackActionParams(message)

        try {
            val receiver = workflowEngine.settings.findReceiver(event.receiverName)
                ?: error("Internal Error: receiver name ${event.receiverName}")
            val maxBatchSize = receiver.timing?.maxReportCount ?: DEFAULT_BATCH_SIZE

            // The send event is the nextEvent
            val sendEvent = ReceiverEvent(Event.EventAction.SEND, receiver.fullName)

            workflowEngine.handleReceiverEvent(event, maxBatchSize) { _, headers, txn ->
                if (headers.isEmpty()) {
                    context.logger.info("Batch: empty batch")
                    return@handleReceiverEvent WorkflowEngine.successfulReceiverResult(headers)
                } else {
                    context.logger.info("Batch contains ${headers.size} reports")
                }
                val inReports = headers.map {
                    val report = workflowEngine.createReport(it)
                    // todo replace the use of Event.Header.Task with info from ReportFile.
                    actionHistory.trackExistingInputReport(it.task.reportId)
                    report
                }
                val mergedReports = when {
                    receiver.format == Report.Format.HL7 -> inReports  // don't merge, when we are about to split
                    receiver.timing?.operation == Receiver.BatchOperation.MERGE -> listOf(Report.merge(inReports))
                    else -> inReports
                }
                val outReports = when (receiver.format) {
                    Report.Format.HL7 -> mergedReports.flatMap { it.split() }
                    else -> mergedReports
                }
                outReports.forEach {
                    val outReport = it.copy(destination = receiver, bodyFormat = receiver.format)
                    workflowEngine.dispatchReport(sendEvent, outReport, actionHistory, receiver, txn, null)
                }
                val msg = if (inReports.size == 1 && outReports.size == 1) "Success: No merging needed - batch of 1"
                else "Success: merged ${inReports.size} reports into ${outReports.size} reports"
                actionHistory.trackActionResult(msg)
                WorkflowEngine.successfulReceiverResult(headers)
            }
            workflowEngine.queue.sendMessage(sendEvent)
        } catch (e: Exception) {
            // For debugging and auditing purposes
            val msg = "Batch function exception for event: $message"
            context.logger.log(Level.SEVERE, msg, e)
            actionHistory.setActionType(TaskAction.batch_error)
            actionHistory.trackActionResult(msg)
        } finally {
            // Note this is operating in a different transaction than the one that did the fetch/lock of the report.
            // This is done to record errors in action history when their main transaction errors
            context.logger.info("About to save ActionHistory for $message")
            workflowEngine.recordAction(actionHistory)
            context.logger.info("Done saving ActionHistory for $message")
        }
    }
}