package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.messaging.eventgrid.EventGridEvent
import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClient
import com.azure.storage.queue.models.SendMessageResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream

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
        blobContainerClient = mock(BlobContainerClient::class.java)
        blobClient = mock(BlobClient::class.java)
        `when`(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient)
        `when`(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient)

        // Mock QueueClient
        queueClient = mock(QueueClient::class.java)
        `when`(queueServiceClient.getQueueClient(anyString())).thenReturn(queueClient)

        // Mock SendMessageResult
        sendMessageResult = mock(SendMessageResult::class.java)

        // Ensure blobUrl is mocked properly
        `when`(blobClient.blobUrl).thenReturn("https://example.com/blobUrl")

        // Mock the upload method
        doNothing().`when`(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())

        // Mock the sendEvent method
        `when`(eventGridPublisherClient.sendEvent(any(EventGridEvent::class.java))).thenReturn(Mono.empty())
    }

    @Test
    fun `submitReport should return CREATED status`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("X-Forwarded-For", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        verify(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
        verify(queueClient).sendMessage(anyString())
    }
}