package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.microsoft.azure.functions.HttpMethod
import gov.cdc.prime.router.common.Environment
import io.mockk.every
import io.mockk.mockkObject
import kotlin.test.Test

class OktaAuthenticationTests {
    private val verifier = OktaAuthentication(PrincipalLevel.USER)

    @Test
    fun `test authenticate`() {
        // This should trigger the local auth
        var claims = verifier.authenticate(null, HttpMethod.GET, "foobar")
        assertThat(claims).isNotNull()
        assertThat(claims?.isPrimeAdmin).isEqualTo(true)
        assertThat(claims?.isSenderOrgClaim).isEqualTo(true)
        assertThat(claims?.organizationNameClaim).isEqualTo("ignore")

        // Bad token
        claims = verifier.authenticate("a.b.c", HttpMethod.GET, "foobar")
        assertThat(claims).isNull()
    }

    @Test
    fun `test isLocal`() {
        mockkObject(Environment) {
            every { Environment.isLocal() } returns false
            assertThat(verifier.isLocal(null)).isFalse()
            assertThat(verifier.isLocal("")).isFalse()
            assertThat(verifier.isLocal("abc")).isFalse()
            assertThat(verifier.isLocal("a.b.c")).isFalse()

            every { Environment.isLocal() } returns true
            assertThat(verifier.isLocal(null)).isTrue()
            assertThat(verifier.isLocal("")).isTrue()
            assertThat(verifier.isLocal("abc")).isTrue()
            assertThat(verifier.isLocal("a.b.c")).isFalse()
        }
    }

    @Test
    fun `test user level authorizeByMembership`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima_az_phd"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.USER, organizationName = "pima-az-phd")
        ).isTrue()
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.USER, organizationName = "az-phd")
        ).isFalse()
    }

    @Test
    fun `test multiple user level authorizeByMembership`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima_az_phd", "DHaz_phd"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships)
        assertThat(
            (verifier.authorizeByMembership(claims, PrincipalLevel.USER, organizationName = "pima-az-phd"))
        ).isTrue()
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.USER, organizationName = "az-phd")
        ).isTrue()
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.USER, organizationName = "pa-phd")
        ).isFalse()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.USER, organizationName = null)).isFalse()
    }

    @Test
    fun `test user level authorizeByMembership with admin account`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima_az_phdAdmins", "DHfoo_ax"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.USER, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.USER, "foo")).isFalse()
    }

    @Test
    fun `test user level authorizeByMembership with system account`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.USER, "pima-az-phd")
        ).isTrue()
        // Any org will return true
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.USER, "foo")).isTrue()
        // val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        val multiMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multiMemberships)
        assertThat(verifier.authorizeByMembership(claims2, PrincipalLevel.USER, "foo")).isTrue()
    }

    @Test
    fun `test admin level authorizeByMembership with system account`() {
        // A Prime Admin should always have verified
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isTrue()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isTrue()
        // val multi = listOf("DHPrimeAdmins", "DHfox")
        val multiMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multiMemberships)
        assertThat(verifier.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isTrue()
    }

    @Test
    fun `test admin level authorizeByMembership with user account`() {
        // A Prime Admin should always have verified
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHaz_phd"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "az-phd")
        ).isFalse()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isFalse()
        // val multi = listOf("DHaz_phd", "DHfox")
        val multiMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHaz_phd", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multiMemberships)
        assertThat(verifier.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
    }

    @Test
    fun `test admin level authorizeByMembership`() {
        // a pima_az_phd admin should only be valid for pima-az-orgs
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima_az_phdAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isFalse()
        // val multi = listOf("DHpima_az_phd", "DHpima_az_phdAdmins")
        val multi: Map<String, Any> = mapOf(
            "organization" to listOf("DHpime_az_phd", "DHpima_az_phdAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multi)
        assertThat(
            verifier.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
        assertThat(verifier.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isFalse()
    }

    @Test
    fun `test system level authorizeByMembership with system account`() {
        // val userMemberships = listOf("DHPrimeAdmins")
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single)
        assertThat(
            verifier.authorizeByMembership(claims, PrincipalLevel.SYSTEM_ADMIN, "foo")
        ).isTrue()
        assertThat(verifier.authorizeByMembership(claims, PrincipalLevel.SYSTEM_ADMIN, null)).isTrue()
        // val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        val multi: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multi)
        assertThat(
            verifier.authorizeByMembership(claims2, PrincipalLevel.SYSTEM_ADMIN, "foo")
        ).isTrue()
    }
}