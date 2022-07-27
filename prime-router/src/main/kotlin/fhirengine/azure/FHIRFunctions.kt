package gov.cdc.prime.router.fhirengine.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.Message
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.engine.elrConvertQueueName
import gov.cdc.prime.router.fhirengine.engine.elrRoutingQueueName
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import org.apache.logging.log4j.kotlin.Logging

class FHIRFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val fhirEngine: FHIREngine = FHIRConverter(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.process),
    private val actionLogger: ActionLogger = ActionLogger()
) : Logging {

    /**
     * An azure function for ingesting full-ELR HL7 data and converting it to FHIR
     */
    @FunctionName("convert-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun convert(
        @QueueTrigger(name = "message", queueName = elrConvertQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        val messageContent = readMessage("Convert", message, dequeueCount)

        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        recordResults(message)
    }

    /**
     * An azure function for routing full-ELR FHIR data.
     */
    @FunctionName("route-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun route(
        @QueueTrigger(name = "message", queueName = elrRoutingQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        val messageContent = readMessage("Route", message, dequeueCount)

        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        recordResults(message)
    }

    /**
     * An azure function for translating full-ELR FHIR data.
     */
    @FunctionName("translate-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun translate(
        @QueueTrigger(name = "message", queueName = elrTranslationQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        val messageContent = readMessage("Translate", message, dequeueCount)
        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        recordResults(message)
    }

    /**
     * Deserializes the [message] into a RawSubmission, verifies it is of the correct type.
     * Logs the [engineType] and [dequeueCount]
     */
    private fun readMessage(engineType: String, message: String, dequeueCount: Int): RawSubmission {
        logger.debug("${engineType}ing message: $message for the $dequeueCount time")
        val messageContent = Message.deserialize(message)
        check(messageContent is RawSubmission) {
            "An unknown message was received by the FHIR $engineType Function " +
                "${messageContent.javaClass.kotlin.qualifiedName}"
        }
        return messageContent
    }

    /**
     * Tracks any action params that are part of the [message] and records the logs and actions to the database
     */
    private fun recordResults(message: String) {
        actionHistory.trackActionParams(message)
        actionHistory.trackLogs(actionLogger.logs)
        workflowEngine.recordAction(actionHistory)
    }
}