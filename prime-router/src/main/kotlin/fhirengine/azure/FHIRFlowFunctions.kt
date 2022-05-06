package gov.cdc.prime.router.fhirengine.azure

import com.google.common.net.HttpHeaders
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
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.HttpException
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.UnsupportedMediaTypeException
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.cli.tests.CompareHl7Data
import gov.cdc.prime.router.fhirengine.azure.http.extensions.contentType
import gov.cdc.prime.router.fhirengine.encoding.FHIR
import gov.cdc.prime.router.fhirengine.encoding.HL7
import gov.cdc.prime.router.fhirengine.encoding.encode
import gov.cdc.prime.router.fhirengine.engine.Message
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.translation.fhir.FHIRtoHL7
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7toFHIR
import org.apache.logging.log4j.kotlin.Logging

const val fhirQueueName = "fhir-raw-received"

/**
 * Functions to execution and logging of FHIR translations
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
        val responseBuilder = request.createResponseBuilder(HttpStatus.OK)
        val body = try {
            val requestBody = request.body
            requireNotNull(requestBody)

            val contentType = request.headers.get(HttpHeaders.CONTENT_TYPE.lowercase())
            when (contentType) {
                HttpUtilities.hl7MediaType, "application/hl7-v2", null -> {
                    responseBuilder.contentType(HttpUtilities.fhirMediaType)
                    val hl7messages = HL7.decode(requestBody)
                    require(hl7messages.size > 0) { "No messages were found" }
                    val bundles = hl7messages.map { message ->
                        HL7toFHIR.translate(message)
                    }
                    buildString {
                        bundles.forEach { bundle ->
                            appendLine(bundle.encode())
                        }
                    }
                }
                HttpUtilities.fhirMediaType -> {
                    responseBuilder.contentType(HttpUtilities.hl7MediaType)
                    val bundle = FHIR.decode(requestBody)
                    val message = FHIRtoHL7().translate(bundle)
                    message.encode()
                }
                else -> {
                    throw UnsupportedMediaTypeException("Unknown media type: $contentType")
                }
            }
        } catch (e: IllegalArgumentException) {
            responseBuilder.status(HttpStatus.BAD_REQUEST)
            actionHistory.trackLogs(
                ActionLog(InvalidReportMessage(e.stackTraceToString()), type = ActionLogLevel.error)
            )
            e.message
        } catch (e: IllegalStateException) {
            responseBuilder.status(HttpStatus.BAD_REQUEST)
            actionHistory.trackLogs(
                ActionLog(InvalidReportMessage(e.stackTraceToString()), type = ActionLogLevel.error)
            )
            e.message
        } catch (e: HttpException) {
            responseBuilder.status(e.code)
            actionHistory.trackLogs(
                ActionLog(InvalidReportMessage(e.stackTraceToString()), type = ActionLogLevel.error)
            )
            e.message
        }

        responseBuilder.body(body)
        val response = responseBuilder.build()
        actionHistory.trackActionRequestResponse(request, response)
        workflowEngine.recordAction(actionHistory)

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

        // Encoding here for two reasons:
        // 1. current fhir libraries only correctly reference within
        //    bundles if those bundles are decoded from json
        // 2. Simulates a future state where we save the fhir bundle between steps
        var translatedBundle = HL7toFHIR.translate(hl7.first()).encode()
        val bundle = FHIR.decode(translatedBundle)
        val result = FHIRtoHL7().translate(bundle)
        val encodedResult = result.encode()

        val comparison = compare(String(blobContent), encodedResult)

        val log: String
        val type: ActionLogLevel

        if (!comparison.passed) {
            log = "Failed on message $message\n$comparison"
            type = ActionLogLevel.error
        } else {
            log = "Successfully processed $message"
            type = ActionLogLevel.info
        }

        logger.debug(log)

        actionHistory.trackLogs(
            ActionLog(FhirActionLogDetail(log), type = type)
        )

        actionHistory.trackActionParams(message)

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