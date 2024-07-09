package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.queue.QueueServiceClient
import com.azure.data.tables.TableClient
import com.azure.data.tables.models.TableEntity
import com.azure.messaging.eventgrid.EventGridEvent
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.models.SendMessageResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.InputStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(SubmissionController::class)
class SubmissionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var blobServiceClient: BlobServiceClient

    @MockBean
    private lateinit var queueServiceClient: QueueServiceClient

    @MockBean
    private lateinit var tableClient: TableClient

    @MockBean
    private lateinit var eventGridPublisherClient: EventGridPublisherAsyncClient<EventGridEvent>

    private val objectMapper = jacksonObjectMapper()

    private lateinit var blobContainerClient: BlobContainerClient
    private lateinit var blobClient: BlobClient
    private lateinit var queueClient: QueueClient
    private lateinit var sendMessageResult: SendMessageResult

    @BeforeEach
    fun setUp() {
        // Mock BlobContainerClient and BlobClient
        blobContainerClient = mockk()
        blobClient = mockk()
        every { blobServiceClient.getBlobContainerClient(any<String>()) } returns blobContainerClient
        every { blobContainerClient.getBlobClient(any<String>()) } returns blobClient

        // Mock QueueClient
        queueClient = mockk()
        every { queueServiceClient.getQueueClient(any<String>()) } returns queueClient

        // Mock sendMessageResult
        sendMessageResult = mockk()
    }

    @Test
    fun `submitReport should return CREATED status`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        // Mock the behavior of the dependencies
        every { blobClient.upload(any<InputStream>(), any<Long>()) } returns Unit
        every { queueClient.sendMessage(any<String>()) } returns sendMessageResult
        every { tableClient.createEntity(any<TableEntity>()) } returns Unit
        every { eventGridPublisherClient.sendEvent(any<EventGridEvent>()) } returns mockk()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client_id", "testClient")
                .header("content-type", "application/hl7-v2")
                .header("payloadname", "testPayload")
                .header("X-Forwarded-For", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        // Verify the interactions with mocks
        verify(exactly = 1) { blobClient.upload(any<InputStream>(), any<Long>()) }
        verify(exactly = 1) { queueClient.sendMessage(any<String>()) }
        verify(exactly = 1) { tableClient.createEntity(any<TableEntity>()) }
        verify(exactly = 1) { eventGridPublisherClient.sendEvent(any<EventGridEvent>()) }
    }
}