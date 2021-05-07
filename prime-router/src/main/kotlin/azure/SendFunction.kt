package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.NullTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.transport.ITransport
import gov.cdc.prime.router.transport.NullTransport
import gov.cdc.prime.router.transport.RetryToken
import java.io.Closeable
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import java.util.logging.Level
import kotlin.random.Random

/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
const val SEND = "send"
const val MAX_RETRY_COUNT = 4
const val MAX_DURATION_VALUE = 120L
const val MAX_FILES_PER_SESSION = 100

// index is retryCount, value is in minutes
val retryDuration = mapOf(1 to 1L, 2 to 5L, 3 to 30L, 4 to 60L, 5 to 120L)
// Use this for testing retries:
// val retryDuration = mapOf(1 to 1L, 2 to 1L, 3 to 1L, 4 to 1L, 5 to 1L)

class SendFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) {
    @FunctionName(SEND)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = SEND) message: String,
        context: ExecutionContext,
        @BindingName("Id") messageId: String? = null,
        @BindingName("DequeueCount") dequeueCount: Int? = null,
        @BindingName("NextVisibleTime") nextVisibleTime: Date? = null,
        @BindingName("InsertionTime") insertionTime: Date? = null,
    ) {
        val actionHistory = ActionHistory(TaskAction.send, context)
        actionHistory.trackActionParams(message)
        context.logger.info(
            "Started Send Function: $message, id=$messageId," +
                " dequeueCount=$dequeueCount, " +
                " nextVisibleTime=$nextVisibleTime, insertionTime=$insertionTime"
        )
        try {
            context.logger.info("Started Send Function: $message")
            val event = Event.parseQueueMessage(message) as ReceiverEvent
            if (event.eventAction != Event.EventAction.SEND) {
                context.logger.warning("Send function received an unhandled action: $message")
                return
            }
            actionHistory.trackActionParams(message)
            workflowEngine.handleReceiverEvent(event, MAX_FILES_PER_SESSION) { receiver, headers, _ ->
                val nextRetryTokens = mutableListOf<RetryToken?>()
                val session = getTransport(receiver)?.startSession(receiver)
                session.use {
                    headers.forEach { header ->
                        nextRetryTokens += sendReport(receiver, header, actionHistory, it, context)
                    }
                }
                // Any retryTokens?
                if (nextRetryTokens.find { it != null } != null) {
                    formReceiverResult(nextRetryTokens)
                } else {
                    WorkflowEngine.successfulReceiverResult(headers)
                }
            }
        } catch (t: Throwable) {
            // For debugging and auditing purposes
            val msg = "Send function unrecoverable exception for event: $message"
            context.logger.log(Level.SEVERE, msg, t)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
        } finally {
            // Note this is operating in a different transaction than the one that did the fetch/lock of the report
            context.logger.info("About to save ActionHistory for $message")
            workflowEngine.recordAction(actionHistory)
            context.logger.info("Done saving ActionHistory for $message")
        }
    }

    private fun getTransport(receiver: Receiver): ITransport? {
        return when (receiver.transport) {
            is SFTPTransportType -> workflowEngine.sftpTransport
            is RedoxTransportType -> workflowEngine.redoxTransport
            is BlobStoreTransportType -> workflowEngine.blobStoreTransport
            is NullTransportType -> NullTransport()
            else -> null
        }
    }

    private fun sendReport(
        receiver: Receiver,
        header: WorkflowEngine.Header,
        actionHistory: ActionHistory,
        session: Closeable?,
        context: ExecutionContext
    ): RetryToken? {
        val retryToken = RetryToken.fromJSON(header.task.retryToken?.data())
        val inputReportId = header.reportFile.reportId
        actionHistory.trackExistingInputReport(inputReportId)
        val serviceName = receiver.fullName
        val nextRetryItems = mutableListOf<String>()
        if (receiver.transport == null) {
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(
                "Not sending $inputReportId to $serviceName: No transports defined"
            )
        } else {
            val retryItems = retryToken?.items
            val sentReportId = UUID.randomUUID() // each sent report gets its own UUID
            val nextRetry = getTransport(receiver)?.send(
                header,
                sentReportId,
                retryItems,
                session,
                actionHistory
            )
            if (nextRetry != null) {
                nextRetryItems += nextRetry
            }
        }
        return formRetryToken(
            nextRetryItems, inputReportId, serviceName, retryToken, context, actionHistory
        )
    }

    private fun formRetryToken(
        nextRetryItems: List<String>,
        reportId: ReportId,
        serviceName: String,
        retryToken: RetryToken?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryToken? {
        return if (nextRetryItems.isEmpty()) {
            // All OK
            context.logger.info("Successfully sent report: $reportId to $serviceName")
            null
        } else {
            val nextRetryCount = (retryToken?.retryCount ?: 0) + 1
            if (nextRetryCount >= MAX_RETRY_COUNT) {
                // Stop retrying and just put the task into an error state
                val msg = "All retries failed. Send Error report for: $reportId to $serviceName"
                actionHistory.trackActionResult(msg)
                context.logger.info(msg)
                RetryToken(nextRetryCount, nextRetryItems)
            } else {
                // retry using a back-off strategy
                val nextRetryTime = calculateRetryTime(nextRetryCount)
                val msg = "Send Failed. Will retry sending report: $reportId to $serviceName} at $nextRetryTime"
                context.logger.info(msg)
                actionHistory.trackActionResult(msg)
                RetryToken(nextRetryCount, nextRetryItems)
            }
        }
    }

    private fun formReceiverResult(nextRetryTokens: List<RetryToken?>): WorkflowEngine.ReceiverResult {
        val retryCount = nextRetryTokens.maxOf { it?.retryCount ?: 0 }
        val nextActionAt = calculateRetryTime(retryCount)
        val nextAction = if (retryCount >= MAX_RETRY_COUNT)
            Event.EventAction.SEND_ERROR
        else
            Event.EventAction.SEND
        return WorkflowEngine.ReceiverResult(
            nextRetryTokens,
            retryAction = nextAction,
            retryActionAt = nextActionAt
        )
    }

    private fun calculateRetryTime(nextRetryCount: Int): OffsetDateTime {
        val waitMinutes = retryDuration.getOrDefault(nextRetryCount, MAX_DURATION_VALUE)
        val randomSeconds = Random.nextLong(-30, 31)
        return OffsetDateTime.now().plusMinutes(waitMinutes).plusSeconds(randomSeconds)
    }
}