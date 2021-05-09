package gov.cdc.prime.router.azure

import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClientBuilder
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64

const val TIME_TO_LIVE_DAYS = 7L
/**
 * Responsible for storing blobs in Azure containers and messages into Azure queues
 */
class QueueAccess {
    fun sendMessages(events: List<Event>) {
        events
            .groupBy { it.eventAction.toQueueName() ?: error("Expected a queue for this action") }
            .forEach { (queueName, eventsForQueue) ->
                val client = createQueueClient(queueName)
                eventsForQueue.forEach {
                    sendOne(client, it)
                }
            }
    }

    fun sendMessage(event: Event) {
        val queueName = event.eventAction.toQueueName() ?: error("Expected a queue for this action")
        val client = createQueueClient(queueName)
        sendOne(client, event)
    }

    private fun sendOne(client: QueueClient, event: Event) {
        val base64Message = String(Base64.getEncoder().encode(event.toQueueMessage().toByteArray()))
        val invisibleDuration = Duration.between(OffsetDateTime.now(), event.at ?: OffsetDateTime.now())
        val timeToLive = invisibleDuration.plusDays(TIME_TO_LIVE_DAYS)
        client.sendMessageWithResponse(
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