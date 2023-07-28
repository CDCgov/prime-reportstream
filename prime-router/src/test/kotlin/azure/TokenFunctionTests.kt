package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.tokens.AccessToken
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.Server2ServerAuthentication
import gov.cdc.prime.router.unittest.UnitTestUtils
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
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.test.Test

class TokenFunctionTests {
    var settings = MockSettings()
    val klogger = mockkClass(KotlinLogger::class)

    val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
    val pubKey = keyPair.getPublic() as RSAPublicKey

    var sender = CovidSender(
        "default",
        "simple_report",
        Sender.Format.CSV,
        CustomerStatus.INACTIVE,
        "default"
    )
    val organization = Organization(
        sender.organizationName,
        "simple_report_org",
        Organization.Jurisdiction.FEDERAL,
        null,
        null,
        null,
        null,
        null
    )
    var validScope = "simple_report.default.report"

    val jwk = Jwk(
        pubKey.getAlgorithm(),
        kid = "kid1",
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )

    val badJwk = Jwk(
        "invalid",
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
        mockkObject(BaseEngine.Companion)
        every { BaseEngine.Companion.databaseAccessSingleton } returns DatabaseAccess(connection)

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
            .setHeaderParam("kid", jwk.kid)
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
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_request")
        assertThat(error.get("error_description").textValue()).isEqualTo("missing_scope")
        assertThat(error.get("error_uri").textValue())
            .isEqualTo("$OAUTH_ERROR_BASE_LOCATION#missing-scope")
    }

    @Test
    fun `Test with a missing scope`() {
        // Setup

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_request")
        assertThat(error.get("error_description").textValue()).isEqualTo("missing_scope")
        assertThat(error.get("error_uri").textValue())
            .isEqualTo("$OAUTH_ERROR_BASE_LOCATION#missing-scope")
    }

    @Test
    fun `Test with a bad scope`() {
        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))
        listOf(
            "no_good_very_bad",
            "two.pieces",
            "four.pieces..",
        ).forEach {
            var httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.parameters.put("client_assertion", token)
            httpRequestMessage.parameters.put("scope", it)
            // Invoke
            var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
            // Verify
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
            val error = jacksonObjectMapper().readTree(response.body as String)
            assertThat(error.get("error").textValue()).isEqualTo("invalid_scope")
            assertThat(error.get("error_description").textValue()).isEqualTo("invalid_scope")
            assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#invalid-scope")
        }
    }

    @Test
    fun `Test no jwt`() {

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", "verylong.signed.jwtstring")
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_request")
        assertThat(error.get("error_description").textValue()).isEqualTo("malformed_jwt")
        assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#malformed-jwt")
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                match<String> {
                    it.startsWith(
                        "AccessToken Request Denied: Malformed JWT JSON: "
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
        var tokenFunction = TokenFunction(UnitTestUtils.simpleMetadata)
        var response = tokenFunction.token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_request")
        assertThat(error.get("error_description").textValue()).isEqualTo("malformed_jwt")
        assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#malformed-jwt")
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                "AccessToken Request Denied: issuer must not be null"
            )
        }
    }

    @Test
    fun `Test expired key`() {
        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))

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
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_client")
        assertThat(error.get("error_description").textValue()).isEqualTo("expired_token")
        assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#expired-token")
    }

    @Test
    fun `Test no keys for sender`() {
        settings.senderStore.put(
            sender.fullName,
            sender
        )
        settings.organizationStore.put(organization.name, organization)
        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_client")
        assertThat(error.get("error_description").textValue()).isEqualTo("no_valid_keys")
        assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#no-valid-keys")
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                "AccessToken Request Denied: Error while requesting simple_report.default.report: " +
                    "Unable to find auth key for simple_report" +
                    " with scope=simple_report.default.report, kid=kid1, and alg=RSA"
            )
        }
    }

    @Test
    fun `Test invalid scope for sender`() {
        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))
        listOf(
            // Wrong org
            listOf(
                "wrong.default.report",
                "AccessToken Request Denied: INVALID_SCOPE while generating token for" +
                    " scope: wrong.default.report for issuer: simple_report.default",
                "Expected organization simple_report. Instead got: wrong"
            ),
            // Wrong
            listOf(
                "simple_report.default.bad",
                "AccessToken Request Denied: INVALID_SCOPE while generating token for" +
                    " scope: simple_report.default.bad for issuer: simple_report.default",
                "Invalid DetailedScope bad"
            ),
        ).forEach {
            var httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.parameters.put("client_assertion", token)
            httpRequestMessage.parameters.put("scope", it[0])
            // Invoke
            var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
            // Verify
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
            val error = jacksonObjectMapper().readTree(response.body as String)
            assertThat(error.get("error").textValue()).isEqualTo("invalid_scope")
            assertThat(error.get("error_description").textValue()).isEqualTo("invalid_scope")
            assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#invalid-scope")
            verify { anyConstructed<ActionHistory>().trackActionResult(it[1]) }
            verify { klogger.warn(it[2]) }
        }
    }

    @Test
    fun `Test no key for scope`() {

        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, "test.scope", jwk))

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        val error = jacksonObjectMapper().readTree(response.body as String)
        assertThat(error.get("error").textValue()).isEqualTo("invalid_client")
        assertThat(error.get("error_description").textValue()).isEqualTo("no_valid_keys")
        assertThat(error.get("error_uri").textValue()).isEqualTo("$OAUTH_ERROR_BASE_LOCATION#no-valid-keys")
        verify {
            anyConstructed<ActionHistory>().trackActionResult(
                "AccessToken Request Denied: Error while requesting simple_report.default.report: " +
                    "Unable to find auth key for simple_report with scope=simple_report.default.report, " +
                    "kid=kid1, and alg=RSA"
            )
        }
    }

    @Test
    fun `Test success with organization`() {
        val expiresAtSeconds = ((System.currentTimeMillis() / 1000) + 10).toInt()
        val expirationDate = Date(expiresAtSeconds.toLong() * 1000)
        token = Jwts.builder()
            .setExpiration(expirationDate) // exp
            .setId(UUID.randomUUID().toString()) // jti
            .setIssuer(organization.name)
            .setHeaderParam("kid", jwk.kid)
            .signWith(keyPair.getPrivate()).compact()
        mockkConstructor(Server2ServerAuthentication::class)
        every {
            anyConstructed<Server2ServerAuthentication>().createAccessToken(any(), any(), any())
        } returns AccessToken(
            "test",
            "test",
            "test",
            10,
            10,
            "test"
        )

        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
    }

    // TODO https://github.com/CDCgov/prime-reportstream/issues/8659
    // This and the following test can be removed after all keys associated with senders have been moved to
    // the organization.  For now these tests cover the possibility that keys for the same scope might exist on the
    // organization and sender; in that case the organization keys are considered first.
    @Test
    fun `Test success when sender key is broken, but organization key is not`() {
        mockkConstructor(Server2ServerAuthentication::class)
        every {
            anyConstructed<Server2ServerAuthentication>().createAccessToken(any(), any(), any())
        } returns AccessToken(
            "test",
            "test",
            "test",
            10,
            10,
            "test"
        )

        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `Test success using parameters in URL`() {

        mockkConstructor(Server2ServerAuthentication::class)
        every {
            anyConstructed<Server2ServerAuthentication>().createAccessToken(any(), any(), any())
        } returns AccessToken(
            "test",
            "test",
            "test",
            10,
            10,
            "test"
        )

        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))

        var httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.parameters.put("client_assertion", token)
        httpRequestMessage.parameters.put("scope", validScope)
        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `Test success using parameters in body`() {

        mockkConstructor(Server2ServerAuthentication::class)
        every {
            anyConstructed<Server2ServerAuthentication>().createAccessToken(any(), any(), any())
        } returns AccessToken(
            "test",
            "test",
            "test",
            10,
            10,
            "test"
        )

        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))

        var httpRequestMessage = MockHttpRequestMessage("client_assertion=$token\n&scope=$validScope")

        // Invoke
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        // Verify
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `Test crazy params in body`() {
        settings.senderStore.put(sender.fullName, sender)
        settings.organizationStore.put(organization.name, Organization(organization, validScope, jwk))

        var httpRequestMessage = MockHttpRequestMessage("client_assertion=&scope=$validScope")
        var response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)

        httpRequestMessage = MockHttpRequestMessage("client_assertion=anything&scope=$validScope")
        response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)

        httpRequestMessage = MockHttpRequestMessage("client_assertion=any.darn.thing&scope=$validScope")
        response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)

        httpRequestMessage = MockHttpRequestMessage("=&=")
        response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)

        httpRequestMessage = MockHttpRequestMessage("gobbledygook==&&&&==")
        response = TokenFunction(UnitTestUtils.simpleMetadata).token(httpRequestMessage)
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}