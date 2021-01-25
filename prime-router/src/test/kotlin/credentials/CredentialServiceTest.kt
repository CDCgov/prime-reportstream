package gov.cdc.prime.router.credentials

import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class CredentialServiceTest : CredentialManagement {

    val VALID_CONNECTION_IDS = listOf("valid1", "35wtfsdfe4t4wr4w4343", "with_underscores-and-dashes")
    val INVALID_CONNECTION_IDS = listOf("slashes/are/not/allowed", "no spaces", "?andotherthings")
    val VALID_CREDENTIAL = UserPassCredential("user1", "pass1")

    @Test
    fun `test save and fetch`() {
        val connectionId = "id1"
        credentialService.saveCredential(connectionId, VALID_CREDENTIAL, "CredentialServiceTest")

        val retVal = credentialService.fetchCredential(connectionId, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST)
        assertEquals(VALID_CREDENTIAL, retVal, "Credential service did not return expected credential")
    }

    @Test
    fun `test fetchCredential handles valid connectionIds`() {
        VALID_CONNECTION_IDS.forEach {
            credentialService.fetchCredential(it, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST)
        }
    }

    @Test
    fun `test fetchCredential throws IllegalArgumentException with non-url safe connectionIds`() {
        INVALID_CONNECTION_IDS.forEach {
            try {
                credentialService.fetchCredential(it, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST)
                fail("IllegalArgumentException not thrown for $it")
            } catch ( e: IllegalArgumentException ) {
                assertEquals("connectionId must match: ^[a-zA-Z0-9_-]*\$", e.message)
            }
        }
    }

    @Test
    fun `test saveCredential handles valid connectionIds`() {
        VALID_CONNECTION_IDS.forEach {
            credentialService.saveCredential(it, VALID_CREDENTIAL,"CredentialServiceTest")
        }
    }

    @Test
    fun `test saveCredential throws IllegalArgumentException with non-url safe connectionIds`() {
        INVALID_CONNECTION_IDS.forEach {
            try {
                credentialService.saveCredential(it, VALID_CREDENTIAL, "CredentialServiceTest")
                fail("IllegalArgumentException not thrown for $it")
            } catch ( e: IllegalArgumentException ) {
                assertEquals("connectionId must match: ^[a-zA-Z0-9_-]*\$", e.message)
            }
        }
    }
}