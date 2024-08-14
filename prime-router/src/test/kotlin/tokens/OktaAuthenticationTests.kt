package gov.cdc.prime.router.tokens

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.microsoft.azure.functions.HttpMethod
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import kotlin.test.Test

class OktaAuthenticationTests {
    @BeforeEach
    fun reset() {
        clearAllMocks() // If using Companion object mocks, you need to be sure to clear between tests
    }

    @Test
    fun `test authenticate bad token`() {
        // Bad token
        val claims = OktaAuthentication.authenticate("a.b.c", HttpMethod.GET, "foobar")
        assertThat(claims).isNull()
    }

    @Test
    fun `test authenticated claims return from authenticate`() {
        // "Good" token
        val claimsMap = mapOf(
            "sub" to "test",
            "organization" to listOf("DHca-phd")
        )
        mockkObject(OktaAuthentication.Companion)
        every { OktaAuthentication.Companion.decodeJwt(any()) } returns
            TestDefaultJwt(
                "a.b.c",
                Instant.now(),
                Instant.now().plusSeconds(60),
                claimsMap
            )

        val authenticatedClaims = OktaAuthentication.authenticate("a.b.c", HttpMethod.GET, "foobar")
        assertThat(authenticatedClaims).isNotNull()
        assertThat(authenticatedClaims?.userName).isEqualTo("test")
        assertThat(authenticatedClaims?.isPrimeAdmin).isEqualTo(false)
        assertThat(authenticatedClaims?.scopes?.size).isEqualTo(1)
        assertThat(authenticatedClaims?.scopes?.contains("ca-phd.*.user")).isEqualTo(true)
    }

    @Test
    fun `test user level authorizeByMembership`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima-az-phd"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(
                claims,
                PrincipalLevel.USER,
                requiredOrganizationName = "pima-az-phd"
            )
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = "az-phd")
        ).isFalse()
    }

    @Test
    fun `test requiredSenderClaim to authorizeByMembership`() {
        var userMemberships: Map<String, Any> = mapOf("organization" to listOf("DHfoo"), "sub" to "bob@bob.com")
        var claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)

        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "foo", true)).isFalse()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "foo", false)).isTrue()

        userMemberships = mapOf("organization" to listOf("DHSender_foo"), "sub" to "bob@bob.com")
        claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)

        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "foo", true)).isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "foo", false)).isTrue()
    }

    @Test
    fun `test null and empty memberships`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf(""),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = "")
        ).isFalse()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = null)
        ).isFalse()
        assertThat(
            OktaAuthentication.authorizeByMembership(
                claims, PrincipalLevel.USER, requiredOrganizationName = "",
                requireSenderClaim = true
            )
        ).isFalse()
        assertThat(
            OktaAuthentication.authorizeByMembership(
                claims, PrincipalLevel.USER, requiredOrganizationName = null,
                requireSenderClaim = true
            )
        ).isFalse()
    }

    @Test
    fun `test multiple user level authorizeByMembership`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima-az-phd", "DHaz-phd"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication
                .authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = "pima-az-phd")
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = "az-phd")
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = "pa-phd")
        ).isFalse()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, requiredOrganizationName = null)
        ).isFalse()
    }

    @Test
    fun `test user level authorizeByMembership with admin account`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima-az-phdAdmins", "DHfoo_ax"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "pima-az-phd")
        ).isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "foo")).isFalse()
    }

    @Test
    fun `test user level authorizeByMembership with system account`() {
        val userMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(userMemberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "pima-az-phd")
        ).isTrue()
        // Any org will return true
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "foo")).isTrue()
        // val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        val multiMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multiMemberships, AuthenticationType.Okta)
        assertThat(OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.USER, "foo")).isTrue()
    }

    @Test
    fun `test admin level authorizeByMembership with system account`() {
        // A Prime Admin should always have verified
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isTrue()
        // val multi = listOf("DHPrimeAdmins", "DHfox")
        val multiMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multiMemberships, AuthenticationType.Okta)
        assertThat(OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isTrue()
    }

    @Test
    fun `test admin level authorizeByMembership with user account`() {
        // A Prime Admin should always have verified
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHaz_phd"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "az-phd")
        ).isFalse()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isFalse()
        // val multi = listOf("DHaz_phd", "DHfox")
        val multiMemberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHaz_phd", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multiMemberships, AuthenticationType.Okta)
        assertThat(OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
    }

    @Test
    fun `test admin level authorizeByMembership`() {
        // a pima_az_phd admin should only be valid for pima-az-orgs
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima-az-phdAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isFalse()
        val multi: Map<String, Any> = mapOf(
            "organization" to listOf("DHpima-az-phd", "DHpima-az-phdAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multi, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
            .isFalse()
        assertThat(OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.ORGANIZATION_ADMIN, null))
            .isFalse()
    }

    @Test
    fun `test dashes and underscores in authorizeByMembership`() {
        var memberships: Map<String, Any> = mapOf(
            "organization" to listOf("DHthe-good-old-boys"),
            "sub" to "bob@bob.com"
        )
        var claims = AuthenticatedClaims(memberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "the-good-old-boys")
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "the_good_old_boys")
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "the_good-old_boys")
        ).isTrue()
        memberships = mapOf(
            "organization" to listOf("DHSender_bobs_country_bunker"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(memberships, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "bobs_country_bunker")
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "bobs-country-bunker")
        ).isTrue()
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.USER, "bobs_country-bunker")
        ).isTrue()
    }

    @Test
    fun `test system level authorizeByMembership with system account`() {
        // val userMemberships = listOf("DHPrimeAdmins")
        val single: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        val claims = AuthenticatedClaims(single, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.SYSTEM_ADMIN, "foo")
        ).isTrue()
        assertThat(OktaAuthentication.authorizeByMembership(claims, PrincipalLevel.SYSTEM_ADMIN, null)).isTrue()
        val multi: Map<String, Any> = mapOf(
            "organization" to listOf("DHPrimeAdmins", "DHfox"),
            "sub" to "bob@bob.com"
        )
        val claims2 = AuthenticatedClaims(multi, AuthenticationType.Okta)
        assertThat(
            OktaAuthentication.authorizeByMembership(claims2, PrincipalLevel.SYSTEM_ADMIN, "foo")
        ).isTrue()
    }
}