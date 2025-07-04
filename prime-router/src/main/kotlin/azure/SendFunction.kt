package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.AS2TransportType
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.EmailTransportType
import gov.cdc.prime.router.GAENTransportType
import gov.cdc.prime.router.NullTransportType
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.SoapTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.context.SendFunctionLoggingContext
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.transport.ITransport
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

class SendFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val reportEventService: IReportStreamEventService = ReportStreamEventService(
        workflowEngine.db,
        workflowEngine.azureEventService,
        workflowEngine.reportService
    ),
) : Logging {
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
        val event = Event.parsePrimeRouterQueueMessage(message) as ReportEvent
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
                val nextRetryItems = mutableListOf<String>()
                val externalFileName = Report.formExternalFilename(header, workflowEngine.reportService)
                val sentReportId = UUID.randomUUID() // each sent report gets its own UUID
                val retryItems = retryToken?.items
                val transport = getTransport(receiver.transportType)
                val lineages = Report.createItemLineagesFromDb(header, sentReportId)
                val nextRetry = transport.send(
                    receiver.transportType,
                    header,
                    sentReportId,
                    externalFileName,
                    retryItems,
                    context,
                    actionHistory,
                    reportEventService,
                    workflowEngine.reportService,
                    lineages,
                    message
                )
                if (nextRetry != null) {
                    nextRetryItems += nextRetry
                }

                logger.info("For $inputReportId:  finished send().  Checking to see if a retry is needed.")
                handleRetry(
                    nextRetryItems,
                    header.reportFile,
                    receiver,
                    header.content,
                    externalFileName,
                    retryToken,
                    actionHistory,
                    event.isEmptyBatch,
                    lineages,
                    message
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
            // Note this is operating in a different transaction than the one that did the fetch/lock of the report
            logger.debug("About to save ActionHistory for $message")
            workflowEngine.recordAction(actionHistory)
            logger.debug("Done saving ActionHistory for $message")
        }
    }

    private fun getTransport(transportType: TransportType): ITransport {
        // IntelliJ complains about this when, but there's a ticket in for it https://youtrack.jetbrains.com/issue/KTIJ-21016
        // It should still compile, unless a TransportType was added without adding it here
        return when (transportType) {
            is SFTPTransportType -> workflowEngine.sftpTransport
            is BlobStoreTransportType -> workflowEngine.blobStoreTransport
            is AS2TransportType -> workflowEngine.as2Transport
            is SoapTransportType -> workflowEngine.soapTransport
            is GAENTransportType -> workflowEngine.gaenTransport
            is RESTTransportType -> workflowEngine.restTransport
            is NullTransportType -> workflowEngine.nullTransport
            is EmailTransportType -> workflowEngine.emailTransport
        }
    }

    /**
     * This method handles our retry logic. There are four basic scenarios:
     *   - Last Mile Failure A. A send_error has been logged elsewhere. There will be no more retries.
     *   - Success. No send error has been logged and no more items are left to be sent.
     *   - Last Mile Failure B. No send error has been logged yet but all retry attempts are exhausted.
     *   - Retry. No send error has been logged and the retry limit has not been reached yet.
     *
     *  @return a ReportEvent to complete pipeline processing
     */
    private fun handleRetry(
        nextRetryItems: List<String>,
        report: ReportFile,
        receiver: Receiver,
        content: ByteArray?,
        externalFileName: String,
        retryToken: RetryToken?,
        actionHistory: ActionHistory,
        isEmptyBatch: Boolean,
        lineages: List<ItemLineage>?,
        message: String,
    ): ReportEvent {
        withLoggingContext(SendFunctionLoggingContext(report.reportId, receiver.fullName)) {
            // mapOf() in kotlin is `1` based (not `0`), but always +1
            val nextRetryCount = (retryToken?.retryCount ?: 0) + 1

            // Last Mile Failure A: unrecoverable send_error occurred
            if (actionHistory.action.actionName.toString() == "send_error") {
                // We didn't get here without a send attempt, and it definitely failed.
                logRetryEvent(
                    nextRetryItems,
                    nextRetryCount,
                    report,
                    receiver,
                    actionHistory,
                    isEmptyBatch,
                    lineages,
                    message
                )

                return logLastMileFailureEvent(
                    report,
                    receiver,
                    content,
                    externalFileName,
                    actionHistory,
                    isEmptyBatch,
                    lineages,
                    message
                )
            }

            // No send errors, and no items to retry
            if (nextRetryItems.isEmpty()) {
                // Success!
                logger.info("Successfully sent report: ${report.reportId} to ${receiver.fullName}")
                return ReportEvent(Event.EventAction.NONE, report.reportId, isEmptyBatch)
            }

            // No send errors, but still items to retry
            if (nextRetryCount > retryDurationInMin.size) {
                // Last Mile Failure B: No retries remaining
                return logLastMileFailureEvent(
                    report,
                    receiver,
                    content,
                    externalFileName,
                    actionHistory,
                    isEmptyBatch,
                    lineages,
                    message
                )
            } else {
                // Keep Retrying
                return logRetryEvent(
                    nextRetryItems,
                    nextRetryCount,
                    report,
                    receiver,
                    actionHistory,
                    isEmptyBatch,
                    lineages,
                    message
                )
            }
        }
    }

    /**
     * Create logging and Azure Events when a send attempt has failed.
     * This will create logs/events for each item which failed sending.
     */
    private fun logRetryEvent(
        nextRetryItems: List<String>,
        nextRetryCount: Int,
        report: ReportFile,
        receiver: Receiver,
        actionHistory: ActionHistory,
        isEmptyBatch: Boolean,
        lineages: List<ItemLineage>?,
        message: String,
    ): ReportEvent {
        // retry using a back-off strategy
        val waitMinutes = retryDurationInMin.getOrDefault(nextRetryCount, defaultMaxDurationValue)
        val randomSeconds = Random.nextInt(ditherRetriesInSec * -1, ditherRetriesInSec)
        val nextRetryTime = OffsetDateTime.now().plusSeconds(waitMinutes)
        val nextRetryToken = RetryToken(nextRetryCount, nextRetryItems)
        val msg = "Send Failed.  Will retry sending report: ${report.reportId} to ${receiver.fullName}" +
            " in $waitMinutes minutes and $randomSeconds seconds at $nextRetryTime."

        logger.warn(msg)
        actionHistory.setActionType(TaskAction.send_warning)
        actionHistory.trackActionResult(msg)
        actionHistory.trackItemSendState(
            ReportStreamEventName.ITEM_SEND_ATTEMPT_FAIL,
            report,
            lineages,
            receiver,
            nextRetryCount,
            nextRetryTime,
            message,
            reportEventService,
            workflowEngine.reportService
        )

        return ReportEvent(Event.EventAction.SEND, report.reportId, isEmptyBatch, nextRetryTime, nextRetryToken)
    }

    /**
     * Create logging and Azure Events for when a Last Mile Failure occurs.
     * This will create logs/events for at the Report level, as well as for each Item..
     */
    private fun logLastMileFailureEvent(
        report: ReportFile,
        receiver: Receiver,
        content: ByteArray?,
        externalFileName: String,
        actionHistory: ActionHistory,
        isEmptyBatch: Boolean,
        lineages: List<ItemLineage>?,
        message: String,
    ): ReportEvent {
        // Stop retrying and just put the task into an error state
        val msg = "All retries failed.  Manual Intervention Required.  " +
            "Send Error report for: ${report.reportId} to ${receiver.fullName}"
        actionHistory.setActionType(TaskAction.send_error)
        actionHistory.trackActionResult(msg)
        logger.warn("Failed to send report: ${report.reportId} to ${receiver.fullName}")

        if (receiver.customerStatus == CustomerStatus.ACTIVE) {
            logger.fatal("${actionHistory.action.actionResult}")
        } else {
            logger.error("${actionHistory.action.actionResult}")
        }

        val fileSize = content?.size ?: 0
        // The last mile failure event has a slightly different pattern because we do not generate a child
        // report as an output for this event so the childReport is the input to the send step and the
        // parent report is the input to the batch step
        val parentReport = workflowEngine.db.fetchFirstParentReport(report.reportId)

        reportEventService.sendReportEvent(
            eventName = ReportStreamEventName.REPORT_LAST_MILE_FAILURE,
            childReport = report,
            pipelineStepName = TaskAction.send,
            queueMessage = message
        ) {
            params(
                mapOf(
                    ReportStreamEventProperties.PROCESSING_ERROR to msg,
                    ReportStreamEventProperties.RECEIVER_NAME to receiver.fullName,
                    ReportStreamEventProperties.TRANSPORT_TYPE to (receiver.transport?.type ?: ""),
                    ReportStreamEventProperties.FILE_LENGTH to fileSize,
                    ReportStreamEventProperties.FILENAME to externalFileName
                )
            )
            if (parentReport != null) {
                parentReportId(parentReport.reportId)
            }
        }

        actionHistory.trackItemSendState(
            ReportStreamEventName.ITEM_LAST_MILE_FAILURE,
            report,
            lineages,
            receiver,
            null,
            null,
            message,
            reportEventService,
            workflowEngine.reportService
        )

        return ReportEvent(Event.EventAction.SEND_ERROR, report.reportId, isEmptyBatch)
    }
}