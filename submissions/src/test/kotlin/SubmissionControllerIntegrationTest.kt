package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.data.tables.models.TableEntity
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.queue.QueueClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.reportstream.shared.QueueMessage.ObjectMapperProvider
import gov.cdc.prime.reportstream.submissions.config.AzureConfig
import gov.cdc.prime.reportstream.submissions.config.SecurityConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.Base64

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AzureConfig::class, SecurityConfig::class)
class SubmissionControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var blobContainerClient: BlobContainerClient

    @Autowired
    private lateinit var queueClient: QueueClient

    @Autowired
    private lateinit var tableClient: TableClient

    private lateinit var objectMapper: ObjectMapper

    companion object {
        private val azuriteContainer = GenericContainer(
            DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
        )
            .withExposedPorts(10000, 10001, 10002)

//        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            azuriteContainer.start()
            val blobEndpoint = "http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10000)}"
            val queueEndpoint = "http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10001)}"
            val tableEndpoint = "http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10002)}"
            val connectionString = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vd" +
                "M02xNOcqFeW5vUwJ2F1rU0A8NHU6eT9+iU7Jk+E6i0z4lIFIKVzjze7d4i/ASTQ5C1R9F6rmTDL6wFg==;" +
                "BlobEndpoint=$blobEndpoint/devstoreaccount1;QueueEndpoint=$queueEndpoint;" +
                "TableEndpoint=$tableEndpoint/devstoreaccount1;"

            registry.add("azure.storage.connection-string") { connectionString }
        }
    }

    @BeforeEach
    fun setUp() {
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        if (!blobContainerClient.exists()) {
            blobContainerClient.create()
        }
    }

    @AfterEach
    fun tearDown() {
        blobContainerClient.deleteIfExists()
        queueClient.clearMessages()
        tableClient.deleteTable()
    }

    @Test
    fun `submitReport should return CREATED status and store data in Azurite`() {
        val data = mapOf("key" to "value")
        val requestBody = objectMapper.writeValueAsString(data)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/reports")
                .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_sender")))
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "org.test")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
                .header(
                    "Okta-Groups",
                    "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhcHBJZCIsIm5iZiI6MTczMDkxMTczMiwiaXNzIjoiSSdtIHRoZSBpc3N1ZXIiLCJ" +
                        "ncm91cHMiOlsiREhTZW5kZXJfb3JnIl0sImV4cCI6NDg4NDUxMTczNywiaWF0IjoxNzMwOTExNzM3LCJqdGkiOiI1YjA" +
                        "5MjhjMC1jMDZmLTQ5OGItOWFmZS1kZDEwODJlNDliMmIifQ.EP3v_kCzWGTWIhhibwTWSzQGMSYVvogbqvrLiwSbTD0X" +
                        "ADRhiBlD4AIJwa_aUp9Zxnc6fbNKPIHWydzYZNUzzMmRkIzSYfmcj1oRjvf0HiXqw-8tSBT1sTBOlpGxWpTuPPnvV9A7" +
                        "ZqqJ614v8x_NyxPdswOdfFpgtSb_nDFLaLR3Tzo5A0JFeNWtlOd8U2gp6a57vggCFt9vDMhrOq8QC6gYJPUn1u7Z_Xfd" +
                        "C1XSm7r3DwcItMbqtVVY1ngixMI7CB0bChcJPgHI37P03IMsVscFrXlPPwxSUkdAe1xZW9w9i0-sI7iLIy78k4gMMXgH" +
                        "W64oopgua3Fdalo-LhDsJA"
                )
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        // Verify blob was uploaded and read its content
        val blobs: MutableList<BlobItem> = mutableListOf()
        blobContainerClient.listBlobs().iterator().forEachRemaining { blobs.add(it) }
        assertEquals(1, blobs.size)
        val blobContent = blobContainerClient.getBlobClient(blobs[0].name).downloadContent().toString()
        assertEquals(requestBody, blobContent)

        // Verify message was sent to queue and read its content
        // Peek the message from the queue
        val peekedMessage = queueClient.peekMessage()?.body?.toString()

        // Check if message is present
        checkNotNull(peekedMessage) { "No message found in the queue" }

        // Deserialize the message
        val deserializedMessage = deserialize(peekedMessage, QueueMessage.ReceiveQueueMessage::class.java)

//        val queueMessageContent = objectMapper.readValue(/* content = */ messages[0].body.toString(), /* valueType = */
//            QueueMessage.ReceiveQueueMessage::class.java)
        val headers = deserializedMessage.headers as Map<*, *>
        assertEquals("org.test", headers["client_id"])
        assertEquals("application/hl7-v2;charset=UTF-8", headers["Content-Type"])
        assertEquals("testPayload", headers["payloadname"])
        assertEquals("127.0.0.1", headers["x-azure-clientip"])

        // Verify entity was added to table and read its content
        val entities: MutableList<TableEntity> = mutableListOf()
        tableClient.listEntities().iterator().forEachRemaining { entities.add(it) }
        assertEquals(1, entities.size)
        val tableEntity = entities[0]
        assertEquals("Received", tableEntity.getProperty("RowKey"))
    }

    fun <T> deserialize(serializedString: String, valueType: Class<T>): T {
        val bytes = Base64.getDecoder().decode(serializedString)
        return ObjectMapperProvider.mapper.readValue(bytes, valueType)
    }
}