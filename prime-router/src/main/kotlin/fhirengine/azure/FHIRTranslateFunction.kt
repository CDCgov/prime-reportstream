package gov.cdc.prime.router.fhirengine.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.Message
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import org.apache.logging.log4j.kotlin.Logging

/**
 * Takes a FHIR message, parses it, and generates translated outputs for each receiver in the destinations list
 * @property workflowEngine the workflow engine
 * @property fhirEngine the fhir engine to use
 * @property actionHistory the action history tracker
 * @property actionLogger the action logger
 */
class FHIRTranslateFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val fhirEngine: FHIREngine = FHIRTranslator(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.translate),
    private val actionLogger: ActionLogger = ActionLogger()
) : Logging {

    /**
     * An azure function for translating full-ELR FHIR data.
     */
    @FunctionName("translate-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun process(
        @QueueTrigger(name = "message", queueName = elrTranslationQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.debug("Translating message: $message for the $dequeueCount time")
        val messageContent = Message.deserialize(message)
        check(messageContent is RawSubmission) {
            "An unknown message was received by the FHIR Translate Function " +
                "${messageContent.javaClass.kotlin.qualifiedName}"
        }

        try {
            fhirEngine.doWork(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        actionHistory.trackActionParams(message)
        actionHistory.trackLogs(actionLogger.logs)
        workflowEngine.recordAction(actionHistory)
    }
}