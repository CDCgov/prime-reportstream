package gov.cdc.prime.router.fhirengine.engine

import QueueMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.reportstream.shared.SubmissionsEntity
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SenderNotFound
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
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
 * Process a message off of the raw-elr azure queue, convert it into FHIR, and store for next step.
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
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

    override val engineType: String = "Convert"

    /**
     * Accepts a [message] in either HL7 or FHIR format
     * HL7 messages will be converted into FHIR.
     * FHIR messages will be decoded and saved
     *
     * [message] is the incoming message to be turned into FHIR and saved
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> = when (message) {
        is FhirReceiveQueueMessage -> {
            fhirEngineRunResults(message, actionLogger, actionHistory)
        }
        else -> {
            throw RuntimeException(
                "Message was not a FhirConvert and cannot be processed: $message"
            )
        }
    }

    /**
     * Processes the received results by handling the sender information and logging the action history.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionLogger The logger used to track actions and errors.
     * @param convertActionHistory The action history related to the conversion process.
     * @return A list of FHIR engine run results.
     */
    private fun fhirEngineRunResults(
        queueMessage: FhirReceiveQueueMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ) {
        val contextMap = mapOf(
            MDCUtils.MDCProperty.ACTION_NAME to actionHistory.action.actionName.name,
            MDCUtils.MDCProperty.REPORT_ID to queueMessage.reportId,
            MDCUtils.MDCProperty.BLOB_URL to queueMessage.blobURL,
        )

        withLoggingContext(contextMap) {
            // Retrieve the sender settings
            val clientId = queueMessage.headers["client_id"]

            val sender = clientId?.takeIf { it.isNotBlank() }?.let { settings.findSender(it) }
            val payloadName = queueMessage.headers["payloadname"]
            val mimeType = queueMessage.headers["Content-Type"]?.substringBefore(';') ?: ""

            if (sender == null) {
                handleSenderNotFound(queueMessage, actionLogger, actionHistory)
            } else {
                if (sender.customerStatus == CustomerStatus.INACTIVE) {
                    handleInactiveSender(queueMessage, actionLogger, actionHistory)
                }
                actionHistory.trackActionSenderInfo(sender.fullName, payloadName)
            }

            actionHistory.trackActionResult(HttpStatus.CREATED)
            actionHistory.trackActionParams(queueMessage.headers.toString())

            if (actionLogger.hasErrors()) {
                reportEventService.sendReceiveProcessingError(
                    ReportStreamEventName.REPORT_NOT_RECEIVABLE,
                    TaskAction.convert,
                    "Unable to create report from received message.",
                    queueMessage.reportId,
                    queueMessage.blobURL
                ) {
                    params(
                        actionLogger.errors.associateBy { ReportStreamEventProperties.PROCESSING_ERROR }
                    )
                }
                val tableEntity = SubmissionsEntity(queueMessage.reportId.toString(), "Rejected").toTableEntity()
                BlobAccess.insertTableEntity(tableEntity)
            } else {
                val sources = listOf(ClientSource(organization = sender!!.organizationName, client = sender.name))
                val report = Report(
                    MimeFormat.valueOfFromMimeType(mimeType),
                    sources,
                    TaskAction.convert,
                    sender.topic
                )
                reportEventService.sendReportEvent(
                    eventName = ReportStreamEventName.REPORT_RECEIVED,
                    childReport = report,
                    pipelineStepName = TaskAction.receive
                ) {
                    params(
                        listOfNotNull(
                            ReportStreamEventProperties.REQUEST_PARAMETERS to queueMessage.headers.toString(),
                            ReportStreamEventProperties.SENDER_NAME to sender.fullName,
                            ReportStreamEventProperties.FILE_LENGTH to
                                queueMessage.headers["content-length"].toString(),
                            getSenderIP(queueMessage.headers)?.let { ReportStreamEventProperties.SENDER_IP to it }
                        ).toMap()
                    )
                }

                val tableEntity = SubmissionsEntity(queueMessage.reportId.toString(), "Accepted").toTableEntity()
                BlobAccess.insertTableEntity(tableEntity)
            }
        }
    }

    /**
     * Handles cases where the sender is not found.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionLogger The logger used to track actions and errors.
     * @param actionHistory The action history related to receiving the report.
     * @return An empty list of FHIR engine run results.
     */
    private fun handleSenderNotFound(
        queueMessage: FhirReceiveQueueMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ) {
        actionHistory.trackActionResult(HttpStatus.BAD_REQUEST)
        actionLogger.error(
            InvalidParamMessage("Sender not found matching client_id: " + queueMessage.headers["client_id"])
        )
    }

    /**
     * Handles cases where the sender is inactive.
     *
     * @param queueMessage The queue message containing details about the report.
     * @param actionLogger The logger used to track actions and errors.
     * @param actionHistory The action history related to receiving the report.
     * @return An empty list of FHIR engine run results.
     */
    private fun handleInactiveSender(
        queueMessage: FhirReceiveQueueMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ) {
        actionHistory.trackActionResult(HttpStatus.NOT_ACCEPTABLE)
        actionLogger.error(SenderNotFound(queueMessage.headers["client_id"].toString()))
    }
}