package gov.cdc.prime.router.fhirengine.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.reportstream.shared.Submission
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIRDestinationFilter
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverEnrichment
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.FhirConvertSubmissionQueueMessage
import gov.cdc.prime.router.fhirengine.engine.PrimeRouterQueueMessage
import gov.cdc.prime.router.fhirengine.engine.ReportPipelineMessage
import gov.cdc.prime.router.fhirengine.engine.SubmissionSenderNotFound
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.report.ReportService
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.util.Base64

class FHIRFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionLogger: ActionLogger = ActionLogger(),
    private val databaseAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    private val queueAccess: QueueAccess = QueueAccess,
    private val submissionTableService: SubmissionTableService = SubmissionTableService.getInstance(),
    val reportService: ReportService = ReportService(ReportGraph(databaseAccess), databaseAccess),
    val azureEventService: AzureEventService = AzureEventServiceImpl(),
    val reportStreamEventService: ReportStreamEventService =
        ReportStreamEventService(databaseAccess, azureEventService, reportService),
) : Logging {

    /**
     * An azure function for ingesting and recording submissions
     */
    @FunctionName("convert-from-submissions-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun convertFromSubmissions(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrSubmissionConvertQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.info(
            "message consumed from ${QueueMessage.elrSubmissionConvertQueueName} queue"
        )
        process(
            message,
            dequeueCount,
            FHIRConverter(reportStreamEventService = reportStreamEventService),
            ActionHistory(TaskAction.convert)
        )
        val messageContent = readMessage("convert", message, dequeueCount)
        val tableEntity = Submission(
            messageContent.reportId.toString(),
            "Accepted",
            messageContent.blobURL,
            actionLogger.errors.takeIf { it.isNotEmpty() }?.map { it.detail.message }?.toString()
        )
        submissionTableService.insertSubmission(tableEntity)
    }

    /**
     * An azure function for ingesting full-ELR HL7 data and converting it to FHIR
     */
    @FunctionName("convert-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun convert(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrConvertQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        process(
            message,
            dequeueCount,
            FHIRConverter(reportStreamEventService = reportStreamEventService),
            ActionHistory(TaskAction.convert)
        )
    }

    /**
     * An azure function for selecting valid destinations for inbound full-ELR FHIR data.
     */
    @FunctionName("destination-filter-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun destinationFilter(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrDestinationFilterQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        process(
            message,
            dequeueCount,
            FHIRDestinationFilter(reportStreamEventService = reportStreamEventService),
            ActionHistory(TaskAction.destination_filter)
        )
    }

    /**
     * An azure function for running receiver filters on full-ELR FHIR data
     */
    @FunctionName("receiver-filter-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun receiverFilter(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrReceiverFilterQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        process(
            message,
            dequeueCount,
            FHIRReceiverFilter(reportStreamEventService = reportStreamEventService),
            ActionHistory(TaskAction.receiver_filter)
        )
    }

    /**
     * An azure function for translating full-ELR FHIR data.
     */
    @FunctionName("translate-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun translate(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrTranslationQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        process(
            message,
            dequeueCount,
            FHIRTranslator(reportStreamEventService = reportStreamEventService),
            ActionHistory(TaskAction.translate)
        )
    }

    /**
     * An Azure function for enriching ELR FHIR receiver data.
     */
    @FunctionName("elr-fhir-receiver-enrichment")
    @StorageAccount("AzureWebJobsStorage")
    fun receiverEnrichment(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrReceiverEnrichmentQueueName)
        message: String,
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        process(
            message,
            dequeueCount,
            FHIRReceiverEnrichment(reportStreamEventService = reportStreamEventService),
            ActionHistory(TaskAction.receiver_enrichment)
        )
    }

    /**
     * Functionality separated from azure function call so a mocked fhirEngine can be passed in for testing.
     * Reads the [message] passed in and processes it using the appropriate [fhirEngine]. If there is an error
     * the [dequeueCount] is tracked as part of the log.
     * [actionHistory] is an optional parameter for use in testing
     */
    internal fun process(
        message: String,
        dequeueCount: Int,
        fhirEngine: FHIREngine,
        actionHistory: ActionHistory,
    ) {
        val messagesToDispatch = runFhirEngine(message, dequeueCount, fhirEngine, actionHistory)
        messagesToDispatch.forEach {
            (it as PrimeRouterQueueMessage).send(queueAccess)
        }
    }

    /**
     * Deserializes the message, create the DB transaction and then runs the FHIR engine
     *
     * @param message the queue message to process
     * @param dequeueCount the number of times the messages has been processed
     * @param fhirEngine the engine that will do the work
     * @param actionHistory the history to record results to
     * @return any messages that need to be dispatched
     */
    private fun runFhirEngine(
        message: String,
        dequeueCount: Int,
        fhirEngine: FHIREngine,
        actionHistory: ActionHistory,
    ): List<QueueMessage> {
        val messageContent = readMessage(fhirEngine.engineType, message, dequeueCount)

        try {
            val newMessages = databaseAccess.transactReturning { txn ->
                val results = fhirEngine.run(messageContent, actionLogger, actionHistory, txn)
                recordResults(message, actionHistory, txn)
                results
            }
            reportStreamEventService.sendQueuedEvents()
            return newMessages
        } catch (ex: DataAccessException) {
            // This is the one exception type that we currently will allow for retrying as there are occasional
            // DB connectivity issues that are resolved without intervention
            logger.error(ex)
            throw ex
        } catch (ex: SubmissionSenderNotFound) {
            // This is a specific error case that can occur while handling a report via the new Submission service
            // In a situation that the sender is not found there is not enough information to record a report event
            // so, we want a poison queue message to be immediately added so that the configuration can be fixed
            logger.error(ex)
            val tableEntity = Submission(
                ex.reportId.toString(),
                "Rejected",
                ex.blobURL,
                actionLogger.errors.takeIf { it.isNotEmpty() }?.map { it.detail.message }?.toString()
            )
            submissionTableService.insertSubmission(tableEntity)
            val encodedMsg = Base64.getEncoder().encodeToString(message.toByteArray())
            queueAccess.sendMessage("${messageContent.messageQueueName}-poison", encodedMsg)
            return emptyList()
        } catch (ex: Exception) {
            // We're catching anything else that occurs because the most likely cause is a code or configuration error
            // that will not be resolved if the message is automatically retried
            // Instead, the error is recorded as an event and message is manually inserted into the poison queue
            val report = databaseAccess.fetchReportFile(messageContent.reportId)
            val encodedMsg = Base64.getEncoder().encodeToString(message.toByteArray())
            val poisonQueueMessageId = queueAccess.sendMessage("${messageContent.messageQueueName}-poison", encodedMsg)
            fhirEngine.reportEventService.sendReportProcessingError(
                ReportStreamEventName.PIPELINE_EXCEPTION,
                report,
                fhirEngine.taskAction,
                ex.message ?: ""
            ) {
                params(mapOf(ReportStreamEventProperties.POISON_QUEUE_MESSAGE_ID to poisonQueueMessageId))
            }

            return emptyList()
        }
    }

    /**
     * Deserializes the [message] into a Fhir Convert/Route/Translate Message, verifies it is of the correct type.
     * Logs the [engineType] and [dequeueCount]
     */
    private fun readMessage(engineType: String, message: String, dequeueCount: Int): ReportPipelineMessage {
        logger.debug(
            "${StringUtils.removeEnd(engineType, "e")}ing message: $message for the $dequeueCount time"
        )

        return when (val queueMessage = QueueMessage.deserialize(message)) {
            is QueueMessage.ReceiveQueueMessage -> {
                FhirConvertSubmissionQueueMessage(
                    queueMessage.reportId,
                    queueMessage.blobURL,
                    queueMessage.digest,
                    queueMessage.blobSubFolderName,
                    queueMessage.headers
                )
            }
            else -> {
                queueMessage as ReportPipelineMessage
            }
        }
    }

    /**
     * Tracks any action params that are part of the [message] and records the logs and actions to the database
     */
    private fun recordResults(message: String, actionHistory: ActionHistory, txn: DataAccessTransaction) {
        actionHistory.trackActionParams(message)
        actionHistory.trackLogs(actionLogger.logs)
        workflowEngine.recordAction(actionHistory, txn)
    }
}