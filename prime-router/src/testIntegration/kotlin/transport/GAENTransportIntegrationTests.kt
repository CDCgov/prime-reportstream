package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.GAENTransportType
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Test
import java.util.UUID

class GAENTransportIntegrationTests : TransportIntegrationTests() {
    private val metadata = Metadata.getInstance()
    private val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    private val gaenTransport = spyk<GAENTransport>()
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
        null,
        null
    )

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
            true
        )
    }

    private fun setupTransport() {
        every { gaenTransport.lookupCredentials(any()) }
            .returns(UserApiKeyCredential("rick", "xzy"))
    }

    @Test
    fun `test send happy path`() {
        val header = makeHeader()
        setupTransport()

        // Set up a OK ENVC API response
        val client = mockk<Client>()
        every { client.executeRequest(any()).statusCode } returns 200
        every { client.executeRequest(any()).responseMessage } returns "OK"
        every { client.executeRequest(any()).data } returns successJson.toByteArray()
        FuelManager.instance.client = client

        val actionHistory = ActionHistory(TaskAction.send)
        val retryItems = gaenTransport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.send)
    }

    @Test
    fun `test send and retry`() {
        val header = makeHeader()
        setupTransport()

        // Set up a OK ENVC API response
        val client = mockk<Client>()
        every { client.executeRequest(any()).statusCode } returns 429
        every { client.executeRequest(any()).responseMessage } returns "Too Many Requests"
        every { client.executeRequest(any()).data } returns maintenanceJson.toByteArray()
        FuelManager.instance.client = client

        val actionHistory = ActionHistory(TaskAction.send)
        val retryItems = gaenTransport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(RetryToken.isAllItems(retryItems)).isTrue()
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.send_warning)
    }

    @Test
    fun `test send and error`() {
        val header = makeHeader()
        setupTransport()

        // Set up a OK ENVC API response
        val client = mockk<Client>()
        every { client.executeRequest(any()).statusCode } returns 400
        every { client.executeRequest(any()).responseMessage } returns "Bad Request"
        every { client.executeRequest(any()).data } returns errorJson.toByteArray()
        FuelManager.instance.client = client

        val actionHistory = ActionHistory(TaskAction.send)
        val retryItems = gaenTransport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.send_error)
        assertThat(actionHistory.action.actionResult).contains("""errorCode": "invalid_date""")
    }
}