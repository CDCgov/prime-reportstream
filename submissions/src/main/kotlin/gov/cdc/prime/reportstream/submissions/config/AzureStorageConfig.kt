package gov.cdc.prime.reportstream.submissions.config

import com.azure.data.tables.TableClient
import com.azure.data.tables.TableClientBuilder
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

    @Value("\${azure.storage.table-name}")
    private lateinit var tableName: String

    @Bean
    fun blobServiceClient(): BlobServiceClient = BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()

    @Bean
    fun queueServiceClient(): QueueServiceClient = QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()

    @Bean
    fun tableClient(): TableClient = TableClientBuilder()
            .connectionString(connectionString)
            .tableName(tableName)
            .buildClient()

    @Bean
    fun containerName(): String = containerName

    @Bean
    fun queueName(): String = queueName

    @Bean
    fun tableName(): String = tableName
}