package gov.cdc.prime.router.credentials

import io.ktor.client.HttpClient

internal object MemoryCredentialService : CredentialService() {
    private val credentialList: HashMap<String, Credential> = HashMap()

    override fun fetchCredential(connectionId: String, httpClient: HttpClient?): Credential? {
        return credentialList[connectionId]
    }

    override fun saveCredential(connectionId: String, credential: Credential, httpClient: HttpClient?) {
        credentialList[connectionId] = credential
    }
}