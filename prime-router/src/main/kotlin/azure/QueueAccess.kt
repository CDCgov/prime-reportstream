package gov.cdc.prime.router.azure

import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClientBuilder
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64

/**
 * Responsible for storing blobs in Azure containers and messages into Azure queues
 */
object QueueAccess {
    /**
     * Queue message time to live.
     */
    private const val timeToLiveDays = 7L

    /**
     * Queue connection string.
     */
    private val connectionString = System.getenv("AzureWebJobsStorage")

    /**
     * Queue clients
     */
    private var clients = mutableMapOf<String, QueueClient>()

    /**
     * Send a message to the queue based on the [event].
     */
    fun sendMessage(event: Event) {
        val queueName = event.eventAction.toQueueName() ?: return
        val base64Message = String(Base64.getEncoder().encode(event.toQueueMessage().toByteArray()))
        val now = OffsetDateTime.now()
        var invisibleDuration = Duration.between(now, event.at ?: now)
        // Bug:  event.at is calculated before the call to workflowengine.recordHistory
        // In cases of very large datasets, that db write can take a very long time, pushing
        // the current time past event.at.  This causes negative durations.  Hence this:
        if (invisibleDuration.isNegative) {
            invisibleDuration = Duration.ZERO
        }
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
        // messageText is deprecated
        val message = createQueueClient(queueName).receiveMessage().body.toString()
        return Event.parseQueueMessage(message)
    }

    /**
     * Creates the queue client for the given queue [name] or reuses an existing one.
     * @return the queue client
     */
    private fun createQueueClient(name: String): QueueClient {
        return if (clients.containsKey(name)) clients[name]!! else {
            clients[name] = QueueServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .createQueue(name)
            clients[name]!!
        }
    }
}