package gov.cdc.prime.router.credentials

object AzureCredentialService : CredentialService() {

    override fun fetchCredential(connectionId: String): Credential? {
        throw Exception("TO BE IMPLEMENTED IN #295")
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        throw Exception("TO BE IMPLEMENTED IN #295")
    }
}