package gov.cdc.prime.router.credentials

internal object MemoryCredentialService : CredentialService() {
    private val credentialList: HashMap<String, Credential> = HashMap()

    override fun fetchCredential(connectionId: String): Credential? {
        return credentialList[connectionId]
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        credentialList[connectionId] = credential
    }
}