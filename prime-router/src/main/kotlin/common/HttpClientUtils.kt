package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class HttpClientUtils {
    companion object {
        /**
         * timeout for http calls
         */
        private const val TIMEOUT = 50_000
        const val REQUEST_TIMEOUT_MILLIS: Long = 130000 // need to be public to be used by inline
        const val SETTINGS_REQUEST_TIMEOUT_MILLIS = 30000

        /**
         * GET (query resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of type HttpResponse and the body in string form
         */
        fun getWithStringResponse(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = get(
                url = url,
                tokens = tokens,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
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
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of type HttpResponse
         */
        fun get(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tokens)).get(url) {
                    timeout {
                        requestTimeoutMillis = timeout
                    }
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }

                    headers?.let {
                        headers {
                            headers.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }

                    accept(acceptedContent)
                }
            }
        }

        /**
         * PUT (modify resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: null default, if present, it's the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of type HttpResponse and the body in string form
         */
        fun putWithStringResponse(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = put(
                url = url,
                tokens = tokens,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
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
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: null default, if present, it's the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of type HttpResponse
         */
        fun put(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tokens)).put(url) {
                    timeout {
                        requestTimeoutMillis = timeout
                    }
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    headers?.let {
                        headers {
                            headers.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    contentType(ContentType.Application.Json)
                    accept(acceptedContent)
                    jsonPayload?.let {
                        setBody(jsonPayload)
                    }
                }
            }
        }

        /**
         * POST (create resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: required, the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of type HttpResponse and the body in string form
         */
        fun postWithStringResponse(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = post(
                url = url,
                tokens = tokens,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
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
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param jsonPayload: required, the string representation of resource to be created
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of type HttpResponse
         */
        fun post(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tokens)).post(url) {
                    timeout {
                        requestTimeoutMillis = timeout
                    }

                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    headers?.let {
                        headers {
                            headers.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    contentType(ContentType.Application.Json)
                    accept(acceptedContent)
                    setBody(jsonPayload)
                }
            }
        }

        /**
         * Submit form to the endpoint as indicated by [url]
         *
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param httpClient: null default, a http client injected by caller
         *
         * @return object of type <T>, suppose to be deserialized from underlying response body
         */
        inline fun <reified T> submitFormT(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): T {
            return runBlocking {
                submitForm(
                    url = url,
                    tokens = tokens,
                    headers = headers,
                    acceptedContent = acceptedContent,
                    timeout = timeout,
                    formParams = formParams,
                    httpClient = httpClient,
                ).body()
            }
        }

        /**
         * Submit form to the endpoint as indicated by [url]
         *
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of HttpResponse
         */
        fun submitForm(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tokens)).submitForm(
                    url,
                    formParameters = Parameters.build {
                        formParams?.forEach { param ->
                            append(param.key, param.value)
                        }
                    }
                ) {
                    timeout {
                        requestTimeoutMillis = timeout
                    }

                    headers?.let {
                        headers {
                            headers.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }

                    accept(acceptedContent)
                }
            }
        }

        /**
         * HEAD operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         */
        fun headWithStringResponse(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = head(
                url = url,
                tokens = tokens,
                headers = headers,
                acceptContent = acceptedContent,
                timeout = timeout,
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
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         */
        fun head(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tokens)).head(url) {
                    timeout {
                        requestTimeoutMillis = timeout
                    }
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value)
                        }
                    }
                    headers?.let {
                        headers {
                            headers.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    accept(acceptContent)
                }
            }
        }

        /**
         * DELETE a resource by endpoint URL [url]
         * A thin wrapper on top of the underlying 3rd party http client, e.g. ktor http client
         * with:
         * @param url: required, the url to the resource endpoint
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         *
         */
        fun deleteWithStringResponse(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = delete(
                url = url,
                tokens = tokens,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
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
         * @param tokens: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         *
         */
        fun delete(
            url: String,
            tokens: BearerTokens? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: createDefaultHttpClient(tokens)).delete(url) {
                    timeout {
                        requestTimeoutMillis = timeout
                    }
                    url {
                        queryParameters?.forEach {
                            parameter(it.key, it.value.toString())
                        }
                    }
                    headers?.let {
                        headers {
                            headers.forEach {
                                append(it.key, it.value)
                            }
                        }
                    }
                    accept(acceptedContent)
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
                // commented out - not to override underlying default logger settings
                // enable to trace http client internals when needed
                // install(Logging) {
                //     logger = Logger.SIMPLE
                //     level = LogLevel.INFO
                // }
                bearerTokens?.let {
                    install(Auth) {
                        bearer {
                            loadTokens {
                                bearerTokens
                            }
                        }
                    }
                }
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
    }
}