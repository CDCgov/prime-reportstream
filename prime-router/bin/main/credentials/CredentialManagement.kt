package gov.cdc.prime.router.credentials

// Option to access credential service by static class
class CredentialHelper() {
    companion object {
        fun getCredentialService(): CredentialService {
            return credentialServiceForStorageMethod()
        }

        fun formCredentialLabel(fromReceiverName: String): String {
            return fromReceiverName
                .replace(".", "--")
                .replace("_", "-")
                .uppercase()
        }
    }
}

// Option to access credential service by interface
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