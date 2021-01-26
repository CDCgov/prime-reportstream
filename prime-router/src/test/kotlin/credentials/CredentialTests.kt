package gov.cdc.prime.router.credentials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class CredentialTests {

    @Test
    fun `test polymorphic serialization of UserPassCredential`() {
        val credential = UserPassCredential("user", "pass")
        val expectedJson = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"pass\"}"
        assertEquals(expectedJson, credential.toJSON(), "Generated JSON does not match")
    }

    @Test
    fun `test polymorphic deserialization of UserPassCredential`() {
        val json = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"pass\"}"
        val credential = Credential.fromJSON(json)
        assertTrue(credential is UserPassCredential, "Deserialized class is not UserPassCredential")
        assertEquals("user", credential.user, "User did not match")
        assertEquals("pass", credential.pass, "Pass did not match")
    }
}