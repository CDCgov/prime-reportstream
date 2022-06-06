package gov.cdc.prime.router.fhirengine.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.apache.logging.log4j.kotlin.Logging

const val elrProcessQueueName = "process-elr"

/**
 * Functions to execution and logging of FHIR translations.
 * @property workflowEngine the workflow engine
 * @property actionHistory the action history tracker
 * @property actionLogger the action logger
 */
class FHIRProcessFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.process),
    private val actionLogger: ActionLogger = ActionLogger()
) : Logging {

    /**
     * An azure function for processing ingesting full-ELR HL7 data.
     */
    @FunctionName("process-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun process(
        @QueueTrigger(name = "message", queueName = elrProcessQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.debug("Processing message: $message for the $dequeueCount time")
        // TODO: This will be part of 4824 and does not work yet.
//        val messageContent = Message.deserialize(message)
//        check(messageContent is RawSubmission) {
//            "An unknown message was received by the FHIR Engine ${messageContent.javaClass.kotlin.qualifiedName}"
//        }
//
//        try {
//            processHL7(messageContent)
//        } catch (e: Exception) {
//            logger.error("Unknown error.", e)
//        }
//        actionHistory.trackActionParams(message)
//        actionHistory.trackLogs(actionLogger.logs)
//        workflowEngine.recordAction(actionHistory)
    }

    /**
     * Process incoming HL7 data from a [messageContent] queue message.
     */
    private fun processHL7(messageContent: RawSubmission) {
        logger.trace("Processing HL7 data for FHIR conversion.")
        try {
            val hl7messages = HL7Reader(actionLogger).getMessages(messageContent.downloadContent())

            if (actionLogger.hasErrors()) {
                throw java.lang.IllegalArgumentException(actionLogger.errors.joinToString("\n") { it.detail.message })
            }

            val fhirBundles = hl7messages.map { message ->
                HL7toFhirTranslator.getInstance().translate(message)
            }
            // TODO instrument the rest of the pipeline.
            logger.info("Received ${fhirBundles.size} FHIR bundles.")
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }
}