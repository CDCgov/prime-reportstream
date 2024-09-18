package gov.cdc.prime.router.fhirengine.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.reportstream.shared.queue_message.QueueMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIRDestinationFilter
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiver
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.FhirReceiveQueueMessage
import gov.cdc.prime.router.fhirengine.engine.PrimeRouterQueueMessage
import gov.cdc.prime.router.fhirengine.engine.ReportPipelineMessage
import gov.cdc.prime.router.fhirengine.engine.initializeQueueMessages
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.Logging

class FHIRFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionLogger: ActionLogger = ActionLogger(),
    private val databaseAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    private val queueAccess: QueueAccess = QueueAccess,
) : Logging {

    /**
     * An azure function for ingesting and recording submissions
     */
    @FunctionName("receive-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun receive(
        @QueueTrigger(name = "message", queueName = QueueMessage.elrReceiveQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.info(
            "message consumed from elr-fhir-receive queue"
        )
        process(message, dequeueCount, FHIRReceiver(), ActionHistory(TaskAction.receive))
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
        process(message, dequeueCount, FHIRConverter(), ActionHistory(TaskAction.convert))
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
        process(message, dequeueCount, FHIRDestinationFilter(), ActionHistory(TaskAction.destination_filter))
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
        process(message, dequeueCount, FHIRReceiverFilter(), ActionHistory(TaskAction.receiver_filter))
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
        process(message, dequeueCount, FHIRTranslator(), ActionHistory(TaskAction.translate))
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

        val newMessages = databaseAccess.transactReturning { txn ->
            val results = fhirEngine.run(messageContent, actionLogger, actionHistory, txn)
            recordResults(message, actionHistory, txn)
            results
        }

        return newMessages
    }

    /**
     * Deserializes the [message] into a Fhir Convert/Route/Translate Message, verifies it is of the correct type.
     * Logs the [engineType] and [dequeueCount]
     */
    private fun readMessage(engineType: String, message: String, dequeueCount: Int): ReportPipelineMessage {
        logger.debug(
            "${StringUtils.removeEnd(engineType, "e")}ing message: $message for the $dequeueCount time"
        )
        // initialize the json types in PrimeRouterQueueMessage
        initializeQueueMessages()

        return when (val queueMessage = QueueMessage.deserialize(message)) {
            is QueueMessage.ReceiveQueueMessage -> {
                FhirReceiveQueueMessage(
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