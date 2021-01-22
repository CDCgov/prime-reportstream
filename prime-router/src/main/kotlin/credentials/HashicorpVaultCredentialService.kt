package gov.cdc.prime.router.credentials

object HashicorpVaultCredentialService : CredentialService() {

    override fun fetchCredential(connectionId: String): Credential? {
        throw Exception("TO BE IMPLEMENTED IN #294")
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        throw Exception("TO BE IMPLEMENTED IN #294")
    }
}