package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.AS2TransportType
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.GAENTransportType
import gov.cdc.prime.router.NullTransportType
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.SoapTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.transport.ITransport
import gov.cdc.prime.router.transport.NullTransport
import gov.cdc.prime.router.transport.RetryToken
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import kotlin.random.Random

/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
const val send = "send"
// needed in case retry index is out-of-bounds call but should not be every be used
const val defaultMaxDurationValue = 120L

// exact time of next retry is slightly randomized to avoid the situation when
// there's a system-wide failure and everything that's failed retries at the exact
// same time producing a spike that makes things worse.
// This is +/- around the actual retry, so they are spread out by up to 2x the value
// It should always be > than the first entry of the retryDurationInMin above
const val initialRetryInMin = 10
const val ditherRetriesInSec = (initialRetryInMin / 2 * 60)

// Note: failure point is the SUM of all retry delays.
val retryDurationInMin = mapOf(
    1 to (initialRetryInMin * 1L), // dither might subtract half from this value
    2 to 60L, // 1hr10m later
    3 to (4 * 60L), // 5hr 10m since submission
    4 to (12 * 60L), // 17hr 10m since submission
    5 to (24 * 60L) //  1d 17r 10m since submission
)

// Use this for testing retries:
// val retryDuration = mapOf(1 to 1L, 2 to 1L, 3 to 1L, 4 to 1L, 5 to 1L)

class SendFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) : Logging {
    @FunctionName(send)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = send) message: String,
        context: ExecutionContext,
        @BindingName("Id") messageId: String? = null,
        @BindingName("DequeueCount") dequeueCount: Int? = null,
        @BindingName("NextVisibleTime") nextVisibleTime: Date? = null,
        @BindingName("InsertionTime") insertionTime: Date? = null,
    ) {
        val event = Event.parseQueueMessage(message) as ReportEvent
        val actionHistory = ActionHistory(TaskAction.send, event.isEmptyBatch)
        var receiverStatus: CustomerStatus = CustomerStatus.INACTIVE
        actionHistory.trackActionParams(message)
        logger.info(
            "Started Send Function: $message, id=$messageId," +
                " dequeueCount=$dequeueCount, " +
                " nextVisibleTime=$nextVisibleTime, insertionTime=$insertionTime"
        )
        try {
            if (event.eventAction != Event.EventAction.SEND) {
                logger.warn("Send function received a message of incorrect type: $message")
                return
            }
            workflowEngine.handleReportEvent(event) { header, retryToken, _ ->
                val receiver = header.receiver
                    ?: error("Internal Error: could not find ${header.task.receiverName}")
                actionHistory.trackActionReceiverInfo(receiver.organizationName, receiver.name)
                receiverStatus = receiver.customerStatus
                val inputReportId = header.reportFile.reportId
                actionHistory.trackExistingInputReport(inputReportId)
                val serviceName = receiver.fullName
                val nextRetryItems = mutableListOf<String>()
                if (receiver.transport == null) {
                    actionHistory.setActionType(TaskAction.send_warning)
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
                logger.info("For $inputReportId:  finished send().  Checking to see if a retry is needed.")
                handleRetry(
                    nextRetryItems,
                    inputReportId,
                    receiver,
                    retryToken,
                    actionHistory,
                    event.isEmptyBatch
                )
            }
        } catch (t: Throwable) {
            // For debugging and auditing purposes
            val msg = "Send function unrecoverable exception: ${t.message} Event: $message"
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            if (receiverStatus == CustomerStatus.ACTIVE) {
                logger.fatal("${actionHistory.action.actionResult}", t)
            } else {
                logger.error("${actionHistory.action.actionResult}", t)
            }
        } finally {
            // Note this is operating in a different transaction than the one that did the fetch/lock of the repor
            logger.debug("About to save ActionHistory for $message")
            workflowEngine.recordAction(actionHistory)
            logger.debug("Done saving ActionHistory for $message")
        }
    }

    private fun getTransport(transportType: TransportType): ITransport? {
        return when (transportType) {
            is SFTPTransportType -> workflowEngine.sftpTransport
            is BlobStoreTransportType -> workflowEngine.blobStoreTransport
            is AS2TransportType -> workflowEngine.as2Transport
            is SoapTransportType -> workflowEngine.soapTransport
            is GAENTransportType -> workflowEngine.gaenTransport
            is RESTTransportType -> workflowEngine.restTransport
            is NullTransportType -> NullTransport()
            else -> null
        }
    }

    private fun handleRetry(
        nextRetryItems: List<String>,
        reportId: ReportId,
        receiver: Receiver,
        retryToken: RetryToken?,
        actionHistory: ActionHistory,
        isEmptyBatch: Boolean
    ): ReportEvent {
        return if (nextRetryItems.isEmpty()) {
            // All OK
            logger.info("Successfully sent report: $reportId to ${receiver.fullName}")
            ReportEvent(Event.EventAction.NONE, reportId, isEmptyBatch)
        } else {
            // mapOf() in kotlin is `1` based (not `0`), but always +1
            val nextRetryCount = (retryToken?.retryCount ?: 0) + 1
            if (nextRetryCount > retryDurationInMin.size) {
                // Stop retrying and just put the task into an error state
                val msg = "All retries failed.  Manual Intervention Required.  " +
                    "Send Error report for: $reportId to ${receiver.fullName}"
                actionHistory.setActionType(TaskAction.send_error)
                actionHistory.trackActionResult(msg)
                if (receiver.customerStatus == CustomerStatus.ACTIVE) {
                    logger.fatal("${actionHistory.action.actionResult}")
                } else {
                    logger.error("${actionHistory.action.actionResult}")
                }
                ReportEvent(Event.EventAction.SEND_ERROR, reportId, isEmptyBatch)
            } else {
                // retry using a back-off strategy
                val waitMinutes = retryDurationInMin.getOrDefault(nextRetryCount, defaultMaxDurationValue)
                val randomSeconds = Random.nextInt(ditherRetriesInSec * -1, ditherRetriesInSec)
                val nextRetryTime = OffsetDateTime.now().plusSeconds(waitMinutes * 60 + randomSeconds)
                val nextRetryToken = RetryToken(nextRetryCount, nextRetryItems)
                val msg = "Send Failed.  Will retry sending report: $reportId to ${receiver.fullName}" +
                    " in $waitMinutes minutes and $randomSeconds seconds at $nextRetryTime"
                logger.warn(msg)
                actionHistory.setActionType(TaskAction.send_warning)
                actionHistory.trackActionResult(msg)
                ReportEvent(Event.EventAction.SEND, reportId, isEmptyBatch, nextRetryTime, nextRetryToken)
            }
        }
    }
}