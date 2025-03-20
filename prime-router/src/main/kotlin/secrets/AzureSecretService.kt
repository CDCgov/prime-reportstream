package gov.cdc.prime.router.secrets

import com.azure.core.credential.TokenCredential
import com.azure.core.http.policy.ExponentialBackoff
import com.azure.core.http.policy.RetryPolicy
import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.security.keyvault.secrets.SecretClient
import com.azure.security.keyvault.secrets.SecretClientBuilder
import java.time.Duration

internal object AzureSecretService : SecretService() {
    private val KEY_VAULT_NAME: String by lazy { System.getenv("SECRET_KEY_VAULT_NAME") ?: "" }
    private val secretClient by lazy { initSecretClient() }

    internal fun initSecretClient(
        secretClientBuilder: SecretClientBuilder = SecretClientBuilder(),
        credential: TokenCredential = DefaultAzureCredentialBuilder().build(),
    ): SecretClient = secretClientBuilder
            .vaultUrl("https://$KEY_VAULT_NAME.vault.azure.net")
            .credential(credential)
            .retryPolicy(RetryPolicy(ExponentialBackoff(3, Duration.ofMillis(250L), Duration.ofSeconds(2))))
            .buildClient()

    override fun fetchSecretFromStore(secretName: String): String? {
        val azureSafeSecretName = secretName.lowercase().replace("_", "-")
        return secretClient.getSecret("functionapp-$azureSafeSecretName")?.let {
            return it.value
        }
    }
}