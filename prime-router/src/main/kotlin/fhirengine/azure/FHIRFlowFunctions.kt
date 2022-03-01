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
import gov.cdc.prime.router.azure.http.extensions.contentType
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.cli.tests.CompareHl7Data
import gov.cdc.prime.router.encoding.FHIR
import gov.cdc.prime.router.encoding.HL7
import gov.cdc.prime.router.encoding.encode
import gov.cdc.prime.router.engine.Message
import gov.cdc.prime.router.engine.RawSubmission
import gov.cdc.prime.router.translation.FHIRtoHL7
import gov.cdc.prime.router.translation.HL7toFHIR
import gov.cdc.prime.router.translation.translate
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

const val fhirQueueName = "fhir-raw-received"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class FHIRFlowFunctions : Logging {
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
            val contentType = request.headers.get(HttpHeaders.CONTENT_TYPE.lowercase())
            when (contentType) {
                HttpUtilities.fhirMediaType, null -> {
                    val hl7messages = HL7.decode(request.body)
                    responseBuilder.contentType(HttpUtilities.fhirMediaType)
                    buildString {
                        hl7messages.forEach { message ->
                            val fhir: Bundle = HL7toFHIR.translate(message)
                            appendLine(fhir.encode())
                        }
                    }
                }
                HttpUtilities.hl7MediaType, "application/hl7-v2" -> {
                    responseBuilder.contentType(HttpUtilities.hl7MediaType)
                    val requestBody = request.body
                    requireNotNull(requestBody)
                    val bundle = FHIR.decode(requestBody)
                    // FHIRtoHL7.templateHL7(bundle)
                    val message = FHIRtoHL7.toORU_R01(bundle)
                    message.encode()
                }
                else -> {
                    throw UnsupportedMediaTypeException("Unknown media type: $contentType")
                }
            }
        } catch (e: IllegalArgumentException) {
            responseBuilder.status(HttpStatus.BAD_REQUEST)
            e.message
        } catch (e: HttpException) {
            responseBuilder.status(e.code)
            e.message
        }
        responseBuilder.body(body)
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
        val hl7 = HL7.decode(String(blobContent))
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