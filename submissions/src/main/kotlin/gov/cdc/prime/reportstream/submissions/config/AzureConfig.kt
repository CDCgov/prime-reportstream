package gov.cdc.prime.reportstream.submissions.config

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AzureConfig {

    @Value("\${azure.storage.connection-string}")
    private lateinit var connectionString: String

    @Value("\${azure.storage.container-name}")
    private lateinit var containerName: String

    @Value("\${azure.storage.queue-name}")
    private lateinit var queueName: String

    @Value("\${azure.storage.table-name}")
    private lateinit var tableName: String

    @Bean
    fun blobContainerClient(): BlobContainerClient {
        val blobServiceClient = BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()

        val blobContainerClient = blobServiceClient.getBlobContainerClient(containerName)

        // Create the container if it doesn't exist
        if (!blobContainerClient.exists()) {
            blobContainerClient.create()
        }

        return blobContainerClient
    }

    @Bean
    fun queueClient(): QueueClient = QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .getQueueClient(queueName)

    @Bean
    fun tableClient(): TableClient {
        val tableServiceClient = TableServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()

        // Ensure the table is created if it does not exist
        tableServiceClient.createTableIfNotExists(tableName)

        return tableServiceClient.getTableClient(tableName)
    }
}