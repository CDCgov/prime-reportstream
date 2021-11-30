package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.GAENTransportType
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Logger

class GAENTransportIntegrationTests {
    private val context = mockkClass(ExecutionContext::class)
    private val metadata = Metadata.getInstance()
    private val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    private val logger = mockkClass(Logger::class)
    private val gaenTransport = spyk<GAENTransport>()
    private val reportId = UUID.randomUUID()
    private val transportType = GAENTransportType("http://localhost:3000")
    private val successJson = """
        {"padding":"-",
        "uuid":"50d5e748-ffb0-4ac2-8149-1b246c1e1696",
        "code":"58900711",
        "expiresAt":"Fri, 05 Nov 2021 18:15:13 UTC",
        "expiresAtTimestamp":1636136113,"longExpiresAt":"Sat, 06 Nov 2021 18:00:13 UTC",
        "longExpiresAtTimestamp":1636221613}
    """.trimIndent()
    private val maintenanceJson = """
        {
            "error": "The server is temporarily down for maintenance. Wait and retry later.",
            "errorCode": "maintenance_mode",
        }
    """.trimIndent()
    private val errorJson = """
        {
            "error": "The provided test or symptom date, was older or newer than the realm allows.",
            "errorCode": "invalid_date",
        }
    """.trimIndent()

    private val task = Task(
        reportId,
        TaskAction.send,
        null,
        "covid-19-gaen",
        "wa-phd.gaen",
        1,
        "",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )

    private val reportFile = ReportFile(
        reportId,
        null,
        TaskAction.send,
        null,
        null,
        null,
        "wa-phd",
        "gaen",
        null, null, "covid-19-gaen", null, null, null, null, null,
        4, // pretend we have 4 items to send.
        null, OffsetDateTime.now(), null
    )

    private fun setupLogger() {
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { logger.warning(any<String>()) }.returns(Unit)
        every { logger.severe(any<String>()) }.returns(Unit)
        every { logger.info(any<String>()) }.returns(Unit)
    }

    private fun makeHeader(): WorkflowEngine.Header {
        val content = """
            abnormal_flag,message_id,illness_onset,date_result_released,patient_phone_number
            A,064545,2021-06-04,2021-06-03,+12896189225
        """.trimIndent()
        return WorkflowEngine.Header(
            task,
            reportFile,
            null,
            settings.findOrganization("wa-phd"),
            settings.findReceiver("wa-phd.gaen"),
            metadata.findSchema("covid-19-gaen"),
            content = content.toByteArray(),
        )
    }

    private fun setupAPIWith200Return() {
    }

    private fun setupTransport() {
        every { gaenTransport.lookupCredentials(any()) }
            .returns(UserApiKeyCredential("rick", "xzy"))
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test send happy path`() {
        val header = makeHeader()
        setupLogger()
        setupTransport()

        // Set up a OK ENVC API response
        val client = mockk<Client>()
        every { client.executeRequest(any()).statusCode } returns 200
        every { client.executeRequest(any()).responseMessage } returns "OK"
        every { client.executeRequest(any()).data } returns successJson.toByteArray()
        FuelManager.instance.client = client

        val actionHistory = ActionHistory(TaskAction.send, context)
        val retryItems = gaenTransport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.send)
    }

    @Test
    fun `test send and retry`() {
        val header = makeHeader()
        setupLogger()
        setupTransport()

        // Set up a OK ENVC API response
        val client = mockk<Client>()
        every { client.executeRequest(any()).statusCode } returns 429
        every { client.executeRequest(any()).responseMessage } returns "Too Many Requests"
        every { client.executeRequest(any()).data } returns maintenanceJson.toByteArray()
        FuelManager.instance.client = client

        val actionHistory = ActionHistory(TaskAction.send, context)
        val retryItems = gaenTransport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(RetryToken.isAllItems(retryItems)).isTrue()
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.send_warning)
    }

    @Test
    fun `test send and error`() {
        val header = makeHeader()
        setupLogger()
        setupTransport()

        // Set up a OK ENVC API response
        val client = mockk<Client>()
        every { client.executeRequest(any()).statusCode } returns 400
        every { client.executeRequest(any()).responseMessage } returns "Bad Request"
        every { client.executeRequest(any()).data } returns errorJson.toByteArray()
        FuelManager.instance.client = client

        val actionHistory = ActionHistory(TaskAction.send, context)
        val retryItems = gaenTransport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.send_error)
    }
}