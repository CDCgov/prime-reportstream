package gov.cdc.prime.reportstream.submissions.azure

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.queue.QueueServiceClient
import com.azure.storage.queue.QueueServiceClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AzureStorageConfig {

    @Value("\${azure.storage.connection-string}")
    private lateinit var connectionString: String

    @Value("\${azure.storage.container-name}")
    private lateinit var containerName: String

    @Value("\${azure.storage.queue-name}")
    private lateinit var queueName: String

    @Bean
    fun blobServiceClient(): BlobServiceClient {
        return BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
    }

    @Bean
    fun containerName(): String {
        return containerName
    }

    @Bean
    fun queueServiceClient(): QueueServiceClient {
        return QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
    }

    @Bean
    fun queueName(): String {
        return queueName
    }
}