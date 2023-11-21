package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import gov.cdc.prime.router.common.Environment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URL
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * Utilities for commands.
 */
class CommandUtilities {
    companion object {
        /**
         * The API endpoint to check for.  This needs to be a simple operation.
         */
        const val waitForApiEndpointPath = "api/lookuptables/list"

        /**
         * timeout for http calls
         */
        private const val TIMEOUT = 50_000
        const val REQUEST_TIMEOUT_MILLIS: Long = 130000 // need to be public to be used by inline
        const val SETTINGS_REQUEST_TIMEOUT_MILLIS = 30000

        /**
         * Waits for the endpoint at [environment] to become available. This function will retry [retries] number of
         * times waiting [pollIntervalSecs] seconds between retries.
         * @throws IOException if a connection was not made
         */
        internal fun waitForApi(
            environment: Environment,
            retries: Int = 30,
            pollIntervalSecs: Long = 1,
            httpClient: HttpClient? = null,
        ) {
            val url = environment.formUrl(waitForApiEndpointPath)
            val accessToken = OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: error("Unable to obtain Okta access token for environment $environment")
            var retryCount = 0
            while (!isEndpointAvailable(url, accessToken, httpClient = httpClient)) {
                retryCount++
                if (retryCount > retries) {
                    throw IOException("Unable to connect to the API at $url")
                }
                runBlocking {
                    delay(pollIntervalSecs * 1000)
                }
            }
        }

        /**
         * Is the service running the environment
         */
        internal fun isApiAvailable(environment: Environment, httpClient: HttpClient? = null): Boolean {
            val url = environment.formUrl(waitForApiEndpointPath)
            val accessToken = OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: error("Unable to obtain Okta access token for environment $environment")
            return isEndpointAvailable(url, accessToken, httpClient = httpClient)
        }

        /**
         * Checks if the API can be connected to.
         * @return true is the API is available, false otherwise
         */
        private fun isEndpointAvailable(url: URL, accessToken: String, httpClient: HttpClient? = null): Boolean {
            return runBlocking {
                val response = CommandUtilities.head(
                    url.toString(),
                    tkn = BearerTokens(accessToken, refreshToken = ""),
                    httpClient = httpClient
                )
                response.status == HttpStatusCode.OK
            }
        }

        private val jsonMapper = jacksonObjectMapper()

        data class DiffRow(val name: String, val baseValue: String, val toValue: String)

        /**
         * Create a list of differences between two JSON strings.
         */
        fun diffJson(base: String, compareTo: String): List<DiffRow> {
            /**
             * Given the [node] call [visitor] all descendant value nodes
             */
            fun walkTree(node: JsonNode, path: String = "", visitor: (name: String, value: String) -> Unit) {
                when {
                    node.isNull -> visitor(path, "null")
                    node.isTextual -> visitor(path, "\"${node.textValue()}\"")
                    node.isBoolean -> visitor(path, node.asText())
                    node.isNumber -> visitor(path, node.asText())
                    node.isArray -> {
                        node.iterator().asSequence().forEachIndexed { index, element ->
                            walkTree(element, "$path[$index]", visitor)
                        }
                    }

                    node.isObject -> {
                        val parentPath = if (path.isBlank()) "" else "$path."
                        node.fields().forEach { entry ->
                            walkTree(entry.value, "$parentPath${entry.key}", visitor)
                        }
                    }
                }
            }

            /**
             * Flatten the [json] structure and create a map of name-value pairs
             */
            fun createMaps(json: String): Map<String, String> {
                val tree: JsonNode = jsonMapper.readTree(json) ?: return emptyMap()
                val resultMap = mutableMapOf<String, String>()
                walkTree(tree) { name, value ->
                    resultMap[name] = value
                }
                return resultMap
            }

            /**
             * Merge the [base] and [compareTo] maps together to create a list of [DiffRow]
             */
            fun mergeMaps(base: Map<String, String>, compareTo: Map<String, String>): List<DiffRow> {
                val commonRows = base
                    .filter { (name, _) -> compareTo.containsKey(name) }
                    .map { (name, baseValue) -> DiffRow(name, baseValue, compareTo[name]!!) }
                val extraBaseRows = base
                    .filter { (name, _) -> !compareTo.containsKey(name) }
                    .map { (name, baseValue) -> DiffRow(name, baseValue, "") }
                val extraCompareToRows = compareTo
                    .filter { (name, _) -> !base.containsKey(name) }
                    .map { (name, compareToValue) -> DiffRow(name, "", compareToValue) }
                return commonRows + extraBaseRows + extraCompareToRows
            }

            val baseMap = createMaps(base)
            val compareToMap = createMaps(compareTo)
            val mergedRows = mergeMaps(baseMap, compareToMap)
            return mergedRows.filter { it.baseValue != it.toValue }.sortedBy { it.name }
        }

        /**
         * GET (query resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of type HttpResponse and the body in string form
         */
        fun getWithStringResponse(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = get(
                url = url,
                tkn = tkn,
                hdr = hdr,
                acceptedCt = acceptedCt,
                expSuccess = expSuccess,
                tmo = tmo,
                queryParameters = queryParameters,
                httpClient = httpClient
            )

            val bodyStr = runBlocking {
                response.body<String>()
            }

            return Pair(response, bodyStr)
        }

        /**
         * GET (query resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of type HttpResponse
         */
        fun get(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tkn)).get(url) {
                    timeout {
                        requestTimeoutMillis = tmo
                    }

                    expectSuccess = expSuccess

                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }

                    hdr?.let {
                        headers {
                            hdr.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }

                    accept(acceptedCt)
                }
            }
        }

        /**
         * PUT (modify resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: null default, if present, it's the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of type HttpResponse and the body in string form
         */
        fun putWithStringResponse(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = put(
                url = url,
                tkn = tkn,
                hdr = hdr,
                acceptedCt = acceptedCt,
                expSuccess = expSuccess,
                tmo = tmo,
                queryParameters = queryParameters,
                jsonPayload = jsonPayload,
                httpClient = httpClient
            )

            val respStr = runBlocking {
                response.body<String>()
            }

            return Pair(response, respStr)
        }

        /**
         * PUT (modify resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: null default, if present, it's the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of type HttpResponse
         */
        fun put(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tkn)).put(url) {
                    timeout {
                        requestTimeoutMillis = tmo
                    }
                    expectSuccess = expSuccess
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    hdr?.let {
                        headers {
                            hdr.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    contentType(ContentType.Application.Json)
                    accept(acceptedCt)
                    jsonPayload?.let {
                        setBody(jsonPayload)
                    }
                }
            }
        }

        /**
         * POST (create resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: required, the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of type HttpResponse and the body in string form
         */
        fun postWithStringResponse(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = post(
                url = url,
                tkn = tkn,
                hdr = hdr,
                acceptedCt = acceptedCt,
                expSuccess = expSuccess,
                tmo = tmo,
                queryParameters = queryParameters,
                jsonPayload = jsonPayload,
                httpClient = httpClient
            )

            val respStr = runBlocking {
                response.body<String>()
            }
            return Pair(response, respStr)
        }

        /**
         * POST (create resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: required, the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of type HttpResponse
         */
        fun post(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tkn)).post(url) {
                    timeout {
                        requestTimeoutMillis = tmo
                    }

                    expectSuccess = expSuccess

                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    hdr?.let {
                        headers {
                            hdr.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    contentType(ContentType.Application.Json)
                    accept(acceptedCt)
                    setBody(jsonPayload)
                }
            }
        }

        /**
         * Submit form to the endpoint as indicated by [url]
         *
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param httpClient: null default, a http client injected by caller
         *
         * @return object of type <T>, suppose to be deserialized from underlying response body
         */
        inline fun <reified T> submitFormT(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): T {
            return runBlocking {
                submitForm(
                    url = url,
                    tkn = tkn,
                    hdr = hdr,
                    acceptedCt = acceptedCt,
                    expSuccess = expSuccess,
                    tmo = tmo,
                    formParams = formParams,
                    httpClient = httpClient,
                ).body()
            }
        }

        /**
         * Submit form to the endpoint as indicated by [url]
         *
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of HttpResponse
         */
        fun submitForm(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tkn)).submitForm(
                    url,
                    formParameters = Parameters.build {
                        formParams?.forEach { param ->
                            append(param.key, param.value)
                        }
                    }
                ) {
                    timeout {
                        requestTimeoutMillis = tmo
                    }

                    expectSuccess = expSuccess

                    hdr?.let {
                        headers {
                            hdr.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }

                    accept(acceptedCt)
                }
            }
        }

        /**
         * HEAD operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         */
        fun headWithStringResponse(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = head(
                url = url,
                tkn = tkn,
                hdr = hdr,
                acceptedCt = acceptedCt,
                expSuccess = expSuccess,
                tmo = tmo,
                queryParameters = queryParameters,
                httpClient = httpClient
            )
            val respStr = runBlocking {
                response.body<String>()
            }
            return Pair(response, respStr)
        }

        /**
         * HEAD operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         */
        fun head(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tkn)).head(url) {
                    timeout {
                        requestTimeoutMillis = tmo
                    }
                    expectSuccess = expSuccess
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    hdr?.let {
                        headers {
                            hdr.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    accept(acceptedCt)
                }
            }
        }

        /**
         * DELETE a resource by endpoint URL [url]
         * A thin wrapper on top of the underlying 3rd party http client, e.g. ktor http client
         * with:
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         *
         */
        fun deleteWithStringResponse(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = delete(
                url = url,
                tkn = tkn,
                hdr = hdr,
                acceptedCt = acceptedCt,
                expSuccess = expSuccess,
                tmo = tmo,
                queryParameters = queryParameters,
                httpClient = httpClient
            )
            val respStr = runBlocking {
                response.body<String>()
            }
            return Pair(response, respStr)
        }

        /**
         * DELETE a resource by endpoint URL [url]
         * A thin wrapper on top of the underlying 3rd party http client, e.g. ktor http client
         * with:
         * @param url: required, the url to the resource endpoint
         * @param tkn: null default, the access token needed to call the endpoint
         * @param hdr: null default, the headers of the request
         * @param acceptedCt: default application/json the accepted content type
         * @param expSuccess: default false, if response with status >= 300 will throw error
         * @param tmo: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         *
         */
        fun delete(
            url: String,
            tkn: BearerTokens? = null,
            hdr: Map<String, String>? = null,
            acceptedCt: ContentType = ContentType.Application.Json,
            expSuccess: Boolean = false,
            tmo: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tkn)).delete(url) {
                    timeout {
                        requestTimeoutMillis = tmo
                    }
                    expectSuccess = expSuccess
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    hdr?.let {
                        headers {
                            hdr.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    accept(acceptedCt)
                }
            }
        }

        /**
         * Create a http client with sensible default settings
         * note: most configuration parameters are overridable
         * e.g. expectSuccess default to false because most of the time
         * the caller wants to handle the whole range of response status
         * @param bearerTokens null default, the access token needed to call the endpoint
         * @return a HttpClient with all sensible defaults
         */
        fun createDefaultHttpClient(bearerTokens: BearerTokens?): HttpClient {
            return HttpClient(Apache) {
                // installs logging into the call to post to the server
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.INFO
                }
                bearerTokens?.let {
                    install(Auth) {
                        bearer {
                            loadTokens {
                                bearerTokens
                            }
                        }
                    }
                }
                expectSuccess = false
                // install contentNegotiation to handle json response
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        }
                    )
                }

                install(HttpTimeout)
                // configures the Apache client with our specified timeouts
                engine {
                    followRedirects = true
                    socketTimeout = TIMEOUT
                    connectTimeout = TIMEOUT
                    connectionRequestTimeout = TIMEOUT
                    customizeClient {
                    }
                }
            }
        }

        /**
         * Nice way to abort a command
         */
        fun abort(message: String): Nothing {
            throw PrintMessage(message, error = true)
        }
    }
}