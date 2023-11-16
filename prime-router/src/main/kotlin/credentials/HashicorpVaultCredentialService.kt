package gov.cdc.prime.router.credentials

import gov.cdc.prime.router.cli.CommandUtilities
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import org.apache.logging.log4j.kotlin.Logging
import org.json.JSONObject

internal object HashicorpVaultCredentialService : CredentialService(), Logging {

    private val VAULT_API_ADDR: String by lazy { System.getenv("VAULT_API_ADDR") ?: "http://127.0.0.1:8200" }
    private val VAULT_TOKEN: String by lazy { System.getenv("VAULT_TOKEN") ?: "" }

    override fun fetchCredential(connectionId: String, httpClient: HttpClient?): Credential? {
        val (response, respStr) = CommandUtilities.getWithStringResponse(
            url = "$VAULT_API_ADDR/v1/secret/$connectionId",
            hdr = mapOf("X-Vault-Token" to VAULT_TOKEN),
            httpClient = httpClient
        )

        return if (response.status == HttpStatusCode.OK) {
            val credentialJson = JSONObject(respStr).getJSONObject("data").toString()
            Credential.fromJSON(credentialJson)
        } else {
            null
        }
    }

    override fun saveCredential(connectionId: String, credential: Credential, httpClient: HttpClient?) {
        val (response, respStr) = CommandUtilities.postWithStringResponse(
            url = "$VAULT_API_ADDR/v1/secret/$connectionId",
            hdr = mapOf("X-Vault-Token" to VAULT_TOKEN),
            jsonPayload = credential.toJSON(),
            httpClient = httpClient
        )
        if (response.status.value != 204) {
            logger.error("Status: ${response.status.value}")
            logger.error("Message: $respStr")
            throw Exception("Failed to save credentials for: $connectionId")
        }
    }
}