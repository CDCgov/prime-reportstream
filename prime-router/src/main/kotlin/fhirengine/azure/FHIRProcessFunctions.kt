package gov.cdc.prime.router.fhirengine.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.HttpException
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.Message
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.http.extensions.contentType
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.apache.logging.log4j.kotlin.Logging

const val elrProcessQueueName = "process-elr"

/**
 * Functions to execution and logging of FHIR translations.
 * @property workflowEngine the workflow engine
 * @property fhirEngine the fhir engine to use
 * @property actionHistory the action history tracker
 * @property actionLogger the action logger
 */
class FHIRProcessFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val fhirEngine: FHIREngine = FHIREngine(),
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
        val messageContent = Message.deserialize(message)
        check(messageContent is RawSubmission) {
            "An unknown message was received by the FHIR Engine ${messageContent.javaClass.kotlin.qualifiedName}"
        }

        try {
            fhirEngine.processHL7(messageContent, actionLogger, actionHistory)
        } catch (e: Exception) {
            logger.error("Unknown error.", e)
        }
        actionHistory.trackActionParams(message)
        actionHistory.trackLogs(actionLogger.logs)
        workflowEngine.recordAction(actionHistory)
    }

    @FunctionName("convert")
    @StorageAccount("AzureWebJobsStorage")
    fun convert(
        @HttpTrigger(
            name = "convert",
            methods = [HttpMethod.POST],
            route = "\$convert-data"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val responseBuilder = request.createResponseBuilder(HttpStatus.OK)
        val body = try {
            val requestBody = request.body
            requireNotNull(requestBody)

            responseBuilder.contentType(HttpUtilities.fhirMediaType)
            val (hl7messages, errors) = HL7Reader().getMessages(requestBody)
            if (errors.size > 0) {
                throw IllegalArgumentException()
            }
            require(hl7messages.size > 0) { "No messages were found" }
            val bundles = hl7messages.map { message ->
                HL7toFhirTranslator.getInstance().translate(message)
            }
            buildString {
                bundles.forEach { bundle ->
                    appendLine(FhirTranscoder.encode(bundle))
                }
            }
        } catch (e: IllegalArgumentException) {
            responseBuilder.status(HttpStatus.BAD_REQUEST)
            e.message
        } catch (e: IllegalStateException) {
            responseBuilder.status(HttpStatus.BAD_REQUEST)
            e.message
        } catch (e: HttpException) {
            responseBuilder.status(e.code)
            e.message
        }

        responseBuilder.body(body)
        val response = responseBuilder.build()

        return response
    }
}