package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.engine.Message
import gov.cdc.prime.router.engine.RawSubmission
import org.apache.logging.log4j.kotlin.Logging

const val fhirQueueName = "fhir-raw-received"

/**
 * Process will work through all the reports waiting in the 'process' queue
 */
class FHIREngine : Logging {
    @FunctionName("process-fhir")
    @StorageAccount("AzureWebJobsStorage")
    fun process(
        @QueueTrigger(name = "message", queueName = fhirQueueName)
        message: String,
        // Number of times this message has been dequeued
        @BindingName("DequeueCount") dequeueCount: Int = 1,
    ) {
        logger.info("FHIR message: $message for the $dequeueCount time")
        val content = Message.deserialize(message)

        check(content is RawSubmission) { "An unknown message was received by the FHIR Engine" }

        val blobContent = content.download()

        logger.info("Got content ${blobContent.size}")

        // val hl7 = deserialize(content) 
        // val fhir = translate(hl7, FHIR)
        // val resultHL7 = translate(fhir, HL7)
        // val result = serialize(resultHL7)

        val result = blobContent
        compare(blobContent, result)
        logger.info("Succesfully handled ${content.digest}")
    }

    fun RawSubmission.download(): ByteArray {
        val blobContent = BlobAccess.downloadBlob(this.blobURL)
        val localDigest = BlobAccess.digestToString(BlobAccess.sha256Digest(blobContent))
        check(this.digest == localDigest) {
            "FHIR - Downloaded file does not match expected file ${this.digest} | $localDigest"
        }
        return blobContent
    }

    internal fun compare(input: ByteArray, output: ByteArray) {
        check(input.contentEquals(output)) { "FHIR - HL7 processing failed" }
    }
}