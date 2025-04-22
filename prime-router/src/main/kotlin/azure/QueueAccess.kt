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
        sendMessageToQueue(event, queueName)
    }

    fun sendMessageToQueue(
        event: Event,
        queueName: String,
        delay: Duration,
    ) {
        if (delay.isZero || delay.isNegative) {
            return sendMessageToQueue(event, queueName)
        }
        val base64 = Base64.getEncoder()
            .encodeToString(event.toQueueMessage().toByteArray())
        sendMessage(queueName, base64, delay)
    }

    /**
     * Send a message to the queue based on the [event] with a provided queue name.
     *
     * The derived queue name from the [event] is ignored purposely.
     */
    fun sendMessageToQueue(event: Event, queueName: String) {
        val base64Message = String(Base64.getEncoder().encode(event.toQueueMessage().toByteArray()))
        val now = OffsetDateTime.now()
        val invisibleDuration = Duration.between(now, event.at ?: now)
        sendMessage(queueName, base64Message, invisibleDuration)
    }

    /**
     * Send a string [message] to the queue.
     *
     * [invisibleDuration] is the time the message is invisible in the queue (effectively a delay)
     * https://docs.microsoft.com/en-us/java/api/com.azure.storage.queue.queueclient.sendmessagewithresponse?view=azure-java-stable#com-azure-storage-queue-queueclient-sendmessagewithresponse(com-azure-core-util-binarydata-java-time-duration-java-time-duration-java-time-duration-com-azure-core-util-context)
     *
     */
    fun sendMessage(
        queueName: String,
        message: String,
        invisibleDuration: Duration = Duration.ZERO,
    ): String {
        // Bug:  event.at is calculated before the call to workflowengine.recordHistory
        // In cases of very large datasets, that db write can take a very long time, pushing
        // the current time past event.at.  This causes negative durations.  Hence this:
        val duration = if (invisibleDuration.isNegative) {
            Duration.ZERO
        } else {
            invisibleDuration
        }
        val timeToLive = invisibleDuration.plusDays(timeToLiveDays)
        val response = createQueueClient(queueName).sendMessageWithResponse(
            message,
            duration,
            timeToLive,
            null,
            null
        )
        return response.value.messageId
    }

    fun receiveMessage(queueName: String): Event {
        // messageText is deprecated
        val message = createQueueClient(queueName).receiveMessage().body.toString()
        return Event.parsePrimeRouterQueueMessage(message)
    }

    /**
     * Creates the queue client for the given queue [name] or reuses an existing one.
     * @return the queue client
     */
    private fun createQueueClient(name: String): QueueClient = if (clients.containsKey(name)) {
            clients[name]!!
        } else {
            clients[name] = QueueServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .createQueue(name)
            clients[name]!!
        }
}