package gov.cdc.prime.router.azure

import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClientBuilder
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64

/**
 * Responsible for storing blobs in Azure containers and messages into Azure queues
 */
class QueueAccess {
    val timeToLiveDays = 7L

    fun sendMessage(event: Event) {
        val queueName = event.eventAction.toQueueName() ?: return
        val base64Message = String(Base64.getEncoder().encode(event.toQueueMessage().toByteArray()))
        val invisibleDuration = Duration.between(OffsetDateTime.now(), event.at ?: OffsetDateTime.now())
        val timeToLive = invisibleDuration.plusDays(timeToLiveDays)
        createQueueClient(queueName).sendMessageWithResponse(
            base64Message,
            invisibleDuration,
            timeToLive,
            null,
            null
        )
    }

    fun receiveMessage(queueName: String): Event {
        val message = createQueueClient(queueName).receiveMessage().messageText
        return Event.parseQueueMessage(message)
    }

    private fun createQueueClient(name: String): QueueClient {
        val connectionString = System.getenv("AzureWebJobsStorage")
        return QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .createQueue(name)
    }
}