package gov.cdc.prime.router.cli

import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

private const val jsonMimeType = "application/json"
private const val apiPath = "/api/settings"

/**
 * Setting Utilities class.
 * This class contains the CRUD REST Client utilitie functions.
 */

class SettingsUtilities {

    companion object {
        /**
         * Increase from the default read timeout for slow responses from the API.
         */
        val requestTimeoutMillis = 30000

        /**
         * PUT function is the CRUD utility function that handle http client CREAT and UPDATE
         * operation.
         * @return: String
         *		ERROR: 		Error on put of name of organization.
         *		SUCCESS: 	Success. Setting organization's name.
         */
        fun put(
            path: String,
            accessToken: String,
            payload: String,
        ): HttpResponse {
            val client = CommandUtilities.createDefaultHttpClient(
                BearerTokens(accessToken, refreshToken = "")
            )
            return runBlocking {
                val response =
                    client.put(path) {
                        timeout {
                            requestTimeoutMillis = requestTimeoutMillis
                        }
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                    }
                response.body()
            }
        }

        /**
         * GET function is the CRUD utility function that handle the http client GET
         * operation.
         * @return: String
         *		ERROR: 		Error getting organization's name.
         *		SUCCESS: 	JSON payload body.
         */
        fun get(
            path: String,
            accessToken: String,
        ): HttpResponse {
            val client = CommandUtilities.createDefaultHttpClient(
                BearerTokens(accessToken, refreshToken = "")
            )
            return runBlocking {
                val response =
                    client.get(path) {
                        timeout {
                            requestTimeoutMillis = requestTimeoutMillis
                        }
                        accept(ContentType.Application.Json)
                    }
                response.body()
            }
        }

        /**
         * DELETE function is the CRUD utility function that handle the http client DELETE
         * operation.
         * @return: String
         *		ERROR: 		Error on delete organization's name.
         *		SUCCESS: 	Success organization's name: JSON response body.
         */
        fun delete(
            path: String,
            accessToken: String,
        ): HttpResponse {
            val client = CommandUtilities.createDefaultHttpClient(
                BearerTokens(accessToken, refreshToken = "")
            )
            return runBlocking {
                client.delete(path) {
                    timeout {
                        requestTimeoutMillis = requestTimeoutMillis
                    }
                    accept(ContentType.Application.Json)
                }
            }
        }
    }
}