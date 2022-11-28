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
import gov.cdc.prime.router.credentials.UserAssertionCredential
import gov.cdc.prime.router.credentials.UserPassCredential
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

    private val mockClientAuthOk = mockJsonResponseWithSuccess(
        """{"access_token": "AYjcyMzY3ZDhiNmJkNTY", 
                        |"refresh_token": "RjY2NjM5NzA2OWJjuE7c", 
                        |"token_type": "Bearer", "expires_in": 3600}"""
    )

    private val mockClientAuthIDTokenOk = mockJsonResponseWithSuccess(
        """{"email": "test-email@test.com",
                        "idToken": "AYjcyMzY3ZDhiNmJkNTY", 
                        |"expiresIn": 3600,
                        "refreshToken": "RjY2NjM5NzA2OWJjuE7c"}"""
    )

    private val mockClientAuthError = mockJsonResponseWithError(
        """{"error": {"code": 500,"message": "Mock internal server error."}}"""
    )

    private val mockClientUnauthorized = mockJsonResponseWithUnauthorized(
        """{"error": {"code": 401,"message": "Mock unauthorized error."}}"""
    )

    private val mockClientPostOk = mockJsonResponseWithSuccess(
        """{"status": "Success", 
                        |"statusDesc": "Received. LIN:4299844", 
                        |"respTrackingId": "UT-20211119-746000000-54"}"""
    )

    private val mockClientPostError = mockJsonResponseWithError(
        """{"error": {"code": 500,"message": "Mock internal server error."}}"""
    )

    private val mockClientUnknownError = mockJsonResponseWithUnknown(
        """{"error": {"code": 999,"message": "Mock internal server error."}}"""
    )

    private fun mockJsonResponseWithSuccess(jsonResponse: String): HttpClient {
        return mockJsonResponse(jsonResponse, HttpStatusCode.OK)
    }

    private fun mockJsonResponseWithError(jsonResponse: String): HttpClient {
        return mockJsonResponse(jsonResponse, HttpStatusCode.InternalServerError)
    }

    private fun mockJsonResponseWithUnauthorized(jsonResponse: String): HttpClient {
        return mockJsonResponse(jsonResponse, HttpStatusCode.Unauthorized)
    }

    private fun mockJsonResponseWithUnknown(jsonResponse: String): HttpClient {
        return mockJsonResponse(jsonResponse, HttpStatusCode(999, "Uknown Error"))
    }

    private fun mockJsonResponse(jsonResponse: String, whatStatusCode: HttpStatusCode): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        jsonResponse.trimMargin(),
                        whatStatusCode,
                        responseHeaders
                    )
                }
            }
            install(ClientContentNegotiation) { json() }
        }
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
            UserApiKeyCredential(
                "test-user",
                "test-key"
            )
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }.returns("")
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connecting to mock service getAssertionToken happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthOk))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserAssertionCredential(
                "test-assertion"
            )
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }.returns("")
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connecting to mock service getIdToken happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthIDTokenOk))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserPassCredential("test-user", "test-pass")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }.returns("")
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connecting to mock service credential happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthIDTokenOk))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserPassCredential("test-user", "test-pass")
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