package gov.cdc.prime.router.secrets

abstract class SecretService {
    companion object {
        private val URL_SAFE_KEY_PATTERN = Regex("^[a-zA-Z0-9-_]*$")
    }

    protected abstract fun fetchSecretFromStore(secretName: String): String?

    fun fetchSecret(secretName: String): String? {
        require(URL_SAFE_KEY_PATTERN.matches(secretName)) {
            "secretName must match: ${URL_SAFE_KEY_PATTERN.pattern}"
        }
        return fetchSecretFromStore(secretName)
    }
}