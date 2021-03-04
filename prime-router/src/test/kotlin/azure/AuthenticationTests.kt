package gov.cdc.prime.router.azure

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthenticationTests {
    val verifier = OktaAuthenticationVerifier()

    @Test
    fun `test user level checkMembership`() {
        val userMemberships = listOf("DHpima_az_phd")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima_az_phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo"))
    }

    @Test
    fun `test user level checkMembership with admin account`() {
        val userMemberships = listOf("DHpima_az_phdAdmins", "DHfoo_ax")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima_az_phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo"))
    }

    @Test
    fun `test user level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima_az_phd"))
        // Any org will return true
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo"))
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertTrue(verifier.checkMembership(multiMemberships, PrincipalLevel.USER, "foo"))
    }

    @Test
    fun `test admin level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "pima_az_phd"))
        // Any org will return true
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertTrue(verifier.checkMembership(multiMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
    }

    @Test
    fun `test admin level checkMembership`() {
        val userMemberships = listOf("DHpima_az_phdAdmins")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "pima_az_phd"))
        assertFalse(verifier.checkMembership(userMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
        val multiMemberships = listOf("DHpima_az_phd", "DHpima_az_phdAdmins")
        assertTrue(verifier.checkMembership(multiMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "pima_az_phd"))
        assertFalse(verifier.checkMembership(multiMemberships, PrincipalLevel.ORGANIZATION_ADMIN, "foo"))
    }

    @Test
    fun `test system level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertTrue(verifier.checkMembership(userMemberships, PrincipalLevel.SYSTEM_ADMIN, "foo"))
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertTrue(verifier.checkMembership(multiMemberships, PrincipalLevel.SYSTEM_ADMIN, "foo"))
    }
}