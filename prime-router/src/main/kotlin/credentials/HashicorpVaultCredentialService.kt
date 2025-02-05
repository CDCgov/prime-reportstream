package gov.cdc.prime.router.credentials

import gov.cdc.prime.router.common.HttpClientUtils
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import org.apache.logging.log4j.kotlin.Logging
import org.json.JSONObject

internal object HashicorpVaultCredentialService : CredentialService(), Logging {

    private val VAULT_API_ADDR: String by lazy { System.getenv("VAULT_API_ADDR") ?: "http://127.0.0.1:8200" }
    private val VAULT_TOKEN: String by lazy { System.getenv("VAULT_TOKEN") ?: "" }

    override fun fetchCredential(
        connectionId: String,
    ): Credential? = fetchCredentialHelper(
            connectionId = connectionId
        )

    /**
     * object specific impl - also adapted to unit tests
     */
    fun fetchCredentialHelper(
        connectionId: String,
        httpClient: HttpClient? = null,
        vaultAddr: String? = null,
        vaultToken: String? = null,
    ): Credential? {
        val (response, respStr) = HttpClientUtils.getWithStringResponse(
            url = "${vaultAddr ?: VAULT_API_ADDR}/v1/secret/$connectionId",
            headers = mapOf("X-Vault-Token" to (vaultToken ?: VAULT_TOKEN)),
            httpClient = httpClient
        )

        return if (response.status == HttpStatusCode.OK) {
            val credentialJson = JSONObject(respStr).getJSONObject("data").toString()
            Credential.fromJSON(credentialJson)
        } else {
            null
        }
    }

    override fun saveCredential(
        connectionId: String,
        credential: Credential,
    ) = saveCredentialHelper(
            connectionId = connectionId,
            credential = credential
        )

    /**
     * object specific impl - also adapted to unit tests
     */
    fun saveCredentialHelper(
        connectionId: String,
        credential: Credential,
        httpClient: HttpClient? = null,
        vaultAddr: String? = null,
        vaultToken: String? = null,
    ) {
        val (response, respStr) = HttpClientUtils.postWithStringResponse(
            url = "${vaultAddr ?: VAULT_API_ADDR}/v1/secret/$connectionId",
            headers = mapOf("X-Vault-Token" to (vaultToken ?: VAULT_TOKEN)),
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