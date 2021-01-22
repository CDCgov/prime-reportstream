package gov.cdc.prime.router.credentials

interface ConnectionCredentialStorageService {
    fun fetchCredential(connectionId: String): ConnectionCredential?
    fun saveCredential(connectionId: String, credential: ConnectionCredential)
}