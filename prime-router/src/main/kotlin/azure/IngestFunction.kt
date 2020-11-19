package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueClientBuilder
import com.azure.storage.queue.models.QueueStorageException
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import org.apache.commons.text.TextStringBuilder
import java.util.*
import java.util.function.Consumer
import java.util.logging.Logger

/**
 * Azure Functions with HTTP Trigger.
 */
class IngestFunction {

    /**
     * This function listens at endpoint "/api/report".
     * Run ./test-ingest.sh to get an example curl call that runs this function.
     * That curl returns something like the following upon success:
     *    {"filename":"lab1-test_results-17-42-31.csv","topic":"covid-19","schema":"pdi-covid-19.schema","action":"","blobURL":"http://azurite:10000/devstoreaccount1/ingested/lab1-test_results-17-42-31-pdi-covid-19.schema-3ddef736-55e1-4a45-ac41-f74086aaa654.csv"}
     */
    @FunctionName("report")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("report ingest function received an upload request.")

        // Create an object representing the incoming CVS file, and validate presence of required metadata.
        val file = try {
            IngestedFile(request, context)
        } catch (e: Exception) {
            val msgs = TextStringBuilder()
            e.suppressedExceptions.forEach{ msgs.appendln(it.message) }
            context.logger.severe(msgs.toString())
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(msgs.toString()).build()
        }
        context.logger.info("Successfully read ${file.filename}. Preparing to queue it for processing.")

        // Queue the object for further processing.  Return the metadata json as a receipt to the caller.
        return try {
            val queueClient = createQueueClient()
            val blobContainerClient = createBlobClient()
            file.queueForProcessing(queueClient, blobContainerClient, context)
            context.logger.info("Successfully queued and stored blob for processing: ${file.blobURL}")
            request.createResponseBuilder(HttpStatus.OK).body(file.toJson()).build()
        } catch (e: Exception) {
              request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("This CVS failed to get queued for further work.\n").build()
        }
    }

    companion object {
        var ingestQueueName = "ingested"
        var ingestContainerName = "ingested"

        // Connect to the ingest queue.
        @Throws(QueueStorageException::class)
        fun createQueueClient(): QueueClient {
            val connectStr = System.getenv("AzureWebJobsStorage")
            return QueueClientBuilder()
                .connectionString(connectStr)
                .queueName(ingestQueueName)
                .buildClient()
        }

        // Connect to the blob container.
        fun createBlobClient(): BlobContainerClient {
            val connectStr = System.getenv("AzureWebJobsStorage")
            val blobServiceClient = BlobServiceClientBuilder().connectionString(connectStr).buildClient()
            return blobServiceClient.getBlobContainerClient(ingestContainerName)
        }
    }
}