package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
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

        private val httpClient: HttpClient =
            HttpClient(Apache) {
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
                engine {
                    followRedirects = true
                    socketTimeout = TIMEOUT
                    connectTimeout = TIMEOUT
                    connectionRequestTimeout = TIMEOUT
                }
            }

        /**
         * GET (query resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = get(
                url = url,
                accessToken = accessToken,
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
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse = invoke(
                HttpMethod.Get,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                httpClient = httpClient
            )

        /**
         * PUT (modify resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = put(
                url = url,
                accessToken = accessToken,
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
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse = invoke(
                method = HttpMethod.Put,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                jsonPayload = jsonPayload,
                httpClient = httpClient
            )

        /**
         * POST (create resource) operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = post(
                url = url,
                accessToken = accessToken,
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
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): HttpResponse = invoke(
                method = HttpMethod.Post,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                jsonPayload = jsonPayload,
                httpClient = httpClient
            )

        /**
         * Submit form to the endpoint as indicated by [url]
         *
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param httpClient: null default, a http client injected by caller
         *
         * @return object of type <T>, suppose to be deserialized from underlying response body
         */
        inline fun <reified T> submitFormT(
            url: String,
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): T = runBlocking {
                submitForm(
                    url = url,
                    accessToken = accessToken,
                    headers = headers,
                    acceptedContent = acceptedContent,
                    timeout = timeout,
                    formParams = formParams,
                    httpClient = httpClient,
                ).body()
            }

        /**
         * Submit form to the endpoint as indicated by [url]
         *
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the response of HttpResponse
         */
        fun submitForm(
            url: String,
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse = runBlocking {
                (httpClient ?: getDefaultHttpClient()).submitForm(
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
                    accessToken?.let {
                        headers {
                            append("Authorization", "Bearer $accessToken")
                        }
                    }
                    accept(acceptedContent)
                }
            }

        /**
         * HEAD operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType? = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = head(
                url = url,
                accessToken = accessToken,
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
         * HEAD operation to the given endpoint resource [url]
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
         * @param headers: null default, the headers of the request
         * @param acceptedContent: default application/json the accepted content type
         * @param timeout: default to a system base value in millis
         * @param queryParameters: null default, query parameters of the request
         * @param httpClient: null default, a http client injected by caller
         *
         * @return the pair of response of HttpResponse and the body in string
         */
        fun head(
            url: String,
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType? = ContentType.Application.Json,
            timeout: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse = invoke(
                method = HttpMethod.Head,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                httpClient = httpClient
            )

        /**
         * DELETE a resource by endpoint URL [url]
         * A thin wrapper on top of the underlying 3rd party http client, e.g. ktor http client
         * with:
         * @param url: required, the url to the resource endpoint
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): Pair<HttpResponse, String> {
            val response = delete(
                url = url,
                accessToken = accessToken,
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
         * @param accessToken: null default, the access token needed to call the endpoint
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse = invoke(
                method = HttpMethod.Delete,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                httpClient = httpClient
            )

        /**
         * Common helper for external func
         */
        private fun invoke(
            method: HttpMethod,
            url: String,
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType? = ContentType.Application.Json,
            timeout: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse = runBlocking {
                (httpClient ?: getDefaultHttpClient()).request(url) {
                    this.method = method
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
                    accessToken?.let {
                        headers {
                            append("Authorization", "Bearer $accessToken")
                        }
                    }
                    acceptedContent?.let {
                        accept(acceptedContent)
                        contentType(acceptedContent)
                    }
                    jsonPayload?.let {
                        setBody(jsonPayload)
                    }
                }
            }

        /**
         * Get a http client with sensible default settings
         * note: most configuration parameters are overridable
         * e.g. expectSuccess default to false because most of the time
         * the caller wants to handle the whole range of response status
         *
         * @return a HttpClient with all sensible defaults
         */
        fun getDefaultHttpClient(): HttpClient = httpClient
    }
}