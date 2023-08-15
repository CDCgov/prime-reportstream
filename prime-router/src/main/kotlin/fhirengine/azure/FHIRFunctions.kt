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
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.Message
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.engine.elrConvertQueueName
import gov.cdc.prime.router.fhirengine.engine.elrRoutingQueueName
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.Logging

class FHIRFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
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
        @BindingName("DequeueCount") dequeueCount: Int = 1
    ) {
        doConvert(message, dequeueCount, FHIRConverter())
    }

    /**
     * Functionality separated from azure function call so a mocked fhirEngine can be passed in for testing.
     * Reads the [message] passed in and processes it using the appropriate [fhirEngine]. If there is an error
     * the [dequeueCount] is tracked as part of the log.
     * [actionHistory] is an optional parameter for use in testing
     */
    internal fun doConvert(
        message: String,
        dequeueCount: Int,
        fhirEngine: FHIREngine,
        actionHistory: ActionHistory = ActionHistory(TaskAction.convert)
    ) {
        val messageContent = readMessage("Convert", message, dequeueCount)
        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        recordResults(message, actionHistory)
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
        @BindingName("DequeueCount") dequeueCount: Int = 1
    ) {
        doRoute(message, dequeueCount, FHIRRouter())
    }

    /**
     * Functionality separated from azure function call so a mocked fhirEngine can be passed in for testing.
     * Reads the [message] passed in and processes it using the appropriate [fhirEngine]. If there is an error
     * the [dequeueCount] is tracked as part of the log.
     * [actionHistory] is an optional parameter for use in testing
     */
    internal fun doRoute(
        message: String,
        dequeueCount: Int,
        fhirEngine: FHIRRouter,
        actionHistory: ActionHistory = ActionHistory(TaskAction.route)
    ) {
        val messageContent = readMessage("Route", message, dequeueCount)

        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        recordResults(message, actionHistory)
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
        @BindingName("DequeueCount") dequeueCount: Int = 1
    ) {
        doTranslate(message, dequeueCount, FHIRTranslator())
    }

    /**
     * Functionality separated from azure function call so a mocked fhirEngine can be passed in for testing.
     * Reads the [message] passed in and processes it using the appropriate [fhirEngine]. If there is an error
     * the [dequeueCount] is tracked as part of the log.
     * [actionHistory] is an optional parameter for use in testing
     */
    fun doTranslate(
        message: String,
        dequeueCount: Int,
        fhirEngine: FHIRTranslator,
        actionHistory: ActionHistory = ActionHistory(TaskAction.translate)
    ) {
        val messageContent = readMessage("Translate", message, dequeueCount)

        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        recordResults(message, actionHistory)
    }

    /**
     * Deserializes the [message] into a RawSubmission, verifies it is of the correct type.
     * Logs the [engineType] and [dequeueCount]
     */
    private fun readMessage(engineType: String, message: String, dequeueCount: Int): RawSubmission {
        logger.debug(
            "${StringUtils.removeEnd(engineType, "e")}ing message: $message for the $dequeueCount time"
        )
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
    private fun recordResults(message: String, actionHistory: ActionHistory) {
        actionHistory.trackActionParams(message)
        actionHistory.trackLogs(actionLogger.logs)
        workflowEngine.recordAction(actionHistory)
    }
}