package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.RetryTransport
import java.time.OffsetDateTime
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
const val dataRetentionDays = 7L
const val send = "send"
const val maxRetryCount = 10
const val maxDurationValue = 120L
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

            val event = Event.parseQueueMessage(message) as ReportEvent
            workflowEngine.handleReportEvent(event) { header, retryToken, _ ->
                val service = workflowEngine.metadata.findService(header.task.receiverName)
                    ?: error("Internal Error: could not find ${header.task.receiverName}")
                val reportId = header.task.reportId
                val serviceName = service.fullName

                val content = workflowEngine.readBody(header)
                val nextRetryTransports = mutableListOf<RetryTransport>()
                service
                    .transports
                    .filterIndexed { i, _ -> retryToken == null || retryToken.transports.find { it.index == i } != null }
                    .forEachIndexed { i, transport ->
                        val retryItems = retryToken?.transports?.find { it.index == i }?.items
                        val nextRetryItems = when (transport) {
                            is SFTPTransportType -> {
                                workflowEngine
                                    .sftpTransport
                                    .send(service, transport, content, reportId, retryItems, context)
                            }
                            is RedoxTransportType -> {
                                workflowEngine
                                    .redoxTransport
                                    .send(service, transport, content, reportId, retryItems, context)
                            }
                            else -> null
                        }
                        if (nextRetryItems != null) {
                            nextRetryTransports.add(RetryTransport(i, nextRetryItems))
                        }
                    }
                handleRetry(nextRetryTransports, reportId, serviceName, retryToken, context)
            }
            // For debugging and auditing purposes
        } catch (t: Throwable) {
            context.logger.log(Level.SEVERE, "Send exception", t)
        }
    }

    private fun handleRetry(
        nextRetryTransports: List<RetryTransport>,
        reportId: ReportId,
        serviceName: String,
        retryToken: RetryToken?,
        context: ExecutionContext
    ): ReportEvent {
        return if (nextRetryTransports.isEmpty()) {
            // All OK
            context.logger.info("Successfully sent report: $reportId to $serviceName")
            // TODO: Next action should be WIPE when implemented
            ReportEvent(Event.Action.NONE, reportId)
        } else {
            val nextRetryCount = (retryToken?.retryCount ?: 0) + 1
            if (nextRetryCount >= maxRetryCount) {
                // Stop retrying and just put the task into an error state
                context.logger.info("All retries failed.  Send Error report for: $reportId to $serviceName")
                ReportEvent(Event.Action.SEND_ERROR, reportId)
            } else {
                // retry using a back-off strategy
                val waitMinutes = retryDuration.getOrDefault(nextRetryCount, maxDurationValue)
                val nextRetryTime = OffsetDateTime.now().plusMinutes(waitMinutes)
                val nextRetryToken = RetryToken(nextRetryCount, nextRetryTransports)
                context.logger.info("Send Failed.  Will retry sending report: $reportId to $serviceName} in $waitMinutes minutes, at $nextRetryTime")
                ReportEvent(Event.Action.SEND, reportId, nextRetryTime, nextRetryToken)
            }
        }
    }
}