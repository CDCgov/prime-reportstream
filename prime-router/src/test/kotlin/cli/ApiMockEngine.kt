package gov.cdc.prime.router.cli

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiMockEngine(
    url: String,
    status: HttpStatusCode,
    body: String,
    val f: ((request: HttpRequestData) -> Unit)? = null,
) {
    fun get() = client.engine
    fun client() = client
    private fun validate(requestData: HttpRequestData) {
        f?.invoke(requestData)
    }

    private val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
    private val client = HttpClient(MockEngine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
        }
        engine {
            addHandler { request ->
                validate(request)
                when {
                    (request.url.encodedPath == url) ->
                        respond(body, status, responseHeaders)
                    else -> {
                        error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
        }
    }
}