package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.common.Environment
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthenticatedClaimsTests {
    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test constructor`() {
        // First test Okta auth
        // failure cases
        var jwt: Map<String, Any> = mapOf() // empty
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Okta) }.isFailure()
        jwt = mapOf("foo" to "bar") // bad
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Okta) }.isFailure()
        jwt = mapOf("organization" to "xyz", "sub" to "c@rlos.com") // bad 'organization'
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Okta) }.isFailure()
        jwt = mapOf("organization" to listOf("DHSender_xyz")) // missing sub
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Okta) }.isFailure()
        jwt = mapOf("sub" to "c@rlos.com") // missing organization
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Okta) }.isFailure()

        // success cases
        jwt = mapOf("organization" to listOf("DHxyz"), "sub" to "c@rlos.com")
        var claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()

        jwt = mapOf("organization" to listOf("DHSender_xyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()

        jwt = mapOf("organization" to listOf("DHPrimeAdmins"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()

        jwt = mapOf("organization" to listOf("DHPrimeAdmins", "DHxyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(2)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()

        jwt = mapOf("organization" to listOf("DHSender_abc", "DHPrimeAdmins", "DHxyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(3)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()
        assertThat(claims.scopes.contains("abc.*.user")).isTrue()

        jwt = mapOf("organization" to listOf("DHSender_abcAdmins", "DHxyzAdmins"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(2)
        assertThat(claims.scopes.contains("xyz.*.admin")).isTrue()
        assertThat(claims.scopes.contains("abc.*.admin")).isTrue()

        // server2server auth with badly formed scope.
        jwt = mapOf("scope" to "a.b.c", "sub" to "c@rlos.com")
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Server2Server) }.isFailure()

        // Now test server2server auth with properly formed slcoe
        jwt = mapOf("scope" to "*.*.primeadmin", "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("*.*.primeadmin")).isTrue()

        // Another test of server2server auth with properly formed scope
        jwt = mapOf("scope" to "a.*.report", "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("a.*.report")).isTrue()

        // server2server Failure cases
        jwt = mapOf() // empty
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Server2Server) }.isFailure()
        jwt = mapOf("sub" to "c@rlos.com") // missing scope
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Server2Server) }.isFailure()
        jwt = mapOf("organization" to "xyz", "sub" to "c@rlos.com") // missing scope
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Server2Server) }.isFailure()
        jwt = mapOf("scope" to "blarg", "sub" to "c@rlos.com") // malformed scope
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Server2Server) }.isFailure()
        jwt = mapOf("foo" to "bar") // general badness
        assertThat { AuthenticatedClaims(jwt, AuthenticationType.Server2Server) }.isFailure()
    }

    @Test
    fun `test generateTestClaims`() {
        var claims = AuthenticatedClaims.generateTestClaims()
        assertThat(claims.userName).isNotNull()
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(2)
        assertThat(claims.scopes.contains("ignore.*.user")).isTrue()
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()

        val sender = CovidSender(
            "mySenderName",
            "myOrgName",
            Sender.Format.CSV,
            CustomerStatus.INACTIVE,
            "mySchema",
            keys = null
        )
        claims = AuthenticatedClaims.generateTestClaims(sender)
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(2)
        assertThat(claims.scopes.contains("myOrgName.*.user")).isTrue()
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()
    }

    @Test
    fun `test authorizedForSubmission with Sender obj`() {
        val req = MockHttpRequestMessage("test")
        val rawClaims1: Map<String, Any> = mapOf("scope" to "oh-doh.quux.report", "sub" to "b@b.com")
        val claims1 = AuthenticatedClaims(rawClaims1, AuthenticationType.Server2Server)
        val matchingOrgA = CovidSender("quux", "oh-doh", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSendOrReceive(matchingOrgA, req)).isTrue()
        // Org matches, but not sender. Attempt to send to oh-doh.foo.report when my scope is oh-doh.quux.report: Fails.
        val matchingOrgB = CovidSender("foo", "oh-doh", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSendOrReceive(matchingOrgB, req)).isFalse()
        val mismatchingSender = CovidSender("foo", "WRONG", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSendOrReceive(mismatchingSender, req)).isFalse()
        val emptySender = CovidSender("", "", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSendOrReceive(emptySender, req)).isFalse()

        val rawClaims2: Map<String, Any> = mapOf("scope" to Scope.primeAdminScope, "sub" to "b@b.com")
        val claims2 = AuthenticatedClaims(rawClaims2, AuthenticationType.Server2Server)
        assertThat(claims2.authorizedForSendOrReceive(matchingOrgA, req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive(matchingOrgB, req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive(mismatchingSender, req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive(emptySender, req)).isTrue()

        val rawClaims3: Map<String, Any> = mapOf("scope" to "oh-doh.*.admin", "sub" to "b@b.com")
        val claims3 = AuthenticatedClaims(rawClaims3, AuthenticationType.Server2Server)
        assertThat(claims3.authorizedForSendOrReceive(matchingOrgA, req)).isTrue()
        assertThat(claims3.authorizedForSendOrReceive(matchingOrgB, req)).isTrue()
        assertThat(claims3.authorizedForSendOrReceive(mismatchingSender, req)).isFalse()
        assertThat(claims3.authorizedForSendOrReceive(emptySender, req)).isFalse()

        val rawClaims4: Map<String, Any> = mapOf("scope" to "oh-doh.*.report", "sub" to "b@b.com")
        val claims4 = AuthenticatedClaims(rawClaims4, AuthenticationType.Server2Server)
        assertThat(claims4.authorizedForSendOrReceive(matchingOrgA, req)).isTrue()
        assertThat(claims4.authorizedForSendOrReceive(matchingOrgB, req)).isTrue()
        assertThat(claims4.authorizedForSendOrReceive(mismatchingSender, req)).isFalse()
        assertThat(claims4.authorizedForSendOrReceive(emptySender, req)).isFalse()

        val rawClaims5: Map<String, Any> = mapOf("sub" to "b@b.com") // missing scope
        assertThat { AuthenticatedClaims(rawClaims5, AuthenticationType.Server2Server) }.isFailure()

        val rawClaims6: Map<String, Any> = mapOf("scope" to "", "sub" to "b@b.com") // empty scope
        assertThat { AuthenticatedClaims(rawClaims6, AuthenticationType.Server2Server) }.isFailure()
    }

    @Test
    fun `test authorizedForSubmission, passing sender and org strings`() {
        val req = MockHttpRequestMessage("test")
        // Very narrowest possible claim:  I'm only allowed this org, this specific sender 'quux' only:
        val rawClaims1: Map<String, Any> = mapOf("scope" to "oh-doh.quux.report", "sub" to "b@b.com")
        val claims1 = AuthenticatedClaims(rawClaims1, AuthenticationType.Server2Server)
        assertThat(claims1.authorizedForSendOrReceive("oh-doh", "quux", req)).isTrue()
        // All the rest of these fail because of the narrowness of the claim:
        assertThat(claims1.authorizedForSendOrReceive("oh-doh", "foo", req)).isFalse()
        assertThat(claims1.authorizedForSendOrReceive("oh-doh", "*", req)).isFalse()
        assertThat(claims1.authorizedForSendOrReceive("oh-doh", null, req)).isFalse()
        assertThat(claims1.authorizedForSendOrReceive("WRONG", "foo", req)).isFalse()
        assertThat(claims1.authorizedForSendOrReceive("", "", req)).isFalse()
        assertThat(claims1.authorizedForSendOrReceive(" ", "", req)).isFalse()

        // broadest possible claim:
        val rawClaims2: Map<String, Any> = mapOf("scope" to Scope.primeAdminScope, "sub" to "b@b.com")
        val claims2 = AuthenticatedClaims(rawClaims2, AuthenticationType.Server2Server)
        assertThat(claims2.authorizedForSendOrReceive("oh-doh", "quux", req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive("oh-doh", "foo", req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive("oh-doh", "*", req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive("oh-doh", null, req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive("WRONG", "foo", req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive("", "", req)).isTrue()
        assertThat(claims2.authorizedForSendOrReceive(" ", "", req)).isTrue()

        val rawClaims3: Map<String, Any> = mapOf("scope" to "oh-doh.*.admin", "sub" to "b@b.com")
        val claims3 = AuthenticatedClaims(rawClaims3, AuthenticationType.Server2Server)
        assertThat(claims3.authorizedForSendOrReceive("oh-doh", "quux", req)).isTrue()
        assertThat(claims3.authorizedForSendOrReceive("oh-doh", "foo", req)).isTrue()
        assertThat(claims3.authorizedForSendOrReceive("oh-doh", "*", req)).isTrue()
        assertThat(claims3.authorizedForSendOrReceive("oh-doh", null, req)).isTrue()
        assertThat(claims3.authorizedForSendOrReceive("WRONG", "foo", req)).isFalse()
        assertThat(claims3.authorizedForSendOrReceive("", "", req)).isFalse()
        assertThat(claims3.authorizedForSendOrReceive(" ", "", req)).isFalse()

        // A typical server2server sender scope:
        val rawClaims4: Map<String, Any> = mapOf("scope" to "sender-org.*.report", "sub" to "b@b.com")
        val claims4 = AuthenticatedClaims(rawClaims4, AuthenticationType.Server2Server)
        assertThat(claims4.authorizedForSendOrReceive("sender-org", "quux", req)).isTrue()
        // All the rest of these fail because of the narrowness of the claim:
        assertThat(claims4.authorizedForSendOrReceive("sender-org", "foo", req)).isTrue()
        assertThat(claims4.authorizedForSendOrReceive("sender-org", "*", req)).isTrue()
        assertThat(claims4.authorizedForSendOrReceive("sender-org", null, req)).isTrue()
        assertThat(claims4.authorizedForSendOrReceive("WRONG", null, req)).isFalse()
        assertThat(claims4.authorizedForSendOrReceive("", "", req)).isFalse()
        assertThat(claims4.authorizedForSendOrReceive(" ", "", req)).isFalse()

        // Server2server senders should be given a scope of the form sender-org.*.report, but
        // we are grandfathering-in the older sender-org.default.report form, as identical to sender-org.*.report
        val rawClaims5: Map<String, Any> = mapOf("scope" to "sender-org.default.report", "sub" to "b@b.com")
        val claims5 = AuthenticatedClaims(rawClaims5, AuthenticationType.Server2Server)
        assertThat(claims5.authorizedForSendOrReceive("sender-org", "quux", req)).isTrue()
        // All the rest of these fail because of the narrowness of the claim:
        assertThat(claims5.authorizedForSendOrReceive("sender-org", "default", req)).isTrue()
        assertThat(claims5.authorizedForSendOrReceive("sender-org", "*", req)).isTrue()
        assertThat(claims5.authorizedForSendOrReceive("sender-org", null, req)).isTrue()
        assertThat(claims5.authorizedForSendOrReceive("WRONG", null, req)).isFalse()
        assertThat(claims5.authorizedForSendOrReceive("", "", req)).isFalse()
        assertThat(claims5.authorizedForSendOrReceive(" ", "", req)).isFalse()
    }

    @Test
    fun `test isLocal`() {
        mockkObject(Environment) {
            every { Environment.isLocal() } returns false
            assertThat(AuthenticatedClaims.isLocal(null)).isFalse()
            assertThat(AuthenticatedClaims.isLocal("")).isFalse()
            assertThat(AuthenticatedClaims.isLocal("abc")).isFalse()
            assertThat(AuthenticatedClaims.isLocal("a.b.c")).isFalse()

            every { Environment.isLocal() } returns true
            assertThat(AuthenticatedClaims.isLocal(null)).isTrue()
            assertThat(AuthenticatedClaims.isLocal("")).isTrue()
            assertThat(AuthenticatedClaims.isLocal("abc")).isTrue()
            assertThat(AuthenticatedClaims.isLocal("a.b.c")).isFalse()
        }
    }

    @Test
    fun `test authenticate local auth path`() {
        // This should trigger the local auth
        val req = MockHttpRequestMessage()
        var claims = AuthenticatedClaims.authenticate(req)
        assertThat(claims).isNotNull()
        assertThat(claims?.isPrimeAdmin).isEqualTo(true)
        assertThat(claims?.scopes?.size).isEqualTo(2)
        assertThat(claims?.scopes?.contains("ignore.*.user")).isEqualTo(true)
        assertThat(claims?.scopes?.contains(Scope.primeAdminScope)).isEqualTo(true)
    }

    @Test
    fun `test authenticate okta happy path`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            "authorization" to "Bearer a.b.c", // Force our way past the Bearer token check
        )
        // Force our way past the isLocal() test.
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false

        // Okta style token. There's a logger call that uses 'sub'
        val jwt = mapOf(oktaMembershipClaim to listOf("DHblarg"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        mockkObject(OktaAuthentication)
        every { OktaAuthentication.authenticate(any(), any(), any()) } returns claims
        assertThat(AuthenticatedClaims.authenticate(req)).isEqualTo(claims)
    }

    @Test
    fun `test authenticate okta fails`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            "authorization" to "Bearer a.b.c", // Force our way past the Bearer token check
        )
        // Force our way past the isLocal() test.
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false

        mockkObject(OktaAuthentication)
        // Okta auth fails, so it returns null
        every { OktaAuthentication.authenticate(any(), any(), any()) } returns null
        assertThat(AuthenticatedClaims.authenticate(req)).isNull()
        verify(exactly = 1) { OktaAuthentication.authenticate(any(), any(), any()) }
    }

    @Test
    fun `test authenticate bad bearer token fails`() {
        val req = MockHttpRequestMessage("test")
        // Force our way past the isLocal() test.
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false

        mockkObject(OktaAuthentication)
        assertThat(AuthenticatedClaims.authenticate(req)).isNull()
        // The null failure is caused by the bad bearer token, not by okta failure
        verify(exactly = 0) { OktaAuthentication.authenticate(any(), any(), any()) }
    }

    @Test
    fun `test authenticate server2server happy path`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            "authorization" to "Bearer a.b.c",
        )

        val jwt = mapOf("scope" to "foo.bar.report", "sub" to "c@rlos.com")
        val claimsIn = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)
        mockkConstructor(Server2ServerAuthentication::class)
        every { anyConstructed<Server2ServerAuthentication>().authenticate(any(), any()) } returns claimsIn
        // Force past the isLocal test.
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false
        val claims1 = AuthenticatedClaims.authenticate(req)
        assertThat(claims1?.scopes?.size).isEqualTo(1)
        assertThat(claims1?.scopes?.contains("foo.bar.report")).isEqualTo(true)
        assertThat(claims1?.isPrimeAdmin).isEqualTo(false)

        // Incorrect authentication-type header.  Still means ==> use token auth; not treated as an error
        req.httpHeaders += mapOf("authentication-type" to "bogus")
        val claims2 = AuthenticatedClaims.authenticate(req)
        assertThat(claims2?.scopes?.size).isEqualTo(1)
        assertThat(claims2?.scopes?.contains("foo.bar.report")).isEqualTo(true)
        assertThat(claims2?.isPrimeAdmin).isEqualTo(false)
    }

    @Test
    fun `test authenticate server2server auth fails`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            "authorization" to "Bearer a.b.c",
        )
        mockkConstructor(Server2ServerAuthentication::class)
        // Force past the isLocal test.
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false
        // The token was not authorized by checkAccessToken, so it returns null
        every { anyConstructed<Server2ServerAuthentication>().authenticate(any(), any()) } returns null
        assertThat(AuthenticatedClaims.authenticate(req)).isNull()
    }

    @Test
    fun `test getAccessToken`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders["Authorization"] = "Bearer tok1"
        var tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isEqualTo("tok1")

        req.httpHeaders.clear()
        req.httpHeaders["authorization"] = "Bearer tok2"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isEqualTo("tok2")

        req.httpHeaders.clear()
        req.httpHeaders["AUTHORIZATION"] = "Bearer tok3"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isEqualTo("tok3")

        req.httpHeaders.clear()
        req.httpHeaders["authorization"] = "tok4"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isEqualTo("tok4")

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "Bearer tok5"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "Bearer tok5"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "bearer tok5"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isNull()

        req.httpHeaders.clear()
        req.httpHeaders["foobar"] = "BEARER tok5"
        tok = AuthenticatedClaims.getAccessToken(req)
        assertThat(tok).isNull()
    }
    @Test
    fun `test authorizeForSubmission with claims run through authenticate`() {
        val req = MockHttpRequestMessage("test")
        req.httpHeaders += mapOf(
            "authorization" to "Bearer a.b.c",
        )
        mockkConstructor(Server2ServerAuthentication::class)
        // Force our way past the isLocal() test.
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false

        val jwt = mapOf("scope" to "xxx.*.report", "sub" to "c@rlos.com")
        val claimsIn = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)

        mockkConstructor(Server2ServerAuthentication::class)
        every { anyConstructed<Server2ServerAuthentication>().authenticate(any(), any()) } returns claimsIn
        val claimsOut = AuthenticatedClaims.authenticate(req)
        assertThat(claimsOut).isNotNull()
        assertThat(claimsOut?.scopes?.contains("xxx.*.report")).isEqualTo(true)
        val mismatchedOrg = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val matchingOrg = CovidSender(
            "Test Sender",
            "xxx",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        if (claimsOut != null) {
            assertThat(claimsOut.authorizedForSendOrReceive(mismatchedOrg, req)).isFalse()
            assertThat(claimsOut.authorizedForSendOrReceive(matchingOrg, req)).isTrue()
        }
    }
}