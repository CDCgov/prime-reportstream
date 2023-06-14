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
import gov.cdc.prime.router.credentials.UserEtorCredential
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
        mapOf("mock-h1" to "value-h1", "mock-h2" to "value-h2")
    )
    private val flexionRestTransportType = RESTTransportType(
        "http://rest-webservice:3001/report",
        "http://rest-webservice:3001/token",
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
    private fun makeFlexionHeader(): WorkflowEngine.Header {
        val content = "{\"resourceType\":\"Bundle\",\"id\":\"969bcbb3-cd34-49be-ac4f-e1b8479b8219\",\"identifier\":{\"value\":\"969bcbb3-cd34-49be-ac4f-e1b8479b8219\"},\"type\":\"message\",\"timestamp\":\"2023-03-20T10:00:37.103-05:00\",\"entry\":[{\"resource\":{\"resourceType\":\"MessageHeader\",\"id\":\"3b5e0ea5-f204-4cb1-8027-7adf07d8065b\",\"meta\":{\"tag\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0103\",\"code\":\"P\",\"display\":\"Production\"}]},\"eventCoding\":{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0003\",\"code\":\"O21\",\"display\":\"OML - Laboratory order\"},\"source\":{\"name\":\"CDC Trusted Intermediary\",\"endpoint\":\"https://reportstream.cdc.gov/\"}}},{\"fullUrl\":\"Provenance/cd2e9813-d9cd-4cb9-bf05-7dcdb6b5fe10\",\"resource\":{\"resourceType\":\"Provenance\",\"id\":\"cd2e9813-d9cd-4cb9-bf05-7dcdb6b5fe10\",\"target\":[{\"reference\":\"Endpoint/1682547324873864000.66e513f6-3631-4d9e-a8f5-f2f6587e814f\"}],\"recorded\":\"2023-03-02T21:30:42.888+00:00\",\"activity\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0003\",\"code\":\"O21\",\"display\":\"OML - Laboratory order\"}]}}},{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"infant-twin-1\",\"extension\":[{\"url\":\"http://hl7.org/fhir/us/core/StructureDefinition/us-core-race\",\"extension\":[{\"url\":\"text\",\"valueString\":\"Asian\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MR\",\"display\":\"Medical Record Number\"}]},\"value\":\"MRN7465737865\"}],\"name\":[{\"use\":\"official\",\"text\":\"TestValue\",\"family\":\"Solo\",\"given\":[\"Jaina\"]}],\"gender\":\"female\",\"birthDate\":\"2017-05-15\",\"_birthDate\":{\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/patient-birthTime\",\"valueDateTime\":\"2017-05-15T11:11:00-05:00\"}]},\"address\":[{\"use\":\"home\",\"line\":[\"5600 S Quebec St Ste 312A\"],\"city\":\"Greenwood Village\",\"district\":\"PA\",\"state\":\"IG\",\"postalCode\":\"80111\",\"country\":\"***\"}],\"multipleBirthInteger\":1,\"contact\":[{\"relationship\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0131\",\"code\":\"N\",\"display\":\"Next of kin\"}]}],\"name\":{\"family\":\"Organa\",\"given\":[\"Leia\"]},\"telecom\":[{\"system\":\"phone\",\"value\":\"+31201234567\"}]}]}},{\"resource\":{\"resourceType\":\"ServiceRequest\",\"id\":\"0beae7d1-7b3a-4a80-a3d3-7c2ae5e61e15\",\"status\":\"active\",\"intent\":\"order\",\"category\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"108252007\",\"display\":\"Laboratory procedure\"}]}],\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"54089-8\",\"display\":\"Newborn Screening Panel\"}]},\"subject\":{\"reference\":\"Patient/infant-twin-1\"},\"authoredOn\":\"2023-03-20T10:00:37-05:00\"}},{\"fullUrl\":\"Endpoint/1682547324873864000.66e513f6-3631-4d9e-a8f5-f2f6587e814f\",\"resource\":{\"resourceType\":\"Endpoint\",\"id\":\"1682547324873864000.66e513f6-3631-4d9e-a8f5-f2f6587e814f\",\"identifier\":[{\"system\":\"https://reportstream.cdc.gov/prime-router\",\"value\":\"flexion.FULL-ELR-FHIR\"}],\"status\":\"active\",\"name\":\"Ignore FULL-ELR-FHIR\"}}]}"

        return WorkflowEngine.Header(
            task,
            reportFile,
            null,
            settings.findOrganization("flexion"),
            settings.findReceiver("flexion.etor-service-receiver"),
            metadata.findSchema("metadata/fhir_transforms/receivers/fhir-transform-sample"),
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
    fun `test flexion rest transport`() {
        val header = makeFlexionHeader()
        val mockRestTransport = spyk(RESTTransport(mockClientPostOk()))
        every { mockRestTransport.lookupDefaultCredential(any()) }.returns(
            UserEtorCredential("flexion", "123")
        )
        every { runBlocking { mockRestTransport.getAuthTokenWithUserEtor(any(), any(), any(), any()) } }.returns(
            TokenInfoEtor("878a0e79-0e97-4f06-8c44-d756b74e8134", "reportStream",
                "036231e0-5a1a-4ded-b784-2c4212e311bb")
        )

        val retryItems = mockRestTransport.send(flexionRestTransportType, header, reportId, null, context, actionHistory)
        assertThat(retryItems).isNull()
    }
}