package gov.cdc.prime.router.credentials

import gov.cdc.prime.router.cli.CommandUtilities
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.Logging
import org.json.JSONObject

internal object HashicorpVaultCredentialService : CredentialService(), Logging {

    private val VAULT_API_ADDR: String by lazy { System.getenv("VAULT_API_ADDR") ?: "http://127.0.0.1:8200" }
    private val VAULT_TOKEN: String by lazy { System.getenv("VAULT_TOKEN") ?: "" }
//    private val manager: FuelManager by lazy { initVaultApi(FuelManager()) }

//    internal fun initVaultApi(manager: FuelManager): FuelManager {
//        // Set the default path
//        manager.basePath = "$VAULT_API_ADDR/v1/secret"
//
//        // Initialize the API token used to secure requests
//        manager.baseHeaders = mapOf("X-Vault-Token" to VAULT_TOKEN)
//
//        return manager
//    }

    override fun fetchCredential(connectionId: String): Credential? {
        return runBlocking {
            val clientObj = CommandUtilities.createDefaultHttpClient(bearerTokens = null)
            clientObj.use { client ->
                val response: HttpResponse = client.get(
                    "$VAULT_API_ADDR/v1/secret/$connectionId",
                ) {
                    headers {
                        append("X-Vault-Token", VAULT_TOKEN)
                    }
                    expectSuccess = true // throw an exception if not successful
                    accept(ContentType.Application.Json)
                }
                // Vault wraps the response in a data key
                val body: String = response.body()
                val credentialJson = JSONObject(body).getJSONObject("data").toString()
                Credential.fromJSON(credentialJson)
            }
        }
//        val (_, _, result) = manager.get("/$connectionId")
//            .responseString()
//        val (data, _) = result
//        return data?.let {
//            // Vault wraps the response in a data key
//            val credentialJson = JSONObject(it).getJSONObject("data").toString()
//            return Credential.fromJSON(credentialJson)
//        }
    }

    override fun saveCredential(connectionId: String, credential: Credential) {
        return runBlocking {
            val clientObj = CommandUtilities.createDefaultHttpClient(bearerTokens = null)
            clientObj.use { client ->
                val response: HttpResponse = client.post(
                    "$VAULT_API_ADDR/v1/secret/$connectionId",
                ) {
                    expectSuccess = true // throw an exception if not successful
                    accept(ContentType.Application.Json)
                    setBody(credential.toJSON())
                }
                if (response.status.value != 204) {
                    logger.error("Status: ${response.status.value}")
                    logger.error("Message: ${response.body<String>()}")
                    throw Exception("Failed to save credentials for: $connectionId")
                }
            }
        }
//        val (_, response, _) = manager.post("/$connectionId")
//            .header("Content-Type", "application/json")
//            .body(credential.toJSON())
//            .response()
//        if (response.statusCode != 204) {
//            logger.error("Status: ${response.statusCode}")
//            logger.error("Message: ${response.responseMessage}")
//            throw Exception("Failed to save credentials for: $connectionId")
//        }
    }
}