package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
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
         * The @Volatile annotation is needed to ensure that the instance property is updated atomically.
         * This prevents other threads from creating more instances and breaking the singleton pattern.
         */

        @Volatile
        private var httpClient: HttpClient? = null

        @Volatile
        private var httpClientWithAuth: HttpClient? = null

        // the httpClient object does not provide a direct means of inspecting its configuration and we need to know
        // what auth token was supplied to the existing client so we can make a determination as to whether or not to
        // return the existing object or return a new one. While the raw token is already in memory as a member of the
        // httpClientWithAuth object and the risk is very low we're using the hash for defense-in-depth purposes.
        @Volatile
        private var accessTokenHash: Int = 0

        /**
         * timeout for http calls
         */
        private const val TIMEOUT = 50_000
        const val REQUEST_TIMEOUT_MILLIS: Long = 130000 // need to be public to be used by inline
        const val SETTINGS_REQUEST_TIMEOUT_MILLIS = 30000

        /**
         * resets client and token hash to default
         */
        private fun reset() {
            httpClient = null
            httpClientWithAuth = null
            accessTokenHash = 0
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return invoke(
                HttpMethod.Get,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                httpClient = httpClient
            )
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return invoke(
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            jsonPayload: String,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return invoke(
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): T {
            return runBlocking {
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            formParams: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: getDefaultHttpClient(accessToken)).submitForm(
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType? = ContentType.Application.Json,
            timeout: Long? = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return invoke(
                method = HttpMethod.Head,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                httpClient = httpClient
            )
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
            accessToken: String? = null,
            headers: Map<String, String>? = null,
            acceptedContent: ContentType = ContentType.Application.Json,
            timeout: Long = REQUEST_TIMEOUT_MILLIS,
            queryParameters: Map<String, String>? = null,
            httpClient: HttpClient? = null,
        ): HttpResponse {
            return invoke(
                method = HttpMethod.Delete,
                url = url,
                accessToken = accessToken,
                headers = headers,
                acceptedContent = acceptedContent,
                timeout = timeout,
                queryParameters = queryParameters,
                httpClient = httpClient
            )
        }

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
        ): HttpResponse {
            return runBlocking {
                (httpClient ?: getDefaultHttpClient(accessToken)).request(url) {
                    this.method = method
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
                    acceptedContent?.let {
                        accept(acceptedContent)
                        contentType(acceptedContent)
                    }
                    jsonPayload?.let {
                        setBody(jsonPayload)
                    }
                }.also { httpClient?.close() }
            }
        }

        /**
         * Create a http client with sensible default settings
         * note: most configuration parameters are overridable
         * e.g. expectSuccess default to false because most of the time
         * the caller wants to handle the whole range of response status
         * @param bearerTokens the access token needed to call the endpoint
         * @return a HttpClient with all sensible defaults
         */
        fun getDefaultHttpClient(accessToken: String?): HttpClient {
            synchronized(this) {
                if (accessToken != null) {
                    return getDefaultHttpClientWithAuth(accessToken)
                } else {
                    httpClient ?: HttpClient(Apache) {
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
                                customizeClient {
                                }
                            }
                    }.also {
                        httpClient = it
                    }
                    return httpClient!!
                }
            }
        }

        /**
         * Called by getDefaultHttpClient as a helper to handle clients with auth tokens. Caller handles thread safety
         * where object creation and fetching is concerned by way of calling this method within a "synchronized" block.
         * This helper method ensures auth client can be reused if possible. Where not possible (ie - the provided token
         * doesn't match the hash of the auth token in the existing auth client), a new one is created and the hash of
         * the new auth token is stored. The goal is to reuse the existing auth client obj as much as possible while
         * ensuring callers are always using a client obj with the auth token they expect to be using.
         *
         * **NOTE**  Java and Kotlin both use pass-by-value with reference copy to pass arguments to a method. There is
         * therefore NO risk of one caller having an httpClientWithAuth obj change out from under them by a subsequent
         * caller who provides a different auth token. This speaks to the second thread-safety concern re: what happens
         * when a caller requests an httpClientWithAuth obj with one auth token and, before that client is able to use
         * the client obj, a second caller requests client obj with a different auth token which results in the
         * httpClientWithAuth obj in this companion class to change.
         *
         * here's why: 
         *
         * The client objects in this class are private and there is no direct reference to them outside the
         * "getter" methods which are written in a manner that ensures they are thread safe. All the places where we
         * actually use the httpClientWithAuth obj in this class are scoped to within a method call using a provided
         * reference copy passed to the method as an argument. All external callers have no access to private members
         * and thus are forced to use the objs in the same safe manner.
         *
         * In other words, in the case where two callers attempt, one immediately after the other, to create and use an
         * httpClientWithAuth object with differing auth token values, the first caller has a COPY of the stack
         * reference to the ORIGINAL object, NOT the actual reference which would point to the NEW obj once the NEW obj
         * is created. Even in the case of an immediate subsequent caller to this method that provides an auth token
         * value different than the first caller, the ORIGINAL caller has a copy of the reference to the still-in-scope
         * ORIGINAL obj in the heap, and thus, is using the ORIGINAL obj with the auth token it provided, and the
         * subsequent caller is using the NEW stack reference copy that points to the NEW client obj that contains the
         * NEW auth token it expects to be there.
         */
        private fun getDefaultHttpClientWithAuth(accessToken: String): HttpClient {
            if (accessTokenHash != accessToken.hashCode()) {
                accessTokenHash = accessToken.hashCode()
                httpClientWithAuth = HttpClient(Apache) {
                    // not using Bearer Auth handler due to refresh token behavior
                    defaultRequest {
                        header("Authorization", "Bearer $accessToken")
                    }
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
                        customizeClient {
                        }
                    }
                }
            }
            return httpClientWithAuth!!
        }
    }
}