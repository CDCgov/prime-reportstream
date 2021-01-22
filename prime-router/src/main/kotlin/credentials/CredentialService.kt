package gov.cdc.prime.router.credentials

interface CredentialService {
    fun fetchCredential(connectionId: String): Credential?
    fun saveCredential(connectionId: String, credential: Credential)
}