package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.NullTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.transport.ITransport
import gov.cdc.prime.router.transport.NullTransport
import gov.cdc.prime.router.transport.RetryItems
import gov.cdc.prime.router.transport.RetryToken
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
const val send = "send"
const val maxRetryCount = 10
const val maxDurationValue = 120L
const val maxFilesPerSession = 100

// index is retryCount, value is in minutes
val retryDuration = mapOf(1 to 1L, 2 to 1L, 3 to 8L, 4 to 15L, 5 to 30L)

class SendFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) {
    @FunctionName(send)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = send)
        message: String,
        context: ExecutionContext,
    ) {
        try {
            context.logger.info("Started Send Function: $message")
            val event = Event.parseQueueMessage(message) as ReceiverEvent
            if (event.eventAction != Event.EventAction.SEND) {
                context.logger.warning("Send function received a unhandled: $message")
                return
            }
            val actionHistory = ActionHistory(event.eventAction.toTaskAction(), context)
            actionHistory.trackActionParams(message)
            workflowEngine.handleReceiverEvent(event, maxFilesPerSession, actionHistory) { receiver, headers, txn ->
                val nextRetryTokens = mutableListOf<RetryToken?>()
                val type = receiver.transport
                val session = if (type != null) getTransport(type)?.startSession(receiver) else null
                try {
                    headers.forEach { header ->
                        val retryToken = RetryToken.fromJSON(header.task.retryToken?.data())
                        val inputReportId = header.reportFile.reportId
                        actionHistory.trackExistingInputReport(inputReportId)
                        val serviceName = receiver.fullName
                        val nextRetryItems = mutableListOf<String>()
                        if (receiver.transport == null) {
                            actionHistory.setActionType(TaskAction.send_error)
                            actionHistory.trackActionResult(
                                "Not sending $inputReportId to $serviceName:" +
                                    " No transports defined"
                            )
                        } else {
                            val nextRetry = sendReport(receiver, header, actionHistory, retryToken, session)
                            if (nextRetry != null) {
                                nextRetryItems += nextRetry
                            }
                        }
                        nextRetryTokens += formRetryToken(
                            nextRetryItems, inputReportId, serviceName, retryToken, context, actionHistory
                        )
                    }
                } finally {
                    if (session != null) session.close()
                }
                val retryCount = nextRetryTokens.maxOf { it?.retryCount ?: 0 }
                val retryAt = calculateRetryTime(retryCount)
                val retryAction = if (retryCount >= maxRetryCount)
                    Event.EventAction.SEND_ERROR else Event.EventAction.SEND
                WorkflowEngine.ReceiverResult(
                    nextRetryTokens,
                    retryAction = retryAction,
                    retryActionAt = retryAt
                )
            }
            // For debugging and auditing purposes
        } catch (t: Throwable) {
            context.logger.log(Level.SEVERE, "Send function exception for event: $message", t)
        }
    }

    private fun sendReport(
        receiver: Receiver,
        header: WorkflowEngine.Header,
        actionHistory: ActionHistory,
        retryToken: RetryToken?,
        session: Any?,
    ): RetryItems? {
        val retryItems = retryToken?.items
        val sentReportId = UUID.randomUUID() // each sent report gets its own UUID
        if (receiver.transport == null) return null
        return getTransport(receiver.transport)?.send(
            header,
            sentReportId,
            retryItems,
            session,
            actionHistory,
        )
    }

    private fun getTransport(transportType: TransportType): ITransport? {
        return when (transportType) {
            is SFTPTransportType -> workflowEngine.sftpTransport
            is RedoxTransportType -> workflowEngine.redoxTransport
            is NullTransportType -> NullTransport()
            else -> null
        }
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
            if (nextRetryCount >= maxRetryCount) {
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

    private fun calculateRetryTime(nextRetryCount: Int): OffsetDateTime {
        val waitMinutes = retryDuration.getOrDefault(nextRetryCount, maxDurationValue)
        return OffsetDateTime.now().plusMinutes(waitMinutes)
    }
}