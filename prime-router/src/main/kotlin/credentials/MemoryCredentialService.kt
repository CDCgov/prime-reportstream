package gov.cdc.prime.router.credentials

class MemoryCredentialService : CredentialService() {
    val credentialList: HashMap<String, Credential>

    init {
        credentialList = HashMap<String, Credential>()
    }

    override fun fetchCredential(connectionId: String): Credential? {
        return credentialList.get(connectionId)
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        credentialList.put(connectionId, credential)
    }
}