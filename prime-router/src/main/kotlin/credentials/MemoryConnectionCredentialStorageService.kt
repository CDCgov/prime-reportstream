package gov.cdc.prime.router.credentials

class MemoryConnectionCredentialStorageService : ConnectionCredentialStorageService {
    val credentialList: HashMap<String, ConnectionCredential>

    init {
        credentialList = HashMap<String, ConnectionCredential>()
    }

    override fun fetchCredential(connectionId: String): ConnectionCredential? {
        return credentialList.get(connectionId)
    }

    override fun saveCredential(connectionId: String, credential: ConnectionCredential) {
        credentialList.put(connectionId, credential)
    }
}