package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClientBuilder
import java.util.*

/**
 * Resposible for storing
 */
object AzureStorage {
    fun getBlobContainer(name: String): BlobContainerClient {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        val blobServiceClient = BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
        val containerClient = blobServiceClient.getBlobContainerClient(name)
        if (!containerClient.exists()) containerClient.create()
        return containerClient
    }

    fun getBlobClient(blobUrl: String): BlobClient {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        return BlobClientBuilder().connectionString(blobConnection).endpoint(blobUrl).buildClient()
    }

    fun createQueueClient(name: String): QueueClient {
        val connectionString = System.getenv("AzureWebJobsStorage")
        return QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .createQueue(name)
    }

    fun uploadMessage(queueName: String, message: String) {
        val base64Message = String(Base64.getEncoder().encode(message.toByteArray()))
        createQueueClient(queueName).sendMessage(base64Message)
    }

    fun downloadMessage(queueName: String): String {
        return createQueueClient(queueName).receiveMessage().messageText
    }
}