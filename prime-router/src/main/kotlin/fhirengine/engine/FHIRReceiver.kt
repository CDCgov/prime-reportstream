package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.hl7v2.model.Message
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.reportstream.shared.SubmissionsEntity
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.InvalidReportMessage
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
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.context.MDCUtils
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.common.AzureHttpUtils.getSenderIP
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.report.ReportService
import org.jooq.Field
import java.time.OffsetDateTime

/**
 * FHIRReceiver is responsible for processing messages from the raw-elr Azure queue, converting them to FHIR format,
 * and storing them for the next step in the pipeline.
 *
 * @param metadata Mockable metadata instance.
 * @param settings Mockable settings provider.
 * @param db Mockable database access.
 * @param blob Mockable blob storage access.
 * @param azureEventService Service for handling Azure events.
 * @param reportService Service for handling report-related operations.
 */
class FHIRReceiver(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService) {

    override val finishedField: Field<OffsetDateTime> = Tables.TASK.PROCESSED_AT

    override val engineType: String = "Receive"

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
        actionLogger.setReportId(queueMessage.reportId)
        val contextMap = createLoggingContextMap(queueMessage, actionHistory)
        // Use the logging context for tracing
        withLoggingContext(contextMap) {
            val sender = getSender(queueMessage, actionLogger, actionHistory)

            // Handle errors if any
            if (sender == null) {
                handleSenderNotFound(queueMessage, actionLogger, actionHistory)
                return emptyList()
            }

            // Process the message if no errors occurred
            return handleSuccessfulProcessing(queueMessage, sender, actionLogger, actionHistory)
        }
    }

    /**
     * Creates the logging context map.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionHistory The action history related to receiving the report.
     * @return The logging context map.
     */
    private fun createLoggingContextMap(
        queueMessage: FhirReceiveQueueMessage,
        actionHistory: ActionHistory,
    ): Map<MDCUtils.MDCProperty, Any> = mapOf(
            MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
            MDCUtils.MDCProperty.REPORT_ID to queueMessage.reportId,
            MDCUtils.MDCProperty.BLOB_URL to queueMessage.blobURL,
        )

    /**
     * Retrieves the sender based on the queue message and logs any relevant errors.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionLogger The logger used to track actions and errors.
     * @param actionHistory The action history related to receiving the report.
     * @return The sender, or null if the sender was not found or is inactive.
     */
    private fun getSender(
        queueMessage: FhirReceiveQueueMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): Sender? {
        val clientId = queueMessage.headers[clientIdHeader]
        val sender = clientId?.takeIf { it.isNotBlank() }?.let { settings.findSender(it) }

        actionHistory.trackActionParams(queueMessage.headers.toString())

        // Handle case where sender is not found
        if (sender == null) {
            actionHistory.trackActionResult(HttpStatus.BAD_REQUEST)
            actionLogger.error(
                InvalidParamMessage("Sender not found matching client_id: " + queueMessage.headers[clientIdHeader])
            )
            return null
        }

        // Handle case where sender is inactive
        if (sender.customerStatus == CustomerStatus.INACTIVE) {
            // Track the action result and log the error
            actionHistory.trackActionResult(HttpStatus.NOT_ACCEPTABLE)
            actionLogger.error(
                InvalidParamMessage("Sender has customer status INACTIVE: " + queueMessage.headers[clientIdHeader])
            )
        }

        // Track sender information
        actionHistory.trackActionSenderInfo(sender.fullName, queueMessage.headers["payloadname"])
        actionHistory.trackActionResult(HttpStatus.CREATED)
        return sender
    }

    /**
     * Handles cases where processing errors occurred.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionLogger The logger used to track actions and errors.
     * @param actionHistory The action history related to receiving the report.
     */
    private fun handleSenderNotFound(
        queueMessage: FhirReceiveQueueMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ) {
        // Track the logs and errors
        actionHistory.trackLogs(actionLogger.logs)

        // Send an error event
        reportEventService.sendReceiveProcessingError(
            ReportStreamEventName.REPORT_NOT_RECEIVABLE,
            TaskAction.convert,
            "Unable to create report from received message.",
            queueMessage.reportId,
            queueMessage.blobURL
        ) {
            params(actionLogger.errors.associateBy { ReportStreamEventProperties.PROCESSING_ERROR })
        }

        // Insert the rejection into the submissions table
        val tableEntity =
            SubmissionsEntity(queueMessage.reportId.toString(), "Rejected").toTableEntity()
        BlobAccess.insertTableEntity("submissions", tableEntity)
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
        val report = validateSubmissionMessage(sender, actionLogger, queueMessage) ?: return emptyList()

        // Determine the mime format of the message
        val mimeFormat =
            MimeFormat.valueOfFromMimeType(
                queueMessage.headers[contentTypeHeader]?.substringBefore(';') ?: ""
            )

        actionHistory.trackExternalInputReport(
            report,
            queueMessage.blobURL,
            mimeFormat.toString(),
            queueMessage.digest.toByteArray()
        )

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
                    getSenderIP(queueMessage.headers)?.let { ReportStreamEventProperties.SENDER_IP to it }
                ).toMap()
            )
        }

        // Insert the acceptance into the submissions table
        val tableEntity = SubmissionsEntity(queueMessage.reportId.toString(), "Accepted").toTableEntity()
        BlobAccess.insertTableEntity("submissions", tableEntity)

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

    private fun validateSubmissionMessage(
        sender: Sender,
        actionLogger: ActionLogger,
        queueMessage: FhirReceiveQueueMessage,
    ): Report? {
        val rawReport = BlobAccess.downloadContent(queueMessage.blobURL, queueMessage.digest)
        return if (rawReport.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
            null
        } else {
            val report: Report
            val sources = listOf(ClientSource(organization = sender.organizationName, client = sender.name))

            when (sender.format) {
                MimeFormat.HL7 -> {
                    val messages: List<Message> = HL7Reader(actionLogger).getMessages(rawReport)
                    val isBatch: Boolean = HL7Reader(actionLogger).isBatch(rawReport, messages.size)
                    // create a Report for this incoming HL7 message to use for tracking in the database

                    report = Report(
                        if (isBatch) MimeFormat.HL7_BATCH else MimeFormat.HL7,
                        sources,
                        messages.size,
                        metadata = metadata,
                        nextAction = TaskAction.convert,
                        topic = sender.topic,
                    )

                    // TODO fix and re-enable https://github.com/CDCgov/prime-reportstream/issues/14103
                    // dupe detection if needed, and if we have not already produced an error
    //                if (!allowDuplicates && !actionLogs.hasErrors()) {
    //                    doDuplicateDetection(
    //                        workflowEngine,
    //                        report,
    //                        actionLogs
    //                    )
    //                }

                    // check for valid message type
                    messages.forEachIndexed { idx, element ->
                        MessageType.validateMessageType(element, actionLogger, idx + 1)
                    }
                }

                MimeFormat.FHIR -> {
                    val bundles = FhirTranscoder.getBundles(rawReport, actionLogger)
                    report = Report(
                        MimeFormat.FHIR,
                        sources,
                        bundles.size,
                        metadata = metadata,
                        nextAction = TaskAction.convert,
                        topic = sender.topic,
                    )
                }

                MimeFormat.CSV -> {
                    TODO()
                }

                else -> {
                    throw IllegalStateException("Unexpected sender format ${sender.format}")
                }
            }
            report
        }
    }
}