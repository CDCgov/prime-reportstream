package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import org.apache.logging.log4j.kotlin.logger
import java.time.OffsetDateTime

const val batch = "batch"
const val defaultBatchSize = 100

/**
 * Min number of times to retry a failed batching operation.
 */
const val NUM_BATCH_RETRIES = 2

/**
 * Batch will find all the reports waiting to with a next "batch" action for a receiver name.
 * It will either send the reports directly or merge them together.  A [workflowEngine] can be passed in for
 * mocking/testing purposes.
 */
class BatchFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) : Logging {
    @FunctionName(batch)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = batch)
        message: String,
        @Suppress("UNUSED_PARAMETER")
        context: ExecutionContext?,
    ) {
        var backstopTime: OffsetDateTime? = null
        try {
            logger.trace("BatchFunction starting.  Message: $message")
            val event = Event.parseQueueMessage(message) as BatchEvent
            if (event.eventAction != Event.EventAction.BATCH) {
                logger.error("BatchFunction received a $message")
                return
            }
            val receiver = workflowEngine.settings.findReceiver(event.receiverName)
                ?: error("Internal Error: receiver name ${event.receiverName}")
            val maxBatchSize = receiver.timing?.maxReportCount ?: defaultBatchSize
            val actionHistory = ActionHistory(
                event.eventAction.toTaskAction(),
                event.isEmptyBatch
            )
            actionHistory.trackActionParams(message)
            backstopTime = OffsetDateTime.now().minusMinutes(
                BaseEngine.getBatchLookbackMins(
                    receiver.timing?.numberPerDay ?: 1, NUM_BATCH_RETRIES
                )
            )
            logger.trace("BatchFunction (msg=$message) using backstopTime=$backstopTime")

            // if this 'batch' event is for an empty batch, create the empty file
            if (event.isEmptyBatch) {

                // There is a potential use case where a receiver could be using HL7 passthrough. There is no agreed-
                //  upon format for what an 'empty' HL7 file looks like, and there are no receivers with this type
                //  in prod as of now (2/2/2022). This short circuit is in case one somehow gets put in in the future
                //  to prevent the application from hard crashing.
                if (receiver.format == Report.Format.HL7) {
                    logger.error(
                        "'Empty Batch' not supported for individual HL7 file. Only CSV/HL7_BATCH " +
                            "formats are supported."
                    )
                } else {
                    workflowEngine.generateEmptyReport(
                        actionHistory,
                        receiver
                    )
                    workflowEngine.recordAction(actionHistory)
                }
            } else {
                workflowEngine.handleBatchEvent(event, maxBatchSize, backstopTime) { headers, txn ->
                    // find any headers that expected to have content but were unable to actually download
                    //  from the blob store.
                    headers.filter { it.expectingContent && it.content == null }
                        .forEach {
                            // TODO: Need to add Action with error state of batch_error. See ticket #3642
                            logger.error(
                                "Failure to download ${it.task.bodyUrl} from blobstore. ReportId: ${it.task.reportId}"
                            )
                        }

                    // get a list of valid headers to process.
                    val validHeaders = if (event.isEmptyBatch) headers else headers.filter { it.content != null }

                    if (validHeaders.isEmpty()) {
                        logger.info("Batch $message: empty batch")
                        return@handleBatchEvent
                    } else {
                        logger.info("Batch $message contains ${validHeaders.size} reports")
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
                        val outEvent = ReportEvent(
                            Event.EventAction.SEND,
                            outReport.id,
                            actionHistory.generatingEmptyReport
                        )
                        workflowEngine.dispatchReport(outEvent, outReport, actionHistory, receiver, txn)
                    }
                    val msg = if (inReports.size == 1 && outReports.size == 1) "Success: No merging needed - batch of 1"
                    else "Success: merged ${inReports.size} reports into ${outReports.size} reports"
                    actionHistory.trackActionResult(msg)
                    workflowEngine.recordAction(actionHistory, txn) // save to db
                }
            }
            actionHistory.queueMessages(workflowEngine) // Must be done after txn, to avoid race condition
            logger.trace("BatchFunction succeeded for message: $message")
        } catch (e: Exception) {
            logger.error(
                "BatchFunction Exception (msg=$message, backstopTime=$backstopTime) : " + e.stackTraceToString()
            )
        }
    }
}