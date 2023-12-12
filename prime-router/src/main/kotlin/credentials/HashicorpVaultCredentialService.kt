package gov.cdc.prime.router.credentials

import com.github.kittinunf.fuel.core.FuelManager
import org.apache.logging.log4j.kotlin.Logging
import org.json.JSONObject

internal object HashicorpVaultCredentialService : CredentialService(), Logging {

    private val VAULT_API_ADDR: String by lazy { System.getenv("VAULT_API_ADDR") ?: "http://127.0.0.1:8200" }
    private val VAULT_TOKEN: String by lazy { System.getenv("VAULT_TOKEN") ?: "" }
    private val manager: FuelManager by lazy { initVaultApi(FuelManager()) }

    internal fun initVaultApi(manager: FuelManager): FuelManager {
        // Set the default path
        manager.basePath = "$VAULT_API_ADDR/v1/secret"

        // Initialize the API token used to secure requests
        manager.baseHeaders = mapOf("X-Vault-Token" to VAULT_TOKEN)

        return manager
    }

    override fun fetchCredential(connectionId: String): Credential? {
        val (_, _, result) = manager.get("/$connectionId")
            .responseString()
        val (data, _) = result
        return data?.let {
            // Vault wraps the response in a data key
            val credentialJson = JSONObject(it).getJSONObject("data").toString()
            return Credential.fromJSON(credentialJson)
        }
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        val (_, response, _) = manager.post("/$connectionId")
            .header("Content-Type", "application/json")
            .body(credential.toJSON())
            .response()
        if (response.statusCode != 204) {
            logger.error("Status: ${response.statusCode}")
            logger.error("Message: ${response.responseMessage}")
            throw Exception("Failed to save credentials for: $connectionId")
        }
    }
}