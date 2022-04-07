package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.FhirActionLogDetail
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.http.extensions.contentType
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.cli.tests.CompareHl7Data
import gov.cdc.prime.router.encoding.HL7
import gov.cdc.prime.router.encoding.encode
import gov.cdc.prime.router.engine.Message
import gov.cdc.prime.router.engine.RawSubmission
import gov.cdc.prime.router.translation.HL7toFHIR
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

const val fhirQueueName = "fhir-raw-received"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class FHIRFlowFunctions(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.process)
) : Logging {
    /**
     * An azure function for processing an HL7 message into of FHIR
     */
    @FunctionName("convert")
    @StorageAccount("AzureWebJobsStorage")
    fun convert(
        @HttpTrigger(
            name = "convert",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "\$convert-data"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val responseBuilder = request.createResponseBuilder(HttpStatus.BAD_REQUEST)
        try {
            val hl7messages = HL7.decode(request.body)
            val body = buildString {
                hl7messages.forEach { message ->
                    val fhir: Bundle = HL7toFHIR.translate(message)
                    appendLine(fhir.encode())
                }
            }
            responseBuilder.status(HttpStatus.OK)
            responseBuilder.contentType(HttpUtilities.fhirMediaType)
            responseBuilder.body(body)
        } catch (e: IllegalArgumentException) {
            responseBuilder.body(e.message)
            actionHistory.trackLogs(
                ActionLog(InvalidReportMessage(e.stackTraceToString()), type = ActionLogLevel.error)
            )
        }

        val response = responseBuilder.build()
        actionHistory.trackActionRequestResponse(request, response)

        return response
    }

    /**
     * An azure function for processing an HL7 message into and out of FHIR
     */
    @FunctionName("process-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun process(
        @QueueTrigger(name = "message", queueName = fhirQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.debug("FHIR message: $message for the $dequeueCount time")
        val content = Message.deserialize(message)

        check(content is RawSubmission) {
            "An unknown message was received by the FHIR Engine ${content.javaClass.kotlin.qualifiedName}"
        }

        val blobContent = content.download()

        logger.debug("Got content ${blobContent.size}")
        // TODO behind an interface?
        val hl7 = HL7.decode(String(blobContent))
        val result = hl7.first().encode()

        val log: String
        val type: ActionLogLevel

        val comparison = compare(String(blobContent), result)
        if (!comparison.passed) {
            log = "Failed on message $message\n$comparison"
            type = ActionLogLevel.error
        } else {
            log = "Successfully processed $message"
            type = ActionLogLevel.info
        }

        logger.debug(log)

        actionHistory.trackActionParams(message)

        actionHistory.trackLogs(
            ActionLog(FhirActionLogDetail(log), type = type)
        )

        workflowEngine.recordAction(actionHistory)
    }

    /**
     * Download the file associated with a RawSubmission message
     */
    fun RawSubmission.download(): ByteArray {
        val blobContent = BlobAccess.downloadBlob(this.blobURL)
        val localDigest = BlobAccess.digestToString(BlobAccess.sha256Digest(blobContent))
        check(this.digest == localDigest) {
            "FHIR - Downloaded file does not match expected file\n${this.digest} | $localDigest"
        }
        return blobContent
    }

    /**
     * Compare two hl7 messages encoded as strings
     */
    internal fun compare(input: String, output: String): CompareData.Result {
        return CompareHl7Data().compare(input.byteInputStream(), output.byteInputStream())
    }
}