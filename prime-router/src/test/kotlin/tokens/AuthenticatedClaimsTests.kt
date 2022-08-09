package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import org.junit.jupiter.api.Test

class AuthenticatedClaimsTests {

    @Test
    fun `test constructor`() {
        // First test Okta auth
        // failure cases
        var jwt: Map<String, Any> = mapOf() // empty
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = true) }.isFailure()
        jwt = mapOf("foo" to "bar") // bad
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = true) }.isFailure()
        jwt = mapOf("organization" to "xyz", "sub" to "c@rlos.com") // bad 'organization'
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = true) }.isFailure()
        jwt = mapOf("organization" to listOf("DHSender_xyz")) // missing sub
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = true) }.isFailure()
        jwt = mapOf("sub" to "c@rlos.com") // missing organization
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = true) }.isFailure()

        // success cases
        jwt = mapOf("organization" to listOf("DHxyz"), "sub" to "c@rlos.com")
        var claims = AuthenticatedClaims(jwt, isOktaAuth = true)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()

        jwt = mapOf("organization" to listOf("DHSender_xyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = true)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()

        jwt = mapOf("organization" to listOf("DHPrimeAdmins"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = true)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()

        jwt = mapOf("organization" to listOf("DHPrimeAdmins", "DHxyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = true)
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(2)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()

        jwt = mapOf("organization" to listOf("DHSender_abc", "DHPrimeAdmins", "DHxyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = true)
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(3)
        assertThat(claims.scopes.contains("xyz.*.user")).isTrue()
        assertThat(claims.scopes.contains(Scope.primeAdminScope)).isTrue()
        assertThat(claims.scopes.contains("abc.*.user")).isTrue()

        jwt = mapOf("organization" to listOf("DHSender_abcAdmins", "DHxyzAdmins"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = true)
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(2)
        assertThat(claims.scopes.contains("xyz.*.admin")).isTrue()
        assertThat(claims.scopes.contains("abc.*.admin")).isTrue()

        // server2server auth with badly formed scope.
        jwt = mapOf("scope" to "a.b.c", "sub" to "c@rlos.com")
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = false) }.isFailure()

        // Now test server2server auth with properly formed slcoe
        jwt = mapOf("scope" to "*.*.primeadmin", "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = false)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("*.*.primeadmin")).isTrue()

        // Another test of server2server auth with properly formed scope
        jwt = mapOf("scope" to "a.*.report", "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt, isOktaAuth = false)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.scopes.size).isEqualTo(1)
        assertThat(claims.scopes.contains("a.*.report")).isTrue()

        // server2server Failure cases
        jwt = mapOf() // empty
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = false) }.isFailure()
        jwt = mapOf("sub" to "c@rlos.com") // missing scope
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = false) }.isFailure()
        jwt = mapOf("organization" to "xyz", "sub" to "c@rlos.com") // missing scope
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = false) }.isFailure()
        jwt = mapOf("scope" to "blarg", "sub" to "c@rlos.com") // malformed scope
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = false) }.isFailure()
        jwt = mapOf("foo" to "bar") // general badness
        assertThat { AuthenticatedClaims(jwt, isOktaAuth = false) }.isFailure()
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
    fun `test generateTestJwtClaims`() {
        var jwtClaims = AuthenticatedClaims.generateTestJwtClaims()
        assertThat(jwtClaims["scope"]).isEqualTo(Scope.primeAdminScope)
        assertThat(jwtClaims.subject).isEqualTo("local@test.com")
        val claims = AuthenticatedClaims(jwtClaims, isOktaAuth = false)
        assertThat(claims.isPrimeAdmin).isTrue()
    }

    @Test
    fun `test authorizedForSubmission with Sender obj`() {
        val req = MockHttpRequestMessage("test")
        val rawClaims1: Map<String, Any> = mapOf("scope" to "oh-doh.quux.report", "sub" to "b@b.com")
        val claims1 = AuthenticatedClaims(rawClaims1, isOktaAuth = false)
        val matchingOrgA = CovidSender("quux", "oh-doh", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSubmission(matchingOrgA, req)).isTrue()
        // Org matches, but not sender. Attempt to send to oh-doh.foo.report when my scope is oh-doh.quux.report: Fails.
        val matchingOrgB = CovidSender("foo", "oh-doh", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSubmission(matchingOrgB, req)).isFalse()
        val mismatchingSender = CovidSender("foo", "WRONG", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSubmission(mismatchingSender, req)).isFalse()
        val emptySender = CovidSender("", "", Sender.Format.HL7, schemaName = "one")
        assertThat(claims1.authorizedForSubmission(emptySender, req)).isFalse()

        val rawClaims2: Map<String, Any> = mapOf("scope" to Scope.primeAdminScope, "sub" to "b@b.com")
        val claims2 = AuthenticatedClaims(rawClaims2, isOktaAuth = false)
        assertThat(claims2.authorizedForSubmission(matchingOrgA, req)).isTrue()
        assertThat(claims2.authorizedForSubmission(matchingOrgB, req)).isTrue()
        assertThat(claims2.authorizedForSubmission(mismatchingSender, req)).isTrue()
        assertThat(claims2.authorizedForSubmission(emptySender, req)).isFalse()

        val rawClaims3: Map<String, Any> = mapOf("scope" to "oh-doh.*.admin", "sub" to "b@b.com")
        val claims3 = AuthenticatedClaims(rawClaims3, isOktaAuth = false)
        assertThat(claims3.authorizedForSubmission(matchingOrgA, req)).isTrue()
        assertThat(claims3.authorizedForSubmission(matchingOrgB, req)).isTrue()
        assertThat(claims3.authorizedForSubmission(mismatchingSender, req)).isFalse()
        assertThat(claims3.authorizedForSubmission(emptySender, req)).isFalse()

        val rawClaims4: Map<String, Any> = mapOf("scope" to "oh-doh.*.report", "sub" to "b@b.com")
        val claims4 = AuthenticatedClaims(rawClaims4, isOktaAuth = false)
        assertThat(claims4.authorizedForSubmission(matchingOrgA, req)).isTrue()
        assertThat(claims4.authorizedForSubmission(matchingOrgB, req)).isTrue()
        assertThat(claims4.authorizedForSubmission(mismatchingSender, req)).isFalse()
        assertThat(claims4.authorizedForSubmission(emptySender, req)).isFalse()

        val rawClaims5: Map<String, Any> = mapOf("sub" to "b@b.com") // missing scope
        assertThat { AuthenticatedClaims(rawClaims5, isOktaAuth = false) }.isFailure()

        val rawClaims6: Map<String, Any> = mapOf("scope" to "", "sub" to "b@b.com") // empty scope
        assertThat { AuthenticatedClaims(rawClaims6, isOktaAuth = false) }.isFailure()
    }

    @Test
    fun `test authorizedForSubmission, passing sender and org strings`() {
        val req = MockHttpRequestMessage("test")
        // Very narrowest possible claim:  I'm only allowed this org, this specific sender 'quux' only:
        val rawClaims1: Map<String, Any> = mapOf("scope" to "oh-doh.quux.report", "sub" to "b@b.com")
        val claims1 = AuthenticatedClaims(rawClaims1, isOktaAuth = false)
        assertThat(claims1.authorizedForSubmission("oh-doh", "quux", req)).isTrue()
        // All the rest of these fail because of the narrowness of the claim:
        assertThat(claims1.authorizedForSubmission("oh-doh", "foo", req)).isFalse()
        assertThat(claims1.authorizedForSubmission("oh-doh", "*", req)).isFalse()
        assertThat(claims1.authorizedForSubmission("oh-doh", null, req)).isFalse()
        assertThat(claims1.authorizedForSubmission("WRONG", "foo", req)).isFalse()
        assertThat(claims1.authorizedForSubmission("", "", req)).isFalse()
        assertThat(claims1.authorizedForSubmission(" ", "", req)).isFalse()

        // broadest possible claim:
        val rawClaims2: Map<String, Any> = mapOf("scope" to Scope.primeAdminScope, "sub" to "b@b.com")
        val claims2 = AuthenticatedClaims(rawClaims2, isOktaAuth = false)
        assertThat(claims2.authorizedForSubmission("oh-doh", "quux", req)).isTrue()
        assertThat(claims2.authorizedForSubmission("oh-doh", "foo", req)).isTrue()
        assertThat(claims2.authorizedForSubmission("oh-doh", "*", req)).isTrue()
        assertThat(claims2.authorizedForSubmission("oh-doh", null, req)).isTrue()
        assertThat(claims2.authorizedForSubmission("WRONG", "foo", req)).isTrue()
        assertThat(claims2.authorizedForSubmission("", "", req)).isFalse()
        assertThat(claims2.authorizedForSubmission(" ", "", req)).isFalse()

        val rawClaims3: Map<String, Any> = mapOf("scope" to "oh-doh.*.admin", "sub" to "b@b.com")
        val claims3 = AuthenticatedClaims(rawClaims3, isOktaAuth = false)
        assertThat(claims3.authorizedForSubmission("oh-doh", "quux", req)).isTrue()
        assertThat(claims3.authorizedForSubmission("oh-doh", "foo", req)).isTrue()
        assertThat(claims3.authorizedForSubmission("oh-doh", "*", req)).isTrue()
        assertThat(claims3.authorizedForSubmission("oh-doh", null, req)).isTrue()
        assertThat(claims3.authorizedForSubmission("WRONG", "foo", req)).isFalse()
        assertThat(claims3.authorizedForSubmission("", "", req)).isFalse()
        assertThat(claims3.authorizedForSubmission(" ", "", req)).isFalse()
    }
}