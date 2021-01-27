package gov.cdc.prime.router.credentials

interface CredentialManagement {
    @Suppress("unused")
    val credentialService: CredentialService
        get() = credentialServiceForStorageMethod()
}

internal fun credentialServiceForStorageMethod(): CredentialService {
    return when (System.getenv("CREDENTIAL_STORAGE_METHOD")) {
        "AZURE" -> AzureCredentialService
        "HASHICORP_VAULT" -> HashicorpVaultCredentialService
        else -> MemoryCredentialService
    }
}