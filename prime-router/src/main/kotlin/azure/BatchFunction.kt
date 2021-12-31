package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import org.apache.logging.log4j.kotlin.logger
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
        val backstopTime = DatabaseAccess.getBackstopTime()
        try {
            context.logger.info("BatchFunction starting.  Message: $message")
            val workflowEngine = WorkflowEngine()
            val event = Event.parseQueueMessage(message) as BatchEvent
            if (event.eventAction != Event.EventAction.BATCH) {
                context.logger.warning("BatchFunction received a $message")
                return
            }
            val receiver = workflowEngine.settings.findReceiver(event.receiverName)
                ?: error("Internal Error: receiver name ${event.receiverName}")
            val maxBatchSize = receiver.timing?.maxReportCount ?: defaultBatchSize
            val actionHistory = ActionHistory(event.eventAction.toTaskAction(), context)
            actionHistory.trackActionParams(message)
            context.logger.info(
                "BatchFunction for $message" +
                    " will look for tasks created after $backstopTime, for receiver ${event.receiverName}"
            )
            workflowEngine.handleBatchEvent(event, maxBatchSize, backstopTime) { headers, txn ->
                // find any headers that expected to have content but were unable to actually download
                //  from the blob store.
                headers.filter { it.expectingContent && it.content == null }
                    .forEach {
                        // TODO: Need to add Action with error state of batch_error. See ticket #3642
                        context.logger.severe(
                            "Failure to download ${it.task.bodyUrl} from blobstore. ReportId: ${it.task.reportId}"
                        )
                    }

                // get a list of valid headers to process
                val validHeaders = headers.filter { it.content != null }

                if (validHeaders.isEmpty()) {
                    context.logger.info("Batch $message: empty batch")
                    return@handleBatchEvent
                } else {
                    context.logger.info("Batch $message contains ${validHeaders.size} reports")
                }

                // only batch files that have the expected content.
                val inReports = validHeaders.map {
                    val report = workflowEngine.createReport(it)
                    // todo replace the use of task.reportId with info from ReportFile.
                    actionHistory.trackExistingInputReport(it.task.reportId)
                    report
                }
                val mergedReports = when {
                    receiver.format.isSingleItemFormat -> inReports // don't merge, when we are about to split
                    receiver.timing?.operation == Receiver.BatchOperation.MERGE -> listOf(Report.merge(inReports))
                    else -> inReports
                }
                val outReports = if (receiver.format.isSingleItemFormat)
                    mergedReports.flatMap { it.split() }
                else
                    mergedReports
                outReports.forEach {
                    val outReport = it.copy(destination = receiver, bodyFormat = receiver.format)
                    val outEvent = ReportEvent(Event.EventAction.SEND, outReport.id)
                    workflowEngine.dispatchReport(outEvent, outReport, actionHistory, receiver, txn, null)
                }
                val msg = if (inReports.size == 1 && outReports.size == 1) "Success: No merging needed - batch of 1"
                else "Success: merged ${inReports.size} reports into ${outReports.size} reports"
                actionHistory.trackActionResult(msg)
                workflowEngine.recordAction(actionHistory, txn) // save to db
            }
            actionHistory.queueMessages(workflowEngine) // Must be done after txn, to avoid race condition
            context.logger.info("BatchFunction succeeded for message: $message")
        } catch (e: Exception) {
            context.logger.log(
                Level.SEVERE,
                "BatchFunction exception for message: $message," +
                    " (while batching tasks created since $backstopTime) : ",
                e
            )
        }
    }
}