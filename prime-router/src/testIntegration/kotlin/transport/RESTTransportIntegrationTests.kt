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
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.Attributes
import io.ktor.util.InternalAPI
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.Memory
import io.ktor.utils.io.core.Input
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RESTTransportIntegrationTests : TransportIntegrationTests() {
    private val metadata = Metadata.getInstance()
    private val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    private val responseHeaders = headersOf("Content-Type" to listOf("application/json;charset=UTF-8"))

    private fun mockClientAuthOk(): HttpClient {
        return mockJsonResponseWithSuccess(
            """{"access_token": "AYjcyMzY3ZDhiNmJkNTY", 
                        |"refresh_token": "RjY2NjM5NzA2OWJjuE7c", 
                        |"token_type": "Bearer", "expires_in": 3600}
            """
        )
    }

    private fun mockClientAuthIDTokenOk(): HttpClient {
        return mockJsonResponseWithSuccess(
            """{"email": "test-email@test.com",
                        "idToken": "AYjcyMzY3ZDhiNmJkNTY", 
                        |"expiresIn": 3600,
                        "refreshToken": "RjY2NjM5NzA2OWJjuE7c"}
            """
        )
    }

    private fun mockClientAuthError(): HttpClient {
        return mockJsonResponseWithError(
            """{"error": {"code": 500,"message": "Mock internal server error."}}"""
        )
    }

    private fun mockClientUnauthorized(): HttpClient {
        return mockJsonResponseWithUnauthorized(
            """{"error": {"code": 401,"message": "Mock unauthorized error."}}"""
        )
    }

    private fun mockClientPostOk(): HttpClient {
        return mockJsonResponseWithSuccess(
            """{"status": "Success", 
                        |"statusDesc": "Received. LIN:4299844", 
                        |"respTrackingId": "UT-20211119-746000000-54"}
            """
        )
    }

    private fun mockClientPostError(): HttpClient {
        return mockJsonResponseWithError(
            """{"error": {"code": 500,"message": "Mock internal server error."}}"""
        )
    }

    private fun mockClientUnknownError(): HttpClient {
        return mockJsonResponseWithUnknown(
            """{"error": {"code": 999,"message": "Mock internal server error."}}"""
        )
    }

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
        headers = mapOf("mock-h1" to "value-h1", "mock-h2" to "value-h2")
    )
    private val flexionRestTransportType = RESTTransportType(
        "v1/etor/demographics",
        "v1/auth",
        "two-legged",
        null,
        mapOf("mock-p1" to "value-p1", "mock-p2" to "value-p2"),
        headers = mapOf("mock-h1" to "value-h1", "mock-h2" to "value-h2")
    )

    private val fakePrivateKey = """-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAvzoqTD4p9tRCyF2sPsq8ZE2/cCslUBYb+u+4whe6PGHAssU9
vFSqeF/ZlaU8Zo/hi+m0AaHcISN+St/VJ2+JuEOs4KDGbYjT/NeT0rN+5fAOHmNI
JVPWyxrTNblTh0Z0w4um37UpQWmBnh+/lmT3faaGZmE+kdaj/Rvg8bmbYAudz0Wa
QQD3gTmSTLDlqiLgYZ4g0kxvk9MP8MIyzU4mW3aHsZbTBq8vXHmBHWGui4XV/3t8
xOknjAEgqYp5W/pV7Z/8yg1QF7jeBdkSqk8gJti+UsMyhGratos/PejDLgJ29TVw
JPnr8JOw4GgTfxEpDVD9uLB+l6pOhVi5/Xy2WwIDAQABAoIBADW2OLtBmrfh1wBA
j2jzuQbTb7op5EkibKmHO/YIhB8BtgaAsF59yYQWKB+IUfbc4xVRh2YN+K62MXsG
c/Cnu58uIxjVD+ckT6btRnAgSrx7OPwAFvulGOfjmP+2FmgguhtS1oRvdi8RW372
m/G6WmnIpGeNWO3NxDIL6pr671GCdUSovtlgmWMDPA6tL2GxXgQ936DIS+3yoDqx
p39ze7WMTVS3qQcf/WLCkj8vugMc2gT1sB60PDwIHXs4sE/dcV017hUoHGZ8I5M3
jEDiBO/EHm2057MAsoCrTBAlHgIp4LRAeJsJrQjIVM5v87J9TnK93j/DjuIHuwt+
5yXUZ0ECgYEA6ueBBdDI6LAc4BRCvEyUKrP9PIJXjmVSkrN+j32mxa/m1Wr+EFo6
qJHU819GkjGjHYS/lm9Ktp1cViZi09GJ8Qc69mghdh4Ul7a0iRHqE8U0OQYQ/+jd
GcveA5oOrSkUmTudKBehA4NG+SAKMlQUxE4B9jNZJSglm/bvfzg6PG0CgYEA0GaE
HSR2GatxfHUMLA0b5cYpddqVIkpyfw6CsIkFO9SA2/uKcJVpOhkUL1+CyN6hOxGZ
5WIhWTsgmRF6CcZTc1OL6zFwVe8Qm/w2rfhSRsQW6r1aHni5n4VaZIjM30qTtXnw
qZMoEMdXp4ZyiyISTEUzbJMAJLvb9n0jqGcy8OcCgYEAlGhwpnjzBbeGdre2Nfr3
vUb73gfHQ2qWUaLfec571/e7EejH3RL0bl28OYpfLqv3jwW4eVWU0Wz30mHsHEEN
Ml9MDDNbuKFmhzdiNhtAVP9JcCF6CgRX9B1OBd6GGTaUEf+M2bCcBHkjFO/+IS/n
8uIJ8T8duhD0OeY4B5tBh70CgYB3R+YgZAkw4h8/tCL2Z75rAsQiT60ChogITz7g
WlzjhNsIjm4LyIkFFuulvc42uK6iB44Fa+hYkW40DnvCcoC7RruUL6TzmrTwAyks
5C/7jyrqytx1CnZ8ZrB5UI+q0p2+xn/IH5FUITT7jwmqtyKwAv/PCkiPvfLFn5S0
Q/U1SwKBgQDZF6UkkOeBh4TKDQl0aXTvstIQ/ChdGm6qVm9ksxlupudEeHhDO2MD
5HZD6unsl7WWAdczWFxUTslGKb1HltT7yeRKEbgPexP9d+g14SLT/pnfO259gZuD
hnm8COa8Kr+bnTqzScpQuOfujHcFEtfcYUGfSS6HusxidwXx+lYi1A==
-----END RSA PRIVATE KEY-----
    """

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
            task,
            reportFile,
            null,
            settings.findOrganization("ignore"),
            settings.findReceiver("ignore.REST_TEST"),
            metadata.findSchema("covid-19"),
            content = content.toByteArray(),
            true
        )
    }

    /**
     * This is horrible, but my understanding is that HttpClientCall and HttpResponse are so tightly coupled that this
     * is pretty much the only way to get a valid dummy HttpResponse object.
     */
    private fun getHttpResponse(statusCode: HttpStatusCode, body: String = ""): HttpResponse {
        val call = mockk<HttpClientCall> {
            every { client } returns mockk {}
            coEvery { body(io.ktor.util.reflect.typeInfo<String>()) } returns "Rest Transport Test Call Body"
            coEvery { bodyNullable(io.ktor.util.reflect.typeInfo<Input>()) } returns object : Input() {
                override fun closeSource() {}

                override fun fill(destination: Memory, offset: Int, length: Int): Int {
                    return 0
                }
            }
            every { coroutineContext } returns EmptyCoroutineContext
            every { attributes } returns Attributes()
            every { request } returns object : HttpRequest {
                override val call: HttpClientCall = this@mockk
                override val attributes: Attributes = Attributes()
                override val content: OutgoingContent = object : OutgoingContent.NoContent() {}
                override val headers: Headers = Headers.Empty
                override val method: HttpMethod = HttpMethod.Get
                override val url: Url = Url("/")
            }

            every { response } returns object : HttpResponse() {
                override val call: HttpClientCall = this@mockk

                @InternalAPI
                override val content: ByteReadChannel = ByteReadChannel(body)
                override val coroutineContext: CoroutineContext = EmptyCoroutineContext
                override val headers: Headers = Headers.Empty
                override val requestTime: GMTDate = GMTDate.START
                override val responseTime: GMTDate = GMTDate.START
                override val status: HttpStatusCode = statusCode
                override val version: HttpProtocolVersion = HttpProtocolVersion.HTTP_1_1
            }
        }

        return call.response
    }

    @BeforeEach
    fun reset() {
        actionHistory = ActionHistory(TaskAction.send)
    }

    @Test
    fun `test connecting to mock service getToken happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthOk()))
        val mockPostReportResponse = getHttpResponse(HttpStatusCode.OK)
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential(
                "test-user",
                "test-key"
            )
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }
            .returns(mockPostReportResponse)
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service getAssertionToken happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthOk()))
        val mockPostReportResponse = getHttpResponse(HttpStatusCode.OK)
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserAssertionCredential(
                "test-assertion"
            )
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }
            .returns(mockPostReportResponse)
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service getIdToken happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthIDTokenOk()))
        val mockPostReportResponse = getHttpResponse(HttpStatusCode.OK)
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserPassCredential("test-user", "test-pass")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }
            .returns(mockPostReportResponse)
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service credential happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthIDTokenOk()))
        val mockPostReportResponse = getHttpResponse(HttpStatusCode.OK)
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserPassCredential("test-user", "test-pass")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }
            .returns(mockPostReportResponse)
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service getToken unhappy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthError()))
        val mockPostReportResponse = getHttpResponse(HttpStatusCode.InternalServerError)
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }
            .returns(mockPostReportResponse)
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service getToken unauthorized`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientUnauthorized()))
        val mockPostReportResponse = getHttpResponse(HttpStatusCode.Unauthorized)
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.postReport(any(), any(), any(), any(), any(), any()) } }
            .returns(mockPostReportResponse)
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service postReport happy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientPostOk()))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo("MockToken", 1000, "MockRefreshToken", null, "bearer")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service postReport unhappy path`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientPostError()))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo("MockToken", 1000, "MockRefreshToken", null, "bearer")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
    }

    @Test
    fun `test connecting to mock service unknown error`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientUnknownError()))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("test-user", "test-key")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo("MockToken", 1000, "MockRefreshToken", null, "bearer")
        )
        val retryItems = mockRestTransport.send(transportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNotNull()
        assertThat(actionHistory.action.httpStatus).isNotNull()
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

    @Test
    fun `test getAuthTokenWithUserApiKey with transport parametters is empty`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthOk()))

        // Given:
        //      lookupDefaultCredential returns mock UserApiKeyCredential object to allow
        //      the getAuthTokenWIthUserApiKey() to be called.
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential(
                "test-user",
                "test-apikey"
            )
        )

        // When:
        //      RESTTransport is called WITH transport.parameters empty
        val retryItems = mockRestTransport.send(
            transportType, header, reportId, null,
            context, actionHistory
        )

        // Then:
        //      getAuthTokenWithUserApiKey should be called with transport.parameters empty
        verify {
            runBlocking {
                mockRestTransport.lookupDefaultCredential(any())
                mockRestTransport.getAuthTokenWithUserApiKey(transportType, any(), any(), any())
            }
        }
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test getAuthTokenWithUserApiKey with transport parametters is NOT empty`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientAuthOk()))

        // Given:
        //      lookupDefaultCredential returns mock UserApiKeyCredential object to allow
        //      the getAuthTokenWIthUserApiKey() to be called.
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential(
                "test-user",
                fakePrivateKey
            )
        )

        // When:
        //      RESTTransport is called WITH flexionRestTransportType which has transport.parameters
        val retryItems = mockRestTransport.send(
            flexionRestTransportType, header, reportId, null,
            context, actionHistory
        )

        // Then:
        //      getAuthTokenWithUserApiKey should be called with transport.parameters NOT empty
        verify {
            runBlocking {
                mockRestTransport.lookupTwoLeggedCredential(any())
                mockRestTransport.getAuthTokenWithUserApiKey(flexionRestTransportType, any(), any(), any())
            }
        }
        assertThat(retryItems).isNull()
    }

    @Test
    fun `test flexion rest transport`() {
        val header = makeHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientPostOk()))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential("flexion", fakePrivateKey)
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserApiKey(any(), any(), any(), any()) } }.returns(
            TokenInfo(accessToken = "MockToken", tokenType = "bearer")
        )

        val retryItems = mockRestTransport.send(
            flexionRestTransportType, header, reportId, null,
            context, actionHistory
        )
        assertThat(retryItems).isNull()
    }
}