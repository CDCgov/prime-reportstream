package gov.cdc.prime.router.secrets

internal object EnvVarSecretService : SecretService() {
    override fun fetchSecretFromStore(secretName: String): String? = fetchEnvironmentVariable(secretName)

    internal fun fetchEnvironmentVariable(secretName: String): String? = System.getenv(secretName)
}