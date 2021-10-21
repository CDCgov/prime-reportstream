package gov.cdc.prime.router.cli

import java.lang.IllegalArgumentException
import java.net.URL

/**
 * Environment related information.
 */
enum class Environment(
    val envName: String,
    val url: URL,
    val oktaApp: OktaCommand.OktaApp? = null
) {
    LOCAL(
        "local",
        URL(
            "http://" +
                (
                    System.getenv("PRIME_RS_API_ENDPOINT_HOST")
                        ?: "localhost"
                    ) + ":7071"
        ),
    ),
    TEST("test", URL("https://test.prime.cdc.gov"), oktaApp = OktaCommand.OktaApp.DH_TEST),
    STAGING("staging", URL("https://staging.prime.cdc.gov"), oktaApp = OktaCommand.OktaApp.DH_TEST),
    PROD("prod", URL("https://prime.cdc.gov"), oktaApp = OktaCommand.OktaApp.DH_PROD);

    /**
     * Append a [path] to the URL of an environment to generate a new URL.  The starting / for [path] is optional
     * and a / will be added as needed.
     * @return a URL
     */
    fun formUrl(path: String): URL {
        var urlString = url.toString()
        if (!urlString.endsWith("/") && !path.startsWith("/"))
            urlString += "/"
        return URL(urlString + path)
    }

    /**
     * The OKTA access token.
     */
    val accessToken: String get() =
        if (oktaApp == null) dummyOktaAccessToken
        else OktaCommand.fetchAccessToken(oktaApp)
            ?: SettingCommand.abort("Invalid access token. Run ./prime login to fetch/refresh your access token.")

    /**
     * The baseUrl for the environment that contains only the host and port.
     */
    val baseUrl: String get() = getBaseUrl(url)

    companion object {
        internal const val dummyOktaAccessToken = "dummy"

        /**
         * Get the environment based on the given [environment] string.
         * @return an environment
         * @throws IllegalArgumentException if the environment cannot be found
         */
        fun get(environment: String): Environment {
            return valueOf(environment.uppercase())
        }

        /**
         * Get the environment from the system environment.
         * @return an environment or LOCAL by default
         */
        fun get(): Environment {
            val primeEnv = System.getenv("PRIME_ENVIRONMENT") ?: ""
            return try {
                get(primeEnv)
            } catch (e: IllegalArgumentException) {
                LOCAL
            }
        }

        /**
         * Get the baseUrl for a URL that contains only the host and port.
         */
        internal fun getBaseUrl(inputUrl: URL): String {
            return if ((inputUrl.protocol == "http" && inputUrl.port != 80) ||
                (inputUrl.protocol == "https" && inputUrl.port != 443)
            )
                "${inputUrl.host}:${inputUrl.port}"
            else
                inputUrl.host
        }
    }
}