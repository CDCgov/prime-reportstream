package gov.cdc.prime.reportstream.submissions.config

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClientBuilder
import com.microsoft.applicationinsights.TelemetryClient
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
        return BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .getBlobContainerClient(containerName)
    }

    @Bean
    fun queueClient(): QueueClient {
        return QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .getQueueClient(queueName)
    }

    @Bean
    fun tableClient(): TableClient {
        return TableServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .createTableIfNotExists(tableName)
    }

    @Bean
    fun telemetryClient(): TelemetryClient {
        return TelemetryClient()
    }
}