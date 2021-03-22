package gov.cdc.prime.router.secrets

// Option to access credential service by static class
class SecretHelper() {
    companion object {
        fun getSecretService(): SecretService {
            return secretServiceForStorageMethod()
        }
    }
}

// Option to access credential service by interface
interface SecretManagement {
    @Suppress("unused")
    val secretService: SecretService
        get() = secretServiceForStorageMethod()
}

internal fun secretServiceForStorageMethod(): SecretService {
    return when (System.getenv("SECRET_STORAGE_METHOD")) {
        "AZURE" -> AzureSecretService
        else -> EnvVarSecretService
    }
}