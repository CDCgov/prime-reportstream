package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.queue.QueueServiceClient
import com.azure.data.tables.TableClient
import com.azure.messaging.eventgrid.EventGridEvent
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.models.SendMessageResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.reportstream.submissions.validators.ClientIdValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(SubmissionController::class)
@Import(ClientIdValidator::class)
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
    }

    @Test
    fun `submitReport should return CREATED status`() {

        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("X-Forwarded-For", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
    }
}
