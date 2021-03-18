package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.NullTransportType
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.transport.ITransport
import gov.cdc.prime.router.transport.NullTransport
import gov.cdc.prime.router.transport.RetryToken
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
const val dataRetentionDays = 7L
const val send = "send"
const val maxRetryCount = 4
const val maxDurationValue = 120L

// index is retryCount, value is in minutes
val retryDuration = mapOf(1 to 1L, 2 to 5L, 3 to 30L, 4 to 60L, 5 to 120L)

class SendFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) {
    @FunctionName(send)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = send)
        message: String,
        context: ExecutionContext,
    ) {
        val actionHistory = ActionHistory(TaskAction.send, context)
        actionHistory.trackActionParams(message)
        try {
            context.logger.info("Started Send Function: $message")
            val event = Event.parseQueueMessage(message) as ReportEvent
            if (event.eventAction != Event.EventAction.SEND) {
                context.logger.warning("Send function received a $message")
                return
            }
            workflowEngine.handleReportEvent(event) { header, retryToken, _ ->
                val receiver = header.receiver
                    ?: error("Internal Error: could not find ${header.task.receiverName}")
                val inputReportId = header.reportFile.reportId
                actionHistory.trackExistingInputReport(inputReportId)
                val serviceName = receiver.fullName
                val nextRetryItems = mutableListOf<String>()
                if (receiver.transport == null) {
                    actionHistory.setActionType(TaskAction.send_error)
                    actionHistory.trackActionResult("Not sending $inputReportId to $serviceName: No transports defined")
                } else {
                    val retryItems = retryToken?.items
                    val sentReportId = UUID.randomUUID() // each sent report gets its own UUID
                    val nextRetry = getTransport(receiver.transport)?.send(
                        receiver.transport,
                        header,
                        sentReportId,
                        retryItems,
                        context,
                        actionHistory,
                    )
                    if (nextRetry != null) {
                        nextRetryItems += nextRetry
                    }
                }
                handleRetry(nextRetryItems, inputReportId, serviceName, retryToken, context, actionHistory)
            }
        } catch (t: Throwable) {
            // For debugging and auditing purposes
            val msg = "Send function exception for event: $message"
            context.logger.log(Level.SEVERE, msg, t)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
        } finally {
            // Note this is operating in a different transaction than the one that did the fetch/lock of the report
            workflowEngine.recordAction(actionHistory)
        }
    }

    private fun getTransport(transportType: TransportType): ITransport? {
        return when (transportType) {
            is SFTPTransportType -> workflowEngine.sftpTransport
            is RedoxTransportType -> workflowEngine.redoxTransport
            is NullTransportType -> NullTransport()
            else -> null
        }
    }

    private fun handleRetry(
        nextRetryItems: List<String>,
        reportId: ReportId,
        serviceName: String,
        retryToken: RetryToken?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): ReportEvent {
        return if (nextRetryItems.isEmpty()) {
            // All OK
            context.logger.info("Successfully sent report: $reportId to $serviceName")
            // TODO: Next action should be WIPE when implemented
            ReportEvent(Event.EventAction.NONE, reportId)
        } else {
            val nextRetryCount = (retryToken?.retryCount ?: 0) + 1
            if (nextRetryCount >= maxRetryCount) {
                // Stop retrying and just put the task into an error state
                val msg = "All retries failed.  Send Error report for: $reportId to $serviceName"
                actionHistory.trackActionResult(msg)
                context.logger.info(msg)
                ReportEvent(Event.EventAction.SEND_ERROR, reportId)
            } else {
                // retry using a back-off strategy
                val waitMinutes = retryDuration.getOrDefault(nextRetryCount, maxDurationValue)
                val nextRetryTime = OffsetDateTime.now().plusMinutes(waitMinutes)
                val nextRetryToken = RetryToken(nextRetryCount, nextRetryItems)
                val msg = "Send Failed.  Will retry sending report: $reportId to $serviceName}" +
                    " in $waitMinutes minutes, at $nextRetryTime"
                context.logger.info(msg)
                actionHistory.trackActionResult(msg)
                ReportEvent(Event.EventAction.SEND, reportId, nextRetryTime, nextRetryToken)
            }
        }
    }
}