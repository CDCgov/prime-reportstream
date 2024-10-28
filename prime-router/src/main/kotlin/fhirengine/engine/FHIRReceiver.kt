package gov.cdc.prime.router.fhirengine.engine

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.reportstream.shared.Submission
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.context.MDCUtils
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.common.AzureHttpUtils.getSenderIP
import gov.cdc.prime.router.report.ReportService
import org.jooq.Field
import java.time.OffsetDateTime

/**
 * FHIRReceiver is responsible for processing messages from the elr-fhir-receive azure queue
 * and storing them for the next step in the pipeline.
 *
 * @param metadata Mockable metadata instance.
 * @param settings Mockable settings provider.
 * @param db Mockable database access.
 * @param blob Mockable blob storage access.
 * @param azureEventService Service for handling Azure events.
 * @param reportService Service for handling report-related operations.
 * @param submissionTableService Service for inserting to the submission azure storage table.
 */
class FHIRReceiver(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
    val submissionTableService: SubmissionTableService = SubmissionTableService.getInstance(),
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService) {

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.PROCESSED_AT

    override val engineType: String = "Receive"
    override val taskAction: TaskAction = TaskAction.receive

    private val clientIdHeader = "client_id"
    private val contentTypeHeader = "content-type"

    /**
     * Processes a message of type [QueueMessage]. This message can be in either HL7 or FHIR format and will be placed
     * on a queue for further processing.
     *
     * @param message The incoming message to be logged and processed.
     * @param actionLogger Logger to track actions and errors.
     * @param actionHistory Tracks the history of actions performed.
     * @return A list of results from the FHIR engine run.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> = when (message) {
            is FhirReceiveQueueMessage -> processFhirReceiveQueueMessage(message, actionLogger, actionHistory)
            else -> throw RuntimeException("Message was not a FhirReceive and cannot be processed: $message")
        }

    /**
     * Processes the FHIR receive queue message.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionLogger The logger used to track actions and errors.
     * @param actionHistory The action history related to receiving the report.
     * @return A list of FHIR engine run results.
     */
    private fun processFhirReceiveQueueMessage(
        queueMessage: FhirReceiveQueueMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        // Use the logging context for tracing
        withLoggingContext(
            mapOf(
            MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
            MDCUtils.MDCProperty.REPORT_ID to queueMessage.reportId,
            MDCUtils.MDCProperty.BLOB_URL to queueMessage.blobURL,
        )
        ) {
            val clientId = queueMessage.headers[clientIdHeader]
            val sender = clientId?.takeIf { it.isNotBlank() }?.let { settings.findSender(it) }
            if (sender == null) {
                val submission =
                    Submission(
                        queueMessage.reportId.toString(), "Rejected",
                        queueMessage.blobURL,
                        "Sender not found matching client_id: ${queueMessage.headers[clientIdHeader]}"
                    )
                submissionTableService.insertSubmission(submission)
                throw SubmissionSenderNotFound(clientId ?: "")
            }
            actionLogger.setReportId(queueMessage.reportId)
            actionHistory.trackActionParams(queueMessage.headers.toString())

            // Process the message if no errors occurred
            return handleSuccessfulProcessing(queueMessage, sender, actionLogger, actionHistory)
        }
    }

    /**
     * Handles successful processing of the queue message.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param sender The sender information.
     * @param actionHistory The action history related to receiving the report.
     * @return A list of FHIR engine run results.
     */
    private fun handleSuccessfulProcessing(
        queueMessage: FhirReceiveQueueMessage,
        sender: Sender,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        // Get content from blob storage and create report
        val sources = listOf(ClientSource(organization = sender.organizationName, client = sender.name))
        val report = Report(
            sender.format,
            sources,
            1,
            metadata = metadata,
            nextAction = TaskAction.convert,
            topic = sender.topic,
            id = queueMessage.reportId,
            bodyURL = queueMessage.blobURL
        )

        // Determine the mime format of the message
        val mimeFormat =
            MimeFormat.valueOfFromMimeType(
                queueMessage.headers[contentTypeHeader]?.substringBefore(';') ?: ""
            )

        val blobInfo = BlobAccess.BlobInfo(
            mimeFormat,
            queueMessage.blobURL,
            queueMessage.digest.toByteArray()
        )

        actionHistory.trackExternalInputReport(
            report,
            blobInfo
        )
        actionHistory.trackActionSenderInfo(sender.fullName, queueMessage.headers["payloadname"])
        actionHistory.trackActionResult(HttpStatus.CREATED)

        // Send an event indicating the report was received
        reportEventService.sendReportEvent(
            eventName = ReportStreamEventName.REPORT_RECEIVED,
            childReport = report,
            pipelineStepName = TaskAction.receive
        ) {
            params(
                listOfNotNull(
                    ReportStreamEventProperties.REQUEST_PARAMETERS to queueMessage.headers.toString(),
                    ReportStreamEventProperties.SENDER_NAME to sender.fullName,
                    ReportStreamEventProperties.FILE_LENGTH to queueMessage.headers["content-length"].toString(),
                    getSenderIP(queueMessage.headers)?.let { ReportStreamEventProperties.SENDER_IP to it },
                    ReportStreamEventProperties.ITEM_FORMAT to mimeFormat
                ).toMap()
            )
        }

        // Insert the acceptance into the submissions table
        val tableEntity = Submission(
            queueMessage.reportId.toString(),
            "Accepted",
            queueMessage.blobURL,
            actionLogger.errors.takeIf { it.isNotEmpty() }?.map { it.detail.message }?.toString()
        )
        submissionTableService.insertSubmission(tableEntity)

        // Create a route event
        val routeEvent = ProcessEvent(Event.EventAction.CONVERT, report.id, Options.None, emptyMap(), emptyList())

        // Return the result of the FHIR engine run
        return listOf(
            FHIREngineRunResult(
                routeEvent,
                report,
                queueMessage.blobURL,
                FhirConvertQueueMessage(
                    report.id,
                    queueMessage.blobURL,
                    queueMessage.digest,
                    queueMessage.blobSubFolderName,
                    sender.topic,
                    sender.schemaName
                )
            )
        )
    }
}

class SubmissionSenderNotFound(sender: String) : RuntimeException() {
    override val message = "No sender was found with id: $sender"
}