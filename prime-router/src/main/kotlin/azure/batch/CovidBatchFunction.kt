package gov.cdc.prime.router.azure.batch

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BatchEvent
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ReportEvent
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime

/**
 * Batch will find all the reports waiting to with a next "covid_batch" action for a receiver name.
 * It will either send the reports directly or merge them together.
 *
 * A [workflowEngine] can be passed in for mocking/testing purposes.
 */
class CovidBatchFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) : Logging {
    @FunctionName(BatchConstants.Function.COVID_BATCH_FUNCTION)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = BatchConstants.Queue.COVID_BATCH_QUEUE)
        message: String,
        @Suppress("UNUSED_PARAMETER")
        context: ExecutionContext?,
    ) {
        try {
            logger.trace("CovidBatchFunction starting.  Message: $message")
            val event = Event.parsePrimeRouterQueueMessage(message) as BatchEvent
            if (event.eventAction != Event.EventAction.BATCH) {
                logger.error("CovidBatchFunction received a $message")
                return
            }
            val actionHistory = ActionHistory(
                event.eventAction.toTaskAction(),
                event.isEmptyBatch
            )
            doBatch(message, event, actionHistory)
        } catch (e: Exception) {
            // already logged, silent catch to not break existing functionality
        }
    }

    /**
     * Pulling the functionality out of the azure function so it is individually testable and we can pass in an
     * [actionHistory] from the mocking framework. Does the batching for the [event] passed in; [message] is needed
     * in this function for logging and tracking
     */
    internal fun doBatch(
        message: String,
        event: BatchEvent,
        actionHistory: ActionHistory,
    ) {
        var backstopTime: OffsetDateTime? = null
        try {
            val receiver = workflowEngine.settings.findReceiver(event.receiverName)
                ?: error("Internal Error: receiver name ${event.receiverName}")
            actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)
            val maxBatchSize = receiver.timing?.maxReportCount ?: BatchConstants.DEFAULT_BATCH_SIZE

            actionHistory.trackActionParams(message)
            backstopTime = OffsetDateTime.now().minusMinutes(
                BaseEngine.getBatchLookbackMins(
                    receiver.timing?.numberPerDay ?: 1, BatchConstants.NUM_BATCH_RETRIES
                )
            )
            logger.trace("CovidBatchFunction (msg=$message) using backstopTime=$backstopTime")

            // if this 'batch' event is for an empty batch, create the empty file
            if (event.isEmptyBatch) {
                // There is a potential use case where a receiver could be using HL7 passthrough. There is no agreed-
                //  upon format for what an 'empty' HL7 file looks like, and there are no receivers with this type
                //  in prod as of now (2/2/2022). This short circuit is in case one somehow gets put in in the future
                //  to prevent the application from hard crashing.
                if (receiver.format == MimeFormat.HL7) {
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
                workflowEngine.handleBatchEvent(event, maxBatchSize, backstopTime) { validHeaders, txn ->
                    if (validHeaders.isEmpty()) {
                        logger.info("Batch $message: empty batch")
                        return@handleBatchEvent
                    } else {
                        logger.info("Batch $message contains ${validHeaders.size} reports")
                    }

                    // only batch files that have the expected content - only for reports that are not already HL7
                    val inReports = validHeaders.map {
                        val report = workflowEngine.createReport(it)
                        // todo replace the use of task.reportId with info from ReportFile.
                        actionHistory.trackExistingInputReport(it.task.reportId)
                        report
                    }
                    val mergedReports = when {
                        receiver.format.isSingleItemFormat -> inReports // don't merge, when we are about to split
                        receiver.timing?.operation == Receiver.BatchOperation.MERGE ->
                            listOf(Report.merge(inReports))

                        else -> inReports
                    }
                    val outReports = if (receiver.format.isSingleItemFormat) {
                        mergedReports.flatMap { it.split() }
                    } else {
                        mergedReports
                    }

                    outReports.forEach {
                        val outReport = it.copy(destination = receiver, bodyFormat = receiver.format)
                        val outEvent = ReportEvent(
                            Event.EventAction.SEND,
                            outReport.id,
                            actionHistory.generatingEmptyReport
                        )
                        workflowEngine.dispatchReport(outEvent, outReport, actionHistory, receiver, txn)
                    }
                    val msg = if (inReports.size == 1 && outReports.size == 1) {
                        "Success: " +
                            "No merging needed - batch of 1"
                    } else {
                        "Success: merged ${inReports.size} reports into ${outReports.size} reports"
                    }
                    actionHistory.trackActionResult(msg)

                    workflowEngine.recordAction(actionHistory, txn) // save to db
                }
            }
            actionHistory.queueMessages(workflowEngine) // Must be done after txn, to avoid race condition
            logger.trace("CovidBatchFunction succeeded for message: $message")
        } catch (e: Exception) {
            logger.error(
                "CovidBatchFunction Exception (msg=$message, backstopTime=$backstopTime) : " + e.stackTraceToString()
            )
            throw e
        }
    }
}