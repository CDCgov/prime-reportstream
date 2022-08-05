package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.common.Environment
import io.jsonwebtoken.Claims
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthenticationStrategyTests {
    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test isLocal`() {
        mockkObject(Environment) {
            every { Environment.isLocal() } returns false
            assertThat(AuthenticationStrategy.isLocal(null)).isFalse()
            assertThat(AuthenticationStrategy.isLocal("")).isFalse()
            assertThat(AuthenticationStrategy.isLocal("abc")).isFalse()
            assertThat(AuthenticationStrategy.isLocal("a.b.c")).isFalse()

            every { Environment.isLocal() } returns true
            assertThat(AuthenticationStrategy.isLocal(null)).isTrue()
            assertThat(AuthenticationStrategy.isLocal("")).isTrue()
            assertThat(AuthenticationStrategy.isLocal("abc")).isTrue()
            assertThat(AuthenticationStrategy.isLocal("a.b.c")).isFalse()
        }
    }

    @Test
    fun `test authenticate okta happy path`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            // General note:  AuthenticationStrategy.authenticate does not check for missing client header,
            // because that check is part of authorization.   So no client header value is defined here - not needed.
            "authentication-type" to "okta",
        )
        mockkObject(OktaAuthentication)
        // Okta style token. There's a logger call that uses 'sub'
        val jwt = mapOf("sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "simple_report")
        every { OktaAuthentication.authenticate(any()) } returns claims
        assertThat(AuthenticationStrategy.authenticate(req)).isEqualTo(claims)
    }

    @Test
    fun `test authenticate okta fails`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            "authentication-type" to "okta"
        )
        mockkObject(OktaAuthentication)
        // Okta auth fails, so it returns null
        every { OktaAuthentication.authenticate(any()) } returns null
        assertThat(AuthenticationStrategy.authenticate(req)).isNull()
    }

    @Test
    fun `test authenticate server2server happy path`() {
        val req = MockHttpRequestMessage("test")
        // Missing authentication-type header in req!  Means ==> use token auth.
        val jwtClaims = mockkClass(Claims::class)
        every { jwtClaims[any()] } returns "simple_report.default.report"
        mockkConstructor(TokenAuthentication::class)
        every { anyConstructed<TokenAuthentication>().authenticate(any(), any()) } returns jwtClaims
        val claims = AuthenticationStrategy.authenticate(req)
        assertThat(claims?.organizationNameClaim).isEqualTo("simple_report")
        assertThat(claims?.isSenderOrgClaim).isEqualTo(false)
        assertThat(claims?.isPrimeAdmin).isEqualTo(false)

        // Incorrect authentication-type header! Means ==> use token auth; not treated as an error
        req.httpHeaders += mapOf("authentication-type" to "bogus")
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isEqualTo("simple_report")
    }

    @Test
    fun `test authenticate server2server auth fails`() {
        val req = MockHttpRequestMessage("test")
        mockkConstructor(TokenAuthentication::class)
        // The token was not authorized by checkAccessToken, so it returns null
        every { anyConstructed<TokenAuthentication>().authenticate(any(), any()) } returns null
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isNull()
    }

    @Test
    fun `test authenticate server2server bad scope`() {
        // (Missing authentication-type header in req!  Means ==> use token auth.)
        val req = MockHttpRequestMessage("test")
        val jwtClaims = mockkClass(Claims::class)
        every { jwtClaims[any()] } returns "foo.bar.report"
        mockkConstructor(TokenAuthentication::class)
        every { anyConstructed<TokenAuthentication>().authenticate(any(), any()) } returns jwtClaims
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isEqualTo("foo")

        every { jwtClaims[any()] } returns "foo.bar"
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isNull()

        every { jwtClaims[any()] } returns "foo"
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isNull()

        every { jwtClaims[any()] } returns ""
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isNull()

        every { jwtClaims[any()] } returns "    "
        assertThat(AuthenticationStrategy.authenticate(req)?.organizationNameClaim)
            .isNull()
    }

    @Test
    fun `test getAccessToken`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders["Authorization"] = "Bearer tok1"
        var tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isEqualTo("tok1")

        req.httpHeaders.clear()
        req.httpHeaders["authorization"] = "Bearer tok2"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isEqualTo("tok2")

        req.httpHeaders.clear()
        req.httpHeaders["AUTHORIZATION"] = "Bearer tok3"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isEqualTo("tok3")

        req.httpHeaders.clear()
        req.httpHeaders["authorization"] = "tok4"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isEqualTo("tok4")

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "Bearer tok5"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "Bearer tok5"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "bearer tok5"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "BEARER tok5"
        tok = AuthenticationStrategy.getAccessToken(req)
        assertThat(tok).isNull()
    }
    @Test
    fun `test validate claims`() {
        val req = MockHttpRequestMessage("test")
        // Missing authentication-type header in req!  Means ==> use token auth.
        val jwtClaims = mockkClass(Claims::class)
        every { jwtClaims[any()] } returns "simple_report.default.report"
        mockkConstructor(TokenAuthentication::class)
        every { anyConstructed<TokenAuthentication>().authenticate(any(), any()) } returns jwtClaims

        val claims = AuthenticationStrategy.authenticate(req)
        val mismatchedSender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val matchingSender = CovidSender(
            "Test Sender",
            "simple_report",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )

        if (claims != null) {
            var result = AuthenticationStrategy.validateClaim(claims, mismatchedSender, req)
            assertThat(result).isFalse()

            result = AuthenticationStrategy.validateClaim(claims, matchingSender, req)
            assertThat(result).isTrue()
        }
    }

    @Test
    fun `test validate claims - isPrimeAdmin`() {
        val req = MockHttpRequestMessage("test")
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships)
        val matchingSender = CovidSender(
            "Test Sender",
            "simple_report",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )

        val result = AuthenticationStrategy.validateClaim(claims, matchingSender, req)
        assertThat(result).isTrue()
    }
}