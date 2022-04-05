package gov.cdc.prime.router.common

import gov.cdc.prime.router.cli.OktaCommand
import java.lang.IllegalArgumentException
import java.net.URL
import java.time.ZoneOffset

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
    STAGING("staging", URL("https://staging.prime.cdc.gov"), oktaApp = OktaCommand.OktaApp.DH_STAGE),
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
     * The baseUrl for the environment that contains only the host and port.
     */
    val baseUrl: String get() = getBaseUrl(url)

    /**
     * Available feature flags for enabling different features
     */
    enum class FeatureFlags(
        val enabledByDefault: Boolean = false
    ) {
        FHIR_ENGINE_TEST_PIPELINE();

        fun enabled(): Boolean {
            return enabledByDefault || System.getenv(this.toString()) == "enabled"
        }
    }

    companion object {
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
            return if (inputUrl.port > 0 &&
                (
                    (inputUrl.protocol == "http" && inputUrl.port != 80) ||
                        (inputUrl.protocol == "https" && inputUrl.port != 443)
                    )
            )
                "${inputUrl.host}:${inputUrl.port}"
            else
                inputUrl.host
        }

        /**
         * Checks if the current environment is the local environment.
         * @return true if local environemnt, false otherwise
         */
        fun isLocal(): Boolean {
            return get() == LOCAL
        }

        /**
         * Time zone to use for ReportStream. Note that Azure runs on UTC, so this forces our local runs to also be UTC.
         */
        val rsTimeZone: ZoneOffset = ZoneOffset.UTC
    }
}

enum class SystemExitCodes(val exitCode: Int) {
    SUCCESS(0),
    FAILURE(1)
}