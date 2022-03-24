package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class AuthenticationClaimsTests {

    @Test
    fun `test constructor`() {
        // failure cases
        var jwt: Map<String, Any> = mapOf() // empty
        assertThat { AuthenticatedClaims(jwt) }.isFailure()
        jwt = mapOf("foo" to "bar") // bad
        assertThat { AuthenticatedClaims(jwt) }.isFailure()
        jwt = mapOf("organization" to "xyz", "sub" to "c@rlos.com") // bad 'organization'
        assertThat { AuthenticatedClaims(jwt) }.isFailure()
        jwt = mapOf("organization" to listOf("DHSender_xyz")) // missing sub
        assertThat { AuthenticatedClaims(jwt) }.isFailure()
        jwt = mapOf("sub" to "c@rlos.com") // missing organization
        assertThat { AuthenticatedClaims(jwt) }.isFailure()

        // success cases
        jwt = mapOf("organization" to listOf("DHxyz"), "sub" to "c@rlos.com")
        var claims = AuthenticatedClaims(jwt)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.isSenderOrgClaim).isFalse()
        assertThat(claims.organizationNameClaim).isEqualTo("xyz")

        jwt = mapOf("organization" to listOf("DHSender_xyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isFalse()
        assertThat(claims.isSenderOrgClaim).isTrue()
        assertThat(claims.organizationNameClaim).isEqualTo("xyz")

        jwt = mapOf("organization" to listOf("DHPrimeAdmins"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.isSenderOrgClaim).isFalse()
        assertThat(claims.organizationNameClaim).isNull()

        jwt = mapOf("organization" to listOf("DHPrimeAdmins", "DHxyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.isSenderOrgClaim).isFalse()
        assertThat(claims.organizationNameClaim).isEqualTo("xyz")

        jwt = mapOf("organization" to listOf("DHSender_abc", "DHPrimeAdmins", "DHxyz"), "sub" to "c@rlos.com")
        claims = AuthenticatedClaims(jwt)
        assertThat(claims.userName).isEqualTo("c@rlos.com")
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.isSenderOrgClaim).isTrue()
        assertThat(claims.organizationNameClaim).isEqualTo("abc") // "xyz" is ignored
    }

    @Test
    fun `test TestClaims`() {
        var claims = AuthenticatedClaims.generateTestClaims()
        assertThat(claims.userName).isNotNull()
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.isSenderOrgClaim).isTrue()
        assertThat(claims.organizationNameClaim).isEqualTo("ignore")

        claims = AuthenticatedClaims.generateTestClaims("foo")
        assertThat(claims.userName).isNotNull()
        assertThat(claims.isPrimeAdmin).isTrue()
        assertThat(claims.isSenderOrgClaim).isTrue()
        assertThat(claims.organizationNameClaim).isEqualTo("foo")
    }
}