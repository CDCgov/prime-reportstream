package gov.cdc.prime.router.transport

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.AUTHORIZATION
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType

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
        orgName: String,
        transportType: TransportType,
        contents: ByteArray,
        reportId: ReportId,
        retryItems: RetryItems?
    ): RetryItems? {
        val redoxTransportType = transportType as RedoxTransportType
        val (key, secret) = getKeyAndSecret(redoxTransportType)
        val messages = String(contents).split("\n") // NDJSON content
        val token = fetchToken(redoxTransportType, key, secret) ?: return RetryToken.allItems
        // DevNote: Redox access tokens live for many days
        val nextRetryItems = messages
            .mapIndexed { index, message -> Pair(index, message) }
            .filter { (index, _) ->
                retryItems == null || RetryToken.isAllItems(retryItems) || retryItems.contains(index.toString())
            }
            .mapNotNull { (index, message) ->
                if (!sendItem(redoxTransportType, token, message)) {
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

    private fun fetchToken(redox: RedoxTransportType, key: String, secret: String): String? {
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
                    null
                }
            }
            is Result.Failure -> null
            else -> null
        }
    }

    private fun sendItem(redox: RedoxTransportType, token: String, message: String): Boolean {
        val url = "${getBaseUrl(redox)}$redoxEndpointPath"
        val (_, _, result) = Fuel
            .post(url)
            .header(CONTENT_TYPE to jsonMimeType, AUTHORIZATION to "Bearer $token")
            .timeout(redoxTimeout)
            .body(message)
            .responseJson()
        // TODO: store the result id when we get line level tracking
        return when (result) {
            is Result.Success -> true
            is Result.Failure -> false
            else -> false
        }
    }
}