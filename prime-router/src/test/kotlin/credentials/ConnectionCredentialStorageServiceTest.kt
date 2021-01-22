package gov.cdc.prime.router.credentials

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConnectionCredentialStorageServiceTest {

    @Test
    fun `test save and fetch`() {
        val storageService = MemoryConnectionCredentialStorageService()

        val connectionId = "id1"
        val credential = UserPassConnectionCredential("user1", "pass1")
        storageService.saveCredential(connectionId, credential)

        val retVal = storageService.fetchCredential(connectionId)
        assertEquals(credential, retVal, "Storage service did not return expected credential")
    }
}