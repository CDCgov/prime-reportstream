package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RESTTransportIntegrationTests : TransportIntegrationTests() {
    private val metadata = Metadata.getInstance()
    private val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    private val responseHeaders = headersOf("Content-Type" to listOf("application/json;charset=UTF-8"))

    private val mockClientAuthOk = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    """{"access_token": "AYjcyMzY3ZDhiNmJkNTY", 
                        |"refresh_token": "RjY2NjM5NzA2OWJjuE7c", 
                        |"token_type": "Bearer", "expires_in": 3600}
                    """.trimMargin(),
                    HttpStatusCode.OK,
                    responseHeaders
                )
            }
        }
        install(ClientContentNegotiation) { json() }
    }

    private val mockClientAuthError = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    """{"error": {"code": 500,"message": "Mock internal server error."}}""",
                    HttpStatusCode.InternalServerError,
                    responseHeaders
                )
            }
        }
        install(ClientContentNegotiation) { json() }
    }

    private val mockClientUnauthorized = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    """{"error": {"code": 401,"message": "Mock unauthorized error."}}""",
                    HttpStatusCode.Unauthorized,
                    responseHeaders
                )
            }
        }
        install(ClientContentNegotiation) { json() }
    }

    private val mockClientPostOk = HttpClient(MockEngine) {
        engine {
            addHandler { _ ->
                respond(
                    """{"status": "Success", 
                        |"statusDesc": "Received. LIN:4299844", 
                        |"respTrackingId": "UT-20211119-746000000-54"}""".trimMargin(),
                    HttpStatusCode.OK,
                    responseHeaders
                )
            }
        }
        install(ClientContentNegotiation) { json() }
    }

    private val mockClientPostError = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    """{"error": {"code": 500,"message": "Mock internal server error."}}""",
                    HttpStatusCode.InternalServerError,
                    responseHeaders
                )
            }
        }
        install(ClientContentNegotiation) { json() }
    }

    private val mockClientUnknownError = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    """{"error": {"code": 999,"message": "Mock internal server error."}}""",
                    HttpStatusCode(999, "Unknown error"),
                    responseHeaders
                )
            }
        }
        install(ClientContentNegotiation) { json() }
    }

    private var actionHistory = ActionHistory(TaskAction.send)
    private val transportType = RESTTransportType(
        "mock-api",
        "mock-tokenUrl",
        null,
        mapOf("mock-h1" to "value-h1", "mock-h2" to "value-h2")
    )
    private val task = Task(
        reportId,
        TaskAction.send,
        null,
        "standard.standard-covid-19",
        "az-phd.elr-test",
        4,
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
        val content = "HL7|Stuff"
        return WorkflowEngine.Header(
            task, reportFile,
            null,
            settings.findOrganization("ny-phd"),
            settings.findReceiver("ny-phd.elr"),
            metadata.findSchema("covid-19"),
            content = content.toByteArray(),
            true
        )
    }

    @BeforeEach
    fun reset() {
        actionHistory = ActionHistory(TaskAction.send)
    }

    @Test
    fun `test connecting to mock service getToken happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthOk))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }.returns("")
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connecting to mock service getToken unhappy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthError))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }.returns("")
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
    }

    @Test
    fun `test connecting to mock service getToken unauthorized`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientUnauthorized))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }.returns("")
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connecting to mock service postReport happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientPostOk))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo("MockToken", 1000, "MockRefreshToken", null, "bearer")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connecting to mock service postReport unhappy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientPostError))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo("MockToken", 1000, "MockRefreshToken", null, "bearer")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
    }

    @Test
    fun `test connecting to mock service unknown error`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientUnknownError))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo("MockToken", 1000, "MockRefreshToken", null, "bearer")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
    }

    @Test
    fun `test creating transport`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport())
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
    }
}