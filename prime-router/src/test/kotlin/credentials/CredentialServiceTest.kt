package gov.cdc.prime.router.credentials

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CredentialServiceTest {

    @Test
    fun `test save and fetch`() {
        val credentialService = MemoryCredentialService()

        val connectionId = "id1"
        val credential = UserPassCredential("user1", "pass1")
        credentialService.saveCredential(connectionId, credential, "CredentialServiceTest")

        val retVal = credentialService.fetchCredential(connectionId, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST)
        assertEquals(credential, retVal, "Credential service did not return expected credential")
    }
}