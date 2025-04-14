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
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.HL7MessageHelpers
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.Configuration
import java.time.OffsetDateTime

/**
 * Batch will find all the reports waiting to with a next "universal_batch" action for a receiver name.
 * It will either send the reports directly or merge them together.
 *
 * A [workflowEngine] can be passed in for mocking/testing purposes.
 */
class UniversalBatchFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) : Logging {
    @FunctionName(BatchConstants.Function.UNIVERSAL_BATCH_FUNCTION)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "message", queueName = BatchConstants.Queue.UNIVERSAL_BATCH_QUEUE)
        message: String,
        @Suppress("UNUSED_PARAMETER")
        context: ExecutionContext?,
    ) {
        try {
            logger.trace("UniversalBatchFunction starting.  Message: $message")
            val event = Event.parsePrimeRouterQueueMessage(message) as BatchEvent
            if (event.eventAction != Event.EventAction.BATCH) {
                logger.error("UniversalBatchFunction received a $message")
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
            logger.trace("UniversalBatchFunction (msg=$message) using backstopTime=$backstopTime")

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

                    batchUniversalData(validHeaders, actionHistory, receiver, txn)

                    workflowEngine.recordAction(actionHistory, txn) // save to db
                }
            }
            actionHistory.queueMessages(workflowEngine) // Must be done after txn, to avoid race condition
            logger.trace("UniversalBatchFunction succeeded for message: $message")
        } catch (e: Exception) {
            logger.error("UniversalBatchFunction Exception (msg=$message, backstopTime=$backstopTime)", e)
            throw e
        }
    }

    /**
     * Process a batch request from the Universal Pipeline for a given set of [validHeaders] and [receiver]. Use
     * [actionHistory] and [txn]
     */
    internal fun batchUniversalData(
        validHeaders: List<WorkflowEngine.Header>,
        actionHistory: ActionHistory,
        receiver: Receiver,
        txn: Configuration?,
    ) {
        if (!receiver.useBatching ||
            receiver.timing == null ||
            receiver.timing.operation != Receiver.BatchOperation.MERGE
        ) {
            // Send each report separately
            validHeaders.forEach {
                // track reportId as 'parent'
                actionHistory.trackExistingInputReport(it.task.reportId)

                // get a Report from the message
                val (report, sendEvent, blobInfo) = Report.generateReportAndUploadBlob(
                    Event.EventAction.SEND,
                    it.content!!,
                    listOf(it.task.reportId),
                    receiver,
                    workflowEngine.metadata,
                    actionHistory,
                    topic = receiver.topic,
                )

                // insert the 'Send' task
                workflowEngine.db.insertTask(
                    report,
                    blobInfo.format.toString(),
                    blobInfo.blobUrl,
                    sendEvent,
                    txn
                )
            }
        } else if (validHeaders.isNotEmpty() ||
            (receiver.timing.whenEmpty.action == Receiver.EmptyOperation.SEND)
        ) {
            // Batch all reports into one
            val messages = validHeaders.map {
                // track reportId as 'parent'
                actionHistory.trackExistingInputReport(it.task.reportId)

                // return message as string
                String(it.content!!)
            }

            // Generate the batch message
            val batchMessage = when (receiver.format) {
                MimeFormat.HL7, MimeFormat.HL7_BATCH -> HL7MessageHelpers.batchMessages(messages, receiver)
                MimeFormat.FHIR -> FHIRBundleHelpers.batchMessages(messages)
                else -> throw IllegalStateException("Unsupported receiver format ${receiver.format} found during batch")
            }

            // get a Report from the message
            val (report, sendEvent, blobInfo) = Report.generateReportAndUploadBlob(
                Event.EventAction.SEND,
                batchMessage.toByteArray(),
                validHeaders.map { it.task.reportId },
                receiver,
                workflowEngine.metadata,
                actionHistory,
                topic = receiver.topic,
            )

            // insert the 'Send' task
            workflowEngine.db.insertTask(
                report,
                blobInfo.format.toString(),
                blobInfo.blobUrl,
                sendEvent,
                txn
            )
        }
    }
}