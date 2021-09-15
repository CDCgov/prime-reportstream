package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.HttpStatusType
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.tokens.AccessToken
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.TokenAuthentication
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.verify
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.collections.Map
import kotlin.test.Test

class MockHttpResponseMessage : HttpResponseMessage.Builder, HttpResponseMessage {
    var httpStatus: HttpStatusType = HttpStatus.OK
    var content: Any? = null

    override fun getStatus(): HttpStatusType {
        return this.httpStatus
    }

    override fun getHeader(var1: String): String {
        return "world"
    }

    override fun getBody(): Any? {
        return this.content
    }

    override fun status(status: HttpStatusType): HttpResponseMessage.Builder {
        this.httpStatus = status
        return this
    }

    override fun header(key: String, value: String): HttpResponseMessage.Builder {
        return this
    }

    override fun body(body: Any?): HttpResponseMessage.Builder {
        this.content = body
        return this
    }

    override fun build(): HttpResponseMessage {
        return this
    }
}

class MockHttpRequestMessage : HttpRequestMessage<String?> {
    val httpHeaders = mutableMapOf<String, String>()
    val parameters = mutableMapOf<String, String>()

    override fun getUri(): URI {
        return URI.create("http://foo.com/")
    }

    override fun getHttpMethod(): HttpMethod {
        return HttpMethod.GET
    }

    override fun getHeaders(): Map<String, String> {
        return this.httpHeaders
    }

    override fun getQueryParameters(): Map<String, String> {
        return this.parameters
    }

    override fun getBody(): String? {
        return null
    }

    override fun createResponseBuilder(var1: HttpStatus): HttpResponseMessage.Builder {
        return MockHttpResponseMessage().status(var1)
    }

    override fun createResponseBuilder(var1: HttpStatusType): HttpResponseMessage.Builder {
        return MockHttpResponseMessage().status(var1)
    }
}

class MockSettings : SettingsProvider {
    var organizationStore: MutableMap<String, Organization> = mutableMapOf()
    var receiverStore: MutableMap<String, Receiver> = mutableMapOf()
    var senderStore: MutableMap<String, Sender> = mutableMapOf()

    override val organizations get() = this.organizationStore.values
    override val senders get() = this.senderStore.values
    override val receivers get() = this.receiverStore.values

    override fun findOrganization(name: String): Organization? {
        return organizationStore[name]
    }

    override fun findReceiver(fullName: String): Receiver? {
        return receiverStore[fullName]
    }

    override fun findSender(fullName: String): Sender? {
        return senderStore[Sender.canonicalizeFullName(fullName)]
    }

    override fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>? {
        val (organizationName, _) = Receiver.parseFullName(fullName)
        val organization = organizationStore[organizationName] ?: return null
        val receiver = receiverStore[fullName] ?: return null
        return Pair(organization, receiver)
    }
}

class TokenFunctionTests {
    val context = mockkClass(ExecutionContext::class)
    var settings = MockSettings()
    val klogger = mockkClass(KotlinLogger::class)

    val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
    val pubKey = keyPair.getPublic() as RSAPublicKey

    var sender = Sender(
        "default",
        "simple_report",
        Sender.Format.CSV,
        "covid-19",
        "default"
    )
    var validScope = "simple_report.default.report"

    val jwk = Jwk(
        pubKey.getAlgorithm(),
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )

    lateinit var token: String

    @BeforeEach
    fun setup() {
        settings = MockSettings()

        mockkConstructor(WorkflowEngine::class)
        every { anyConstructed<WorkflowEngine>().settings } returns settings
        every { anyConstructed<WorkflowEngine>().recordAction(any()) } returns Unit

        val dataProvider = MockDataProvider { arrayOf<MockResult>(MockResult(0, null)) }
        val connection = MockConnection(dataProvider)
        mockkObject(WorkflowEngine.Companion)
        every { WorkflowEngine.Companion.databaseAccess } returns DatabaseAccess(connection)

        mockkConstructor(DatabaseJtiCache::class)
        every { anyConstructed<DatabaseJtiCache>().isJTIOk(any(), any()) } returns true

        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().trackActionResult(ofType(String::class)) } returns Unit

        every { klogger.warn(ofType(String::class)) } returns Unit
        mockkObject(Scope.Companion)
        every { Scope.Companion.logger } returns klogger

        val expiresAtSeconds = ((System.currentTimeMillis() / 1000) + 10).toInt()
        val expirationDate = Date(expiresAtSeconds.toLong() * 1000)
        token = Jwts.builder()
            .setExpiration(expirationDate) // exp
            .setId(UUID.randomUUID().toString()) // jti
            .setIssuer(sender.fullName)
            .signWith(keyPair.getPrivate()).compact()
    }

    @AfterEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test with a bad message`() {
        // Setup

        var httpRequestMessage = MockHttpRequestMessage()
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.getBody()).isEqualTo("Missing client_assertion parameter")
    }

    @Test
    fun `Test with a missing scope`() {
        // Setup

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.getBody()).isEqualTo("Missing scope parameter")
    }

    @Test
    fun `Test with a bad scope`() {
        listOf(
            "no_good_very_bad",
            "two.pieces",
            "four.pieces..",
        ).forEach {
            var httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.parameters.put("client_assertion", token)
            httpRequestMessage.parameters.put("scope", it)
            // Invoke
            var response = TokenFunction().report(httpRequestMessage, context)
            // Verify
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
            assertThat(response.getBody()).isEqualTo("Incorrect scope format: $it")
        }
    }

    @Test
    fun `Test no jwt`() {

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", "verylong.signed.jwtstring")
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.getBody()).isEqualTo(null)
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                match<String> {
                    it.startsWith(
                        "Rejecting SenderToken JWT: io.jsonwebtoken.MalformedJwtException: Unable to read JSON value:"
                    )
                }
            )
        }
    }

    @Test
    fun `Test missing issuer`() {
        val expiresAtSeconds = ((System.currentTimeMillis() / 1000) + 10).toInt()
        val expirationDate = Date(expiresAtSeconds.toLong() * 1000)
        val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
        val token = Jwts.builder()
            .setExpiration(expirationDate) // exp
            .signWith(keyPair.getPrivate()).compact()
        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var tokenFunction = TokenFunction()
        var response = tokenFunction.report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                "Rejecting SenderToken JWT: java.lang.NullPointerException: issuer must not be null"
            )
        }
    }

    @Test
    fun `Test expired key`() {
        settings.senderStore.put(sender.fullName, Sender(sender, validScope, jwk))

        val expiresAtSeconds = ((System.currentTimeMillis() / 1000) + 10).toInt()
        val expirationDate = Date(expiresAtSeconds.toLong() - 1000)
        val token = Jwts.builder()
            .setExpiration(expirationDate) // exp
            .setId("helloworld") // jti
            .setIssuer(sender.fullName)
            .signWith(keyPair.getPrivate()).compact()
        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `Test no keys for sender`() {
        settings.senderStore.put(
            sender.fullName,
            sender
        )
        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                "AccessToken Request Denied: Error while requesting simple_report.default.report: " +
                    "No auth keys associated with sender simple_report.default"
            )
        }
    }

    @Test
    fun `Test invalid scope for sender`() {
        settings.senderStore.put(sender.fullName, Sender(sender, validScope, jwk))
        listOf(
            // Wrong org
            listOf(
                "wrong.default.report",
                "AccessToken Request Denied: Error while requesting wrong.default.report: " +
                    "Invalid scope for this sender: wrong.default.report",
                "Expected organization simple_report. Instead got: wrong"
            ),
            // Wrong sender
            listOf(
                "simple_report.wrong.report",
                "AccessToken Request Denied: Error while requesting simple_report.wrong.report: " +
                    "Invalid scope for this sender: simple_report.wrong.report",
                "Expected sender default. Instead got: wrong"
            ),
            // Wrong 
            listOf(
                "simple_report.default.bad",
                "AccessToken Request Denied: Error while requesting simple_report.default.bad: " +
                    "Invalid scope for this sender: simple_report.default.bad",
                "Invalid DetailedScope bad"
            ),
        ).forEach {
            var httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.parameters.put("client_assertion", token)
            httpRequestMessage.parameters.put("scope", it[0])
            // Invoke
            var response = TokenFunction().report(httpRequestMessage, context)
            // Verify
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
            verify { anyConstructed<ActionHistory>().trackActionResult(it[1]) }
            verify { klogger.warn(it[2]) }
        }
    }

    @Test
    fun `Test no key for scope`() {

        settings.senderStore.put(sender.fullName, Sender(sender, "test.scope", jwk))

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                "AccessToken Request Denied: Error while requesting simple_report.default.report: " +
                    "Unable to find auth key for simple_report.default with scope=simple_report.default.report, " +
                    "kid=null, and alg=RS256"
            )
        }
    }

    @Test
    fun `Test success`() {

        mockkConstructor(TokenAuthentication::class)
        every { anyConstructed<TokenAuthentication>().createAccessToken(any(), any(), any()) } returns AccessToken(
            "test",
            "test",
            "test",
            10,
            10,
            "test"
        )

        settings.senderStore.put(sender.fullName, Sender(sender, validScope, jwk))

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction().report(httpRequestMessage, context)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
    }
}