package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import io.jsonwebtoken.Claims
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthenticationStrategyTests {

    private val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    private val connection = MockConnection(dataProvider)
    private val accessSpy = spyk(DatabaseAccess(connection))

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test authenticate okta happy path`() {
        val req = MockHttpRequestMessage("test")
        val requiredScope = "not used by okta auth"
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
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)).isEqualTo(claims)
    }

    @Test
    fun `test authenticate okta fails`() {
        val req = MockHttpRequestMessage("test")
        val requiredScope = "not used by okta auth"
        req.httpHeaders += mapOf(
            "authentication-type" to "okta"
        )
        mockkObject(OktaAuthentication)
        // Okta auth fails, so it returns null
        every { OktaAuthentication.authenticate(any()) } returns null
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)).isNull()
    }

    @Test
    fun `test authenticate twolegged happy path`() {
        val req = MockHttpRequestMessage("test")
        val requiredScope = "simple_report.default.report"
        // Missing authentication-type header in req!  Means ==> use token auth.
        val jwtClaims = mockkClass(Claims::class)
        every { jwtClaims[any()] } returns "foo" // there's a logger call that looks up jwtClaims[sub]
        mockkConstructor(TokenAuthentication::class)
        every { anyConstructed<TokenAuthentication>().checkAccessToken(any(), any()) } returns jwtClaims
        val claims = AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)
        assertThat(claims?.organizationNameClaim).isEqualTo("simple_report")
        assertThat(claims?.userName).isEqualTo("foo")
        assertThat(claims?.isSenderOrgClaim).isEqualTo(false)
        assertThat(claims?.isPrimeAdmin).isEqualTo(false)

        // Incorrect authentication-type header! Means ==> use token auth; not treated as an error
        req.httpHeaders += mapOf("authentication-type" to "bogus")
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isEqualTo("simple_report")
    }

    @Test
    fun `test authenticate twolegged auth fails`() {
        val req = MockHttpRequestMessage("test")
        val requiredScope = "simple_report.default.report"
        mockkConstructor(TokenAuthentication::class)
        // The token was not authorized by checkAccessToken, so it returns null
        every { anyConstructed<TokenAuthentication>().checkAccessToken(any(), any()) } returns null
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isNull()
    }

    @Test
    fun `test authenticate twolegged bad scope`() {
        // (Missing authentication-type header in req!  Means ==> use token auth.)
        val req = MockHttpRequestMessage("test")
        var requiredScope = "blarg.default.report"
        val jwtClaims = mockkClass(Claims::class)
        every { jwtClaims[any()] } returns "foo"
        mockkConstructor(TokenAuthentication::class)
        every { anyConstructed<TokenAuthentication>().checkAccessToken(any(), any()) } returns jwtClaims
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isEqualTo("blarg")

        requiredScope = "blarg.report" // bad scope format
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isNull()

        requiredScope = "blarg" // bad scope format
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isNull()

        requiredScope = "" // bad scope format
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isNull()

        requiredScope = "   " // bad scope format
        assertThat(AuthenticationStrategy.authenticate(req, requiredScope, accessSpy)?.organizationNameClaim)
            .isNull()
    }
}