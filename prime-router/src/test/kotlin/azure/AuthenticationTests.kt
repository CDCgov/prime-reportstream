package gov.cdc.prime.router.azure

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthenticationTests {
    private val verifier = OktaAuthenticationVerifier()

    @Test
    fun `test user level checkMembership`() {
        val userMemberships = listOf("DHpima_az_phd")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "pima-az-phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "az-phd"))
    }

    @Test
    fun `test multiple user level checkMembership`() {
        val userMemberships = listOf("DHpima_az_phd", "DHaz_phd")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "pima-az-phd"))
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "az-phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "pa-phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = null))
    }

    @Test
    fun `test user level checkMembership with admin account`() {
        val userMemberships = listOf("DHpima_az_phdAdmins", "DHfoo_ax")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima-az-phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo"))
    }

    @Test
    fun `test user level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima-az-phd"))
        // Any org will return true
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo"))
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertTrue(verifier.checkMembership(multiMemberships, PrincipalLevel.USER, "foo"))
    }

    @Test
    fun `test admin level checkMembership with system account`() {
        // A Prime Admin should always have verified
        val single = listOf("DHPrimeAdmins")
        assertTrue(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd"))
        assertTrue(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
        assertTrue(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, null))
        val multi = listOf("DHPrimeAdmins", "DHfox")
        assertTrue(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
    }

    @Test
    fun `test admin level checkMembership with user account`() {
        // A Prime Admin should always have verified
        val single = listOf("DHaz_phd")
        assertFalse(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "az-phd"))
        assertFalse(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
        assertFalse(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, null))
        val multi = listOf("DHaz_phd", "DHfox")
        assertFalse(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
    }

    @Test
    fun `test admin level checkMembership`() {
        // a pima_az_phd admin should only be valid for pima-az-orgs
        val single = listOf("DHpima_az_phdAdmins")
        assertTrue(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd"))
        assertFalse(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
        assertFalse(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, null))
        val multi = listOf("DHpima_az_phd", "DHpima_az_phdAdmins")
        assertTrue(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd"))
        assertFalse(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
        assertFalse(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, null))
    }

    @Test
    fun `test system level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.SYSTEM_ADMIN, "foo"))
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.SYSTEM_ADMIN, null))
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertTrue(verifier.checkMembership(multiMemberships, PrincipalLevel.SYSTEM_ADMIN, "foo"))
    }
}