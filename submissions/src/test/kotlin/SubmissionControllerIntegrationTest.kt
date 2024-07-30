package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.core.util.Context
import com.azure.data.tables.TableClient
import com.azure.data.tables.models.TableEntity
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.models.PeekedMessageItem
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
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
                .content(requestBody)
                .contentType(MediaType.valueOf("application/hl7-v2"))
                .header("client_id", "testClient")
                .header("payloadname", "testPayload")
                .header("x-azure-clientip", "127.0.0.1")
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        // Verify blob was uploaded and read its content
        val blobs: MutableList<BlobItem> = mutableListOf()
        blobContainerClient.listBlobs().iterator().forEachRemaining { blobs.add(it) }
        assertEquals(1, blobs.size)
        val blobContent = blobContainerClient.getBlobClient(blobs[0].name).downloadContent().toString()
        assertEquals(requestBody, blobContent)

        // Verify message was sent to queue and read its content
        val messages: MutableList<PeekedMessageItem> = mutableListOf()
        queueClient.peekMessages(
            10,
            Duration.ofSeconds(30),
            Context.NONE
        ).iterator().forEachRemaining { messages.add(it) }
        assertEquals(1, messages.size)

        val queueMessageContent = objectMapper.readValue(messages[0].body.toString(), Map::class.java) as Map<*, *>
        val headers = queueMessageContent["headers"] as Map<*, *>
        assertEquals("testClient", headers["client_id"])
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
}