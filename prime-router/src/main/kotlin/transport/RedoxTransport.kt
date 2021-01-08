package gov.cdc.prime.router.transport

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.AUTHORIZATION
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import java.util.logging.Level

class RedoxTransport() : ITransport {
    private val secretEnvName = "REDOX_SECRET"
    private val redoxTimeout = 1000
    private val redoxBaseUrl = "https://api.redoxengine.com"
    private val redoxAuthPath = "/auth/authenticate"
    private val redoxEndpointPath = "/endpoint"
    private val redoxKey = "apiKey"
    private val redoxSecret = "secret"
    private val redoxAccessToken = "accessToken"
    private val jsonMimeType = "application/json"

    override fun send(
        orgService: OrganizationService,
        transportType: TransportType,
        contents: ByteArray,
        reportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext
    ): RetryItems? {
        val redoxTransportType = transportType as RedoxTransportType
        val (key, secret) = getKeyAndSecret(redoxTransportType)
        val messages = String(contents).split("\n") // NDJSON content
        val token = fetchToken(redoxTransportType, key, secret, context) ?: return RetryToken.allItems
        // DevNote: Redox access tokens live for many days
        val nextRetryItems = messages
            .mapIndexed { index, message -> Pair(index, message) }
            .filter { (index, _) ->
                retryItems == null || RetryToken.isAllItems(retryItems) || retryItems.contains(index.toString())
            }
            .mapNotNull { (index, message) ->
                if (!sendItem(redoxTransportType, token, message, "$reportId-$index", context)) {
                    index.toString()
                } else {
                    null
                }
            }
        return if (nextRetryItems.isNotEmpty()) nextRetryItems else null
    }

    private fun getBaseUrl(redox: RedoxTransportType): String {
        return redox.baseUrl ?: redoxBaseUrl
    }

    private fun getKeyAndSecret(redox: RedoxTransportType): Pair<String, String> {
        // Dev Note: The Redox API key doesn't change, while the secret can
        val secret = System.getenv(secretEnvName) ?: ""
        if (secret.isBlank()) error("Unable to find $secretEnvName")
        return Pair(redox.apiKey, secret)
    }

    private fun fetchToken(redox: RedoxTransportType, key: String, secret: String, context: ExecutionContext): String? {
        val url = "${getBaseUrl(redox)}$redoxAuthPath"
        val (_, _, result) = Fuel
            .post(url)
            .header(CONTENT_TYPE to jsonMimeType)
            .timeout(redoxTimeout)
            .body("""{"$redoxKey": "$key", "$redoxSecret": "$secret"}""")
            .responseJson()
        return when (result) {
            is Result.Success -> {
                if (result.value.obj().has(redoxAccessToken)) {
                    result.value.obj().getString(redoxAccessToken)
                } else {
                    context.logger.log(Level.SEVERE, "Redox Auth call succeeded but token parsing failed")
                    null
                }
            }
            is Result.Failure -> {
                context.logger.log(Level.SEVERE, "Redox Auth call failed: ${result.error.message}")
                null
            }
            else -> {
                context.logger.log(Level.SEVERE, "Redox Auth called failed")
                null
            }
        }
    }

    private fun sendItem(
        redox: RedoxTransportType,
        token: String,
        message: String,
        id: String,
        context: ExecutionContext
    ): Boolean {
        val url = "${getBaseUrl(redox)}$redoxEndpointPath"
        context.logger.log(Level.INFO, "About to post Redox msg to $url")
        val (_, _, result) = Fuel
            .post(url)
            .header(CONTENT_TYPE to jsonMimeType, AUTHORIZATION to "Bearer $token")
            .timeout(redoxTimeout)
            .body(message)
            .responseJson()
        // TODO: store the result id when we get line level tracking
        return when (result) {
            is Result.Success -> {
                context.logger.log(Level.INFO, "Successfully posted Redox msg: $id")
                true
            }
            is Result.Failure -> {
                context.logger.log(Level.WARNING, "FAILED to post Redox msg: $id")
                false
            }
            else -> false
        }
    }
}