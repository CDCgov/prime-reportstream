package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueServiceClient
import com.azure.storage.queue.models.SendMessageResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import gov.cdc.prime.reportstream.submissions.config.AzureConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
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
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.AfterEach
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.Import

@WebMvcTest(SubmissionController::class)
@Import(AzureConfig::class)
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
    private lateinit var telemetryClient: TelemetryClient

    private lateinit var objectMapper: ObjectMapper

    private lateinit var blobContainerClient: BlobContainerClient
    private lateinit var blobClient: BlobClient
    private lateinit var queueClient: QueueClient
    private lateinit var sendMessageResult: SendMessageResult

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

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

        // Mock the queue sendMessage method
        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        // Mock the table createEntity method
        doNothing().`when`(tableClient).createEntity(any())

        // Ensure telemetryClient methods are mocked
        doNothing().`when`(telemetryClient).trackEvent(anyString(), anyMap(), isNull())
        doNothing().`when`(telemetryClient).flush()
    }

    @AfterEach
    fun tearDown() {
        // Reset all mocks after each test to ensure isolation
        reset(
            blobServiceClient,
            queueServiceClient,
            tableClient,
            telemetryClient,
            blobContainerClient,
            blobClient,
            queueClient
        )
    }


    @Test
    fun `submitReport should return CREATED status with content-type hl7-v2`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        verify(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
        verify(queueClient).sendMessage(anyString())
        verify(tableClient).createEntity(any())
        verify(telemetryClient).trackEvent(anyString(), anyMap(), isNull())
        verify(telemetryClient).flush()
    }

    @Test
    fun `submitReport should return CREATED status with content-type fhir+ndjson`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.valueOf("application/fhir+ndjson"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        verify(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
        verify(queueClient).sendMessage(anyString())
        verify(telemetryClient).trackEvent(anyString(), anyMap(), isNull())
        verify(telemetryClient).flush()
    }

    @Test
    fun `submitReport should return UNSUPPORTED_MEDIA_TYPE status with unsupported content-type`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType)
    }

    @Test
    fun `submitReport should return BAD_REQUEST status when client_id is missing`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `submitReport should return INTERNAL_SERVER_ERROR on blob storage failure`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        doThrow(RuntimeException("Blob storage failure"))
            .`when`(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.parseMediaType("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)

        verify(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
    }

    @Test
    fun `submitReport should return INTERNAL_SERVER_ERROR on queue service failure`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        doNothing().`when`(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
        doThrow(RuntimeException("Queue service failure")).`when`(queueClient).sendMessage(anyString())

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.parseMediaType("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)

        verify(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
        verify(queueClient).sendMessage(anyString())
    }

    @Test
    fun `submitReport should log ReportReceivedEvent with correct details`() {
        // Helper function to safely cast the captured map to Map<String, String>
        fun mapToStringString(input: Map<*, *>): Map<String, String> {
            return input.mapNotNull { (key, value) ->
                val stringKey = key as? String
                val stringValue = value as? String
                if (stringKey != null && stringValue != null) {
                    stringKey to stringValue
                } else {
                    null
                }
            }.toMap()
        }

        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)
        val expectedBlobUrl = "https://example.com/blobUrl"

        whenever(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        val eventCaptor = argumentCaptor<String>()
        val propertiesCaptor = argumentCaptor<Map<String, String>>()

        verify(telemetryClient).trackEvent(eventCaptor.capture(), propertiesCaptor.capture(), isNull())

        val capturedEvent = eventCaptor.firstValue
        val capturedProperties = mapToStringString(propertiesCaptor.firstValue)

        assert(capturedEvent == "ReportReceivedEvent")
        val eventDetails = objectMapper.readValue(capturedProperties["event"], Map::class.java)
        assert(eventDetails["reportId"] != null)
        assert(eventDetails["blobUrl"] == expectedBlobUrl)
        assert(eventDetails["headers"] != null)
        assert(eventDetails["senderIP"] == "127.0.0.1")
    }
}