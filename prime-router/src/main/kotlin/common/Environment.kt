package gov.cdc.prime.router.cli

import java.lang.IllegalArgumentException
import java.net.URL

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

    fun formUrl(path: String): URL {
        var urlString = url.toString()
        if (!urlString.endsWith("/") && !path.startsWith("/"))
            urlString += "/"
        return URL(urlString + path)
    }

    val accessToken: String get() =
        if (oktaApp == null) "dummy"
        else OktaCommand.fetchAccessToken(oktaApp)
            ?: SettingCommand.abort("Invalid access token. Run ./prime login to fetch/refresh your access token.")

    val baseUrl: String get() =
        if ((url.protocol == "http" && url.port != 80) || (url.protocol == "https" && url.port != 443))
            "${url.host}:${url.port}"
        else
            url.host

    companion object {
        fun get(environment: String): Environment {
            return valueOf(environment.uppercase())
        }

        fun get(): Environment {
            val primeEnv = System.getenv("PRIME_ENVIRONMENT") ?: ""
            return try {
                get(primeEnv)
            } catch (e: IllegalArgumentException) {
                LOCAL
            }
        }
    }
}