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
class CsvIngestFunction {

    /**
     * This function listens at endpoint "/api/csv".
     * Ugly invocation that actually worked for me:
     * curl -X POST --data-binary "@/Users/DKIQ/Documents/code/prime-data-hub/prime-router/src/test/csv_test_files/input/happy-path.csv" "https://jim-hub-20201109.azurewebsites.net/api/csv?filename=happy-path.csv&topic=covid-19&schema-name=pdi-covid-19"
     * Which returns the following:
     * {"filename":"happy-path.csv","topic":"covid-19","schemaName":"pdi-covid-19","action":null,"blobURL":null}
     */
    @FunctionName("csv")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("csv ingest function received an upload request.")

        // Create an object representing the incoming CVS file, and validate presence of required metadata.
        val csv = try {
            Csv(request, context)
        } catch (e: Exception) {
            val msgs = TextStringBuilder()
            Arrays.asList(*e.suppressed).forEach(Consumer { ex: Throwable -> msgs.appendln(ex.message) })
            context.logger.severe(msgs.toString())
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(msgs.toString()).build()
        }
        context.logger.info("Successfully read ${csv.filename}. Preparing to queue it for processing.")

        // Queue the object for further processing.  Return the metadata json as a receipt to the caller.
        return try {
            val queueClient = createQueueClient()
            val blobContainerClient = createBlobClient()
            csv.queueForProcessing(queueClient, blobContainerClient, context)
            context.logger.info("Successfully queued and stored blob for processing: ${csv.blobURL}")
            request.createResponseBuilder(HttpStatus.OK).body(csv.toJson()).build()
        } catch (e: Exception) {
              request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("This CVS failed to get queued for further work.\n").build()
        }
    }

    companion object {
        var csvIngestQueueName = "ingested"
        var csvIngestContainerName = "ingested"

        // Connect to the ingest queue.
        @Throws(QueueStorageException::class)
        fun createQueueClient(): QueueClient {
            val connectStr = System.getenv("AzureWebJobsStorage")
            // String csvIngestQueueName = System.getenv("CsvIngestQueueName");
            return QueueClientBuilder()
                .connectionString(connectStr)
                .queueName(csvIngestQueueName)
                .buildClient()
        }

        // Connect to the blob container.
        fun createBlobClient(): BlobContainerClient {
            val connectStr = System.getenv("AzureWebJobsStorage")
            val blobServiceClient = BlobServiceClientBuilder().connectionString(connectStr).buildClient()
            // String csvIngestContainerName = System.getenv("CsvIngestContainerName");
            return blobServiceClient.getBlobContainerClient(csvIngestContainerName)
        }
    }
}