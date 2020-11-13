package gov.cdc.prime.router.azure;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Arrays;
import java.util.logging.Logger;

import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import com.azure.storage.blob.*;
import com.microsoft.azure.functions.annotation.StorageAccount;
import org.apache.commons.text.TextStringBuilder;


/**
 * Azure Functions with HTTP Trigger.
 */
public class CsvIngestFunction {
    Logger logger;

    // Storage Connection string.  Assumption is that the queue and the blob go into the 
    // same Azure storage.
    /*
    static final String connectStr =
            "DefaultEndpointsProtocol=https;" +
                    "AccountName=jimhubstorage;" +
                    "AccountKey=Z0zwX3bIZi5pAk489VbGkjILaenhi2WhinmPW6tue+guyhkBgBnFGys7OwAz4j0ecegKY+RDadXDP7I5NieB2A==;" +
                    "EndpointSuffix=core.windows.net";
    */

    static String csvIngestQueueName = "ingested";
    static String csvIngestContainerName = "ingested";

    // Connect to the ingest queue.
    public static QueueClient createQueueClient() throws QueueStorageException {
        String connectStr = System.getenv("AzureWebJobsStorage");
        // String csvIngestQueueName = System.getenv("CsvIngestQueueName");
        return new QueueClientBuilder()
                .connectionString(connectStr)
                .queueName(csvIngestQueueName)
                .buildClient();
    }

    // Connect to the blob container.
    public static BlobContainerClient createBlobClient() {
        String connectStr = System.getenv("AzureWebJobsStorage");
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
        // String csvIngestContainerName = System.getenv("CsvIngestContainerName");
        return blobServiceClient.getBlobContainerClient(csvIngestContainerName);
    }



    /**
     * This function listens at endpoint "/api/csv".
     * Ugly invocation that actually worked for me:
     * curl -X POST --data-binary "@/Users/DKIQ/Documents/code/prime-data-hub/prime-router/src/test/csv_test_files/input/happy-path.csv" "https://jim-hub-20201109.azurewebsites.net/api/csv?filename=happy-path.csv&topic=covid-19&schema-name=pdi-covid-19"
     * Which returns the following:
     * {"filename":"happy-path.csv","topic":"covid-19","schemaName":"pdi-covid-19","action":null,"blobURL":null}
     */
    @FunctionName("csv")
    @StorageAccount("AzureWebJobsStorage")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS)
                    HttpRequestMessage<String> request,
            final ExecutionContext context) {

        logger = context.getLogger();
        logger.info("csv ingest function received an upload request.");

        // Create an object representing the incoming CVS file, and validate presence of required metadata.
        Csv csv;
        try {
            csv = new Csv(request, context);
        } catch (Exception e) {
            TextStringBuilder msgs = new TextStringBuilder();
            Arrays.asList(e.getSuppressed()).forEach(ex -> msgs.appendln(ex.getMessage()));
            logger.severe(msgs.toString());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(msgs.toString()).build();
        }
        logger.info("Successfully read " + csv.getFilename() + ".  Preparing to queue it for processing.");

        // Queue the object for further processing.  Return the metadata json as a receipt to the caller.
        try {
            QueueClient queueClient = createQueueClient();
            BlobContainerClient blobContainerClient = createBlobClient();
            csv.queueForProcessing(queueClient, blobContainerClient, logger);
            logger.info("Successfully queued and stored blob for processing: " + csv.getBlobURL());
            return request.createResponseBuilder(HttpStatus.OK).body(csv.toJson() + "\n").build();
        } catch (Exception e) {
            TextStringBuilder msgs = new TextStringBuilder();
            Arrays.asList(e.getSuppressed()).forEach(ex -> msgs.appendln(ex.getMessage()));
            logger.severe("Error queuing/storing " + csv.getFilename() + ": " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("This CVS failed to get queued for further work.\n").build();
        }
    }
}
