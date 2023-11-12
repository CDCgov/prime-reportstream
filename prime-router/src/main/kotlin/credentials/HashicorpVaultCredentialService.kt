package gov.cdc.prime.router.credentials

import gov.cdc.prime.router.cli.CommandUtilities
import org.apache.logging.log4j.kotlin.Logging
import org.json.JSONObject

internal object HashicorpVaultCredentialService : CredentialService(), Logging {

    private val VAULT_API_ADDR: String by lazy { System.getenv("VAULT_API_ADDR") ?: "http://127.0.0.1:8200" }
    private val VAULT_TOKEN: String by lazy { System.getenv("VAULT_TOKEN") ?: "" }

    override fun fetchCredential(connectionId: String): Credential? {
        val (_, respStr) = CommandUtilities.getWithStringResponse(
            url = "$VAULT_API_ADDR/v1/secret/$connectionId",
            hdr = mapOf("X-Vault-Token" to VAULT_TOKEN)
        )
        val credentialJson = JSONObject(respStr).getJSONObject("data").toString()
        return Credential.fromJSON(credentialJson)
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        val (response, respStr) = CommandUtilities.postWithStringResponse(
            url = "$VAULT_API_ADDR/v1/secret/$connectionId",
            hdr = mapOf("X-Vault-Token" to VAULT_TOKEN),
            jsonPayload = credential.toJSON()
        )
        if (response.status.value != 204) {
            logger.error("Status: ${response.status.value}")
            logger.error("Message: $respStr")
            throw Exception("Failed to save credentials for: $connectionId")
        }
    }
}