package gov.cdc.prime.router.cli

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.common.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URL

/**
 * Utilities for commands.
 */
class CommandUtilities {
    companion object {
        /**
         * The API endpoint to check for.  This needs to be a simple operation.
         */
        private const val waitForApiEndpointPath = "api/lookuptables/list"

        /**
         * Waits for the endpoint at [environment] to become available. This function will retry [retries] number of
         * times waiting [pollIntervalSecs] seconds between retries.
         * @throws IOException if a connection was not made
         */
        internal fun waitForApi(environment: Environment, retries: Int = 30, pollIntervalSecs: Long = 1) {
            val url = environment.formUrl(waitForApiEndpointPath)
            val accessToken = OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: error("Unable to obtain Okta access token for environment $environment")
            var retryCount = 0
            while (!isEndpointAvailable(url, accessToken)) {
                retryCount++
                if (retryCount > retries)
                    throw IOException("Unable to connect to the API at $url")
                runBlocking {
                    delay(pollIntervalSecs * 1000)
                }
            }
        }

        /**
         * Checks if the API can be connected to.
         * @return true is the API is available, false otherwise
         */
        private fun isEndpointAvailable(url: URL, accessToken: String): Boolean {
            val (_, _, result) = Fuel.head(url.toString())
                .authentication()
                .bearer(accessToken)
                .response()
            return when (result) {
                is Result.Success -> true
                else -> false
            }
        }
    }
}