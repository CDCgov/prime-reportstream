package gov.cdc.prime.router.credentials

import com.github.kittinunf.fuel.core.FuelManager
import org.apache.logging.log4j.kotlin.Logging

object HashicorpVaultCredentialService : CredentialService(), Logging {

    private val VAULT_API_ADDR: String by lazy { System.getProperty("VAULT_API_ADDR", "http://127.0.0.1:8200") }
    private val VAULT_TOKEN: String by lazy { System.getProperty("VAULT_ROOT_TOKEN", "") }
    private val manager: FuelManager by lazy { initVaultApi(FuelManager()) }

    internal fun initVaultApi(manager: FuelManager): FuelManager {
        // Set the default path
        manager.basePath = "$VAULT_API_ADDR/v1/secret"

        // Initialize the API token used to secure requests
        manager.baseHeaders = mapOf("X-Vault-Token" to VAULT_TOKEN)

        return manager
    }

    override fun fetchCredential(connectionId: String): Credential? {
        val (request, response, result) = manager.get("/$connectionId")
            .responseString()
        val (data, error) = result
        return Credential.fromJSON(data)
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        val (request, response, result) = manager.post("/$connectionId")
            .body(credential.toJSON())
            .response()
        if (response.statusCode != 200) {
            logger.error { "Failed to save credentials for: $connectionId" }
        }
    }
}