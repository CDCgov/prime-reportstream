package gov.cdc.prime.router.azure

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
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.cli.tests.CompareHl7Data
import gov.cdc.prime.router.encoding.FHIR
import gov.cdc.prime.router.encoding.HL7
import gov.cdc.prime.router.encoding.encode
import gov.cdc.prime.router.engine.Message
import gov.cdc.prime.router.engine.RawSubmission
import org.apache.logging.log4j.kotlin.Logging

const val fhirQueueName = "fhir-raw-received"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class FHIRFlowFunctions : Logging {
    /**
     * An azure function for processing an HL7 message into of FHIR
     */
    @FunctionName("translate-hl7-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun receive(
        @HttpTrigger(
            name = "translate",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "translate"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val responseBuilder = request.createResponseBuilder(HttpStatus.BAD_REQUEST)
        try {
            val hl7messages = HL7.deserialize(request.body!!)
            val body = buildString {
                hl7messages.forEach { hl7 ->
                    val fhir = FHIR.translate(hl7)
                    appendLine(fhir.encode())
                }
            }
            responseBuilder.status(HttpStatus.OK)
            responseBuilder.header(HttpHeaders.CONTENT_TYPE, "application/fhir+json")
            responseBuilder.body(body)
        } catch (e: IllegalArgumentException) {
            responseBuilder.status(HttpStatus.BAD_REQUEST)
            responseBuilder.body(e.message)
        }
        return responseBuilder.build()
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
        val hl7 = HL7.deserialize(String(blobContent))
        val result = hl7.first().encode()

        val comparison = compare(String(blobContent), result)
        if (!comparison.passed) {
            logger.debug("Failed on message $message\n$comparison")
        } else {
            logger.debug("Successfully processed $message")
        }
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