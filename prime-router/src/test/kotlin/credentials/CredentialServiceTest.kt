package gov.cdc.prime.router.credentials

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CredentialServiceTest {

    @Test
    fun `test save and fetch`() {
        val credentialService = MemoryCredentialService()

        val connectionId = "id1"
        val credential = UserPassCredential("user1", "pass1")
        credentialService.saveCredential(connectionId, credential)

        val retVal = credentialService.fetchCredential(connectionId)
        assertEquals(credential, retVal, "Credential service did not return expected credential")
    }
}