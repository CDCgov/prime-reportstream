package gov.cdc.prime.router.cli

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result

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
            payload: String
        ): ResponseResultOf<FuelJson> {
            return Fuel
                .put(path)
                .authentication()
                .bearer(accessToken)
                .header(CONTENT_TYPE to jsonMimeType)
                .jsonBody(payload)
                .timeoutRead(requestTimeoutMillis)
                .responseJson()
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
        ): Triple<Request, Response, Result<String, FuelError>> {
            return Fuel
                .get(path)
                .authentication()
                .bearer(accessToken)
                .header(CONTENT_TYPE to jsonMimeType)
                .timeoutRead(requestTimeoutMillis)
                .responseString()
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
        ): Triple<Request, Response, Result<String, FuelError>> {
            return Fuel
                .delete(path)
                .authentication()
                .bearer(accessToken)
                .header(CONTENT_TYPE to jsonMimeType)
                .timeoutRead(requestTimeoutMillis)
                .responseString()
        }
    }
}