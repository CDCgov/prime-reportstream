package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.models.SendMessageResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.reportstream.shared.QueueMessage.ObjectMapperProvider
import gov.cdc.prime.reportstream.shared.auth.AuthZService
import gov.cdc.prime.reportstream.submissions.TelemetryService
import gov.cdc.prime.reportstream.submissions.config.AllowedParametersConfig
import gov.cdc.prime.reportstream.submissions.config.SecurityConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64
import java.util.UUID

@WebMvcTest(SubmissionController::class)
@Import(SecurityConfig::class, AllowedParametersConfig::class)
class SubmissionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var blobContainerClient: BlobContainerClient

    @Autowired
    private lateinit var queueClient: QueueClient

    @Autowired
    private lateinit var tableClient: TableClient

    @Autowired
    private lateinit var telemetryService: TelemetryService

    @Autowired
    private lateinit var authZService: AuthZService

    @TestConfiguration
    class Config {
        @Bean
        fun blobContainerClient(): BlobContainerClient = mock()

        @Bean
        fun queueClient(): QueueClient = mock()

        @Bean
        fun tableClient(): TableClient = mock()

        @Bean
        fun telemetryService(): TelemetryService = mock()

        @Bean
        fun authZService(): AuthZService = mock()
    }

    private lateinit var objectMapper: ObjectMapper

    private lateinit var blobClient: BlobClient
    private lateinit var sendMessageResult: SendMessageResult

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        // Mock BlobContainerClient and BlobClient
        blobClient = mock(BlobClient::class.java)
        `when`(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient)

        // Ensure blobUrl is mocked properly
        `when`(blobClient.blobUrl).thenReturn("https://example.com/blobUrl")

        // Mock the upload method
        doNothing().`when`(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())

        // Mock QueueClient
        sendMessageResult = mock(SendMessageResult::class.java)
        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)

        // Mock the table createEntity method
        doNothing().`when`(tableClient).createEntity(any())

        // Ensure telemetryService methods are mocked
        doNothing().`when`(telemetryService).trackEvent(anyString(), anyMap())
        doNothing().`when`(telemetryService).flush()
    }

    @AfterEach
    fun tearDown() {
        // Reset all mocks after each test to ensure isolation
        reset(
            blobClient,
            blobContainerClient,
            queueClient,
            tableClient,
            telemetryService,
        )
    }

    @Test
    fun `submitReport should return CREATED status with content-type hl7-v2 and correct arguments`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)
        val expectedBlobUrl = "https://example.com/blobUrl"

        // Mock the UUID generation to ensure a predictable report ID
        val reportId = UUID.randomUUID()
        val uuidMockedStatic = mockStatic(UUID::class.java)
        uuidMockedStatic.`when`<UUID> { UUID.randomUUID() }.thenReturn(reportId)

        // Capture the arguments passed to the upload and sendMessage methods
        val blobInputStreamCaptor = argumentCaptor<InputStream>()
        val blobSizeCaptor = argumentCaptor<Long>()
        val messageCaptor = argumentCaptor<String>()

        `when`(blobClient.blobUrl).thenReturn(expectedBlobUrl)
        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)
        `when`(authZService.isSenderAuthorized(org.mockito.kotlin.any(), org.mockito.kotlin.any<(String) -> String>()))
            .thenReturn(true)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        verify(blobClient).upload(blobInputStreamCaptor.capture(), blobSizeCaptor.capture())
        verify(queueClient).sendMessage(messageCaptor.capture())

        // Assert the captured arguments
        assert(blobSizeCaptor.firstValue == requestBody.length.toLong())
        val capturedMessage = deserialize(messageCaptor.firstValue, QueueMessage.ReceiveQueueMessage::class.java)
        assert(capturedMessage.reportId == reportId)
        assert(capturedMessage.blobURL == expectedBlobUrl)
        val headers = capturedMessage.headers as Map<*, *>
        assert(headers["client_id"] == "testClient")
        assert(headers["Content-Type"] == "application/hl7-v2;charset=UTF-8")
        assert(headers["payloadname"] == "testPayload")

        uuidMockedStatic.close()
    }

    @Test
    fun `submitReport should return CREATED status with content-type fhir+ndjson and correct arguments`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)
        val expectedBlobUrl = "https://example.com/blobUrl"

        // Mock the UUID generation to ensure a predictable report ID
        val reportId = UUID.randomUUID()

        val uuidMockedStatic = mockStatic(UUID::class.java)
        uuidMockedStatic.`when`<UUID> { UUID.randomUUID() }.thenReturn(reportId)

        // Capture the arguments passed to the upload and sendMessage methods
        val blobInputStreamCaptor = argumentCaptor<InputStream>()
        val blobSizeCaptor = argumentCaptor<Long>()
        val messageCaptor = argumentCaptor<String>()

        `when`(blobClient.blobUrl).thenReturn(expectedBlobUrl)
        `when`(queueClient.sendMessage(anyString())).thenReturn(sendMessageResult)
        `when`(authZService.isSenderAuthorized(org.mockito.kotlin.any(), org.mockito.kotlin.any<(String) -> String>()))
            .thenReturn(true)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
                .content(requestBody)
                .contentType(MediaType.valueOf("application/fhir+ndjson"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        verify(blobClient).upload(blobInputStreamCaptor.capture(), blobSizeCaptor.capture())
        verify(queueClient).sendMessage(messageCaptor.capture())

        // Assert the captured arguments
        assert(blobSizeCaptor.firstValue == requestBody.length.toLong())
        val capturedMessage = deserialize(messageCaptor.firstValue, QueueMessage.ReceiveQueueMessage::class.java)
        assert(capturedMessage.reportId == reportId)
        assert(capturedMessage.blobURL == expectedBlobUrl)
        val headers = capturedMessage.headers as Map<*, *>
        assert(headers["client_id"] == "testClient")
        assert(headers["Content-Type"] == "application/fhir+ndjson;charset=UTF-8")
        assert(headers["payloadname"] == "testPayload")

        uuidMockedStatic.close()
    }

    @Test
    fun `submitReport should return UNSUPPORTED_MEDIA_TYPE status with unsupported content-type`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
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

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                .string("Bad Request: Missing required header: client_id")
            )
    }

    @Test
    fun `submitReport should return INTERNAL_SERVER_ERROR on blob storage failure`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(authZService.isSenderAuthorized(org.mockito.kotlin.any(), org.mockito.kotlin.any<(String) -> String>()))
            .thenReturn(true)
        doThrow(RuntimeException("Blob storage failure"))
            .`when`(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
                .content(requestBody)
                .contentType(MediaType.parseMediaType("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(MockMvcResultMatchers.content().string("Internal Server Error: Blob storage failure"))

        verify(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
    }

    @Test
    fun `submitReport should return INTERNAL_SERVER_ERROR on queue service failure`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        `when`(authZService.isSenderAuthorized(org.mockito.kotlin.any(), org.mockito.kotlin.any<(String) -> String>()))
            .thenReturn(true)
        doNothing().`when`(blobClient).upload(any(ByteArrayInputStream::class.java), anyLong())
        doThrow(RuntimeException("Queue service failure")).`when`(queueClient).sendMessage(anyString())

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
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
    fun `submitReport should log SUBMISSION_RECEIVED with correct details`() {
        // Helper function to safely cast the captured map to Map<String, String>
        fun mapToStringString(input: Map<*, *>): Map<String, String> = input.mapNotNull { (key, value) ->
                val stringKey = key as? String
                val stringValue = value as? String
                if (stringKey != null && stringValue != null) {
                    stringKey to stringValue
                } else {
                    null
                }
            }.toMap()

        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)
        val expectedBlobUrl = "https://example.com/blobUrl"
        val reportId = UUID.randomUUID()

        // Mock the UUID generation to ensure a predictable report ID
        val uuidMockedStatic = mockStatic(UUID::class.java)
        uuidMockedStatic.`when`<UUID> { UUID.randomUUID() }.thenReturn(reportId)
        `when`(authZService.isSenderAuthorized(org.mockito.kotlin.any(), org.mockito.kotlin.any<(String) -> String>()))
            .thenReturn(true)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
                .queryParam("processing", "test1", "test2")
                .queryParam("test", "test2")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        val eventCaptor = argumentCaptor<String>()
        val propertiesCaptor = argumentCaptor<Map<String, String>>()

        verify(telemetryService).trackEvent(eventCaptor.capture(), propertiesCaptor.capture())

        val capturedEvent = eventCaptor.firstValue
        val capturedProperties = mapToStringString(propertiesCaptor.firstValue)

        assert(capturedEvent == "SUBMISSION_RECEIVED")
        val eventDetails = objectMapper.readValue(capturedProperties["event"], Map::class.java)
        assert(eventDetails["reportId"] == reportId.toString())
        assert(eventDetails["blobUrl"] == expectedBlobUrl)
        assert(eventDetails["senderIp"] == "127.0.0.1")
        assert(eventDetails["method"] == "POST")
        assert(eventDetails["senderName"] == "testClient")
        assert(eventDetails["pipelineStepName"] == "submission")
        assert(eventDetails["url"] == "http://localhost/api/v1/reports")
        val requestParameters = eventDetails["requestParameters"] as Map<*, *>
        val headers = requestParameters["headers"] as Map<*, *>
        assert(headers["client_id"] == "testClient")
        assert(headers["Content-Type"] == "application/hl7-v2;charset=UTF-8")
        assert(headers["payloadname"] == "testPayload")
        assert(headers["x-azure-clientip"] == "127.0.0.1")
        val queryParameters = requestParameters["queryParameters"] as Map<*, *>
        assert(queryParameters.isEmpty())

        uuidMockedStatic.close()
    }

    fun <T> deserialize(serializedString: String, valueType: Class<T>): T {
        val bytes = Base64.getDecoder().decode(serializedString)
        return ObjectMapperProvider.mapper.readValue(bytes, valueType)
    }
}