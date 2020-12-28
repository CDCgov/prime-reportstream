package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import java.util.logging.Level

const val batch = "batch"
const val defaultBatchSize = 100

/**
 * Batch will find all the reports waiting to with a next "batch" action for a receiver name.
 * It will either send the reports directly or merge them together.
 */
class BatchFunction {
    @FunctionName(batch)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = batch)
        message: String,
        context: ExecutionContext,
    ) {
        try {
            context.logger.info("Batch message: $message")
            val workflowEngine = WorkflowEngine()
            val event = Event.parse(message) as ReceiverEvent
            val receiver = workflowEngine.metadata.findService(event.receiverName)
                ?: error("Internal Error: receiver name ${event.receiverName}")
            val maxBatchSize = receiver.batch?.maxBatchSize ?: defaultBatchSize

            workflowEngine.handleReceiverEvent(event, maxBatchSize) { headers, txn ->
                if (headers.isEmpty()) {
                    context.logger.info("Batch: empty batch")
                    return@handleReceiverEvent
                } else {
                    context.logger.info("Batch contains ${headers.size} reports")
                }
                val inReports = headers.map { workflowEngine.createReport(it) }
                val outReports = when (receiver.batch?.operation) {
                    OrganizationService.BatchOperation.MERGE -> listOf(Report.merge(inReports))
                    else -> inReports
                }
                outReports.forEach {
                    val outReport = it.copy(destination = receiver)
                    val outEvent = ReportEvent(Event.Action.SEND, outReport.id)
                    workflowEngine.dispatchReport(outEvent, outReport, txn)
                    context.logger.info("Batch: queued to send ${outEvent.toMessage()}")
                }
            }
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "Batch exception", e)
        }
    }
}