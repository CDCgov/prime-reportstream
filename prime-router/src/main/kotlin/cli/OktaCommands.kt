package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.sun.net.httpserver.HttpServer
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Implements OAUTH2 authorization workflows with the HHS-PRIME Okta account.
 * By default, the application checks locally for active Okta access tokens
 * If no active access tokens exist, the system attempts to get a new token from the Okta API and saves it
 *
 * A PKCE OAUTH2 authorization workflow login option is available by adding the parameter "--manual"
 * On manual mode, a browser is launched for the user to enter credentials.
 * A local server is setup to handle the Oauth2 redirect and to capture
 * the authorization code.
 *
 * Based on Okta article https://developer.okta.com/blog/2018/12/13/oauth-2-for-native-and-mobile-apps
 */

private const val oktaProdBaseUrl = "https://hhs-prime.okta.com"
private const val oktaPreviewBaseUrl = "https://hhs-prime.oktapreview.com"
private const val oktaProdClientId = "0oa6kt4j3tOFz5SH84h6"
private const val oktaPreviewClientId = "0oa2fs6vp3W5MTzjh1d7"
private const val oktaAuthorizePath = "/oauth2/default/v1/authorize" // Default authorization server
private const val oktaIntrospectPath = "/oauth2/default/v1/introspect" // For checking if a token is active
private const val oktaTokenPath = "/oauth2/default/v1/token"
private const val oktaScope = "simple_report_dev"
private const val redirectPort = 9988
private const val redirectHost = "http://localhost"
private const val redirectPath = "/callback"
private const val localPrimeFolder = ".prime"
private const val localTokenFileName = "accessToken.json"

/**
 * Logs into the Okta account and saves the access token in a hidden file
 */
class LoginCommand : OktaCommand(
    name = "login",
    help = "Login to the HHS-PRIME authorization service"
) {
    private var redirectResult: String? = null
    private var server: HttpServer? = null

    private val env by option(
        "--env", help = "Connect to <name> environment", metavar = "name", envvar = "PRIME_ENVIRONMENT"
    )
        .choice("local", "test", "staging", "prod")
        .default("local", "local")

    private val useApiKey by option(
        "--useApiKey",
        help = "Enable to sign in to Okta via an Api request instead of a login page"
    ).flag(default = false)

    private val forceRefreshToken by option(
        "--force",
        help = "Enable to do a login regardless of any current access tokens"
    ).flag(default = false)

    override fun run() {
        val oktaApp = Environment.get(env).oktaApp ?: abort("No need to login in this environment")

        // Load environment variables for the Okta Api
        // The prefix has to be added as this value is already set for other uses
        oktaBaseUrl = oktaBaseUrls[oktaApp] ?: error("Invalid app - Okta url")
        oktaClientId = clientIds[oktaApp] ?: error("Invalid app")

        oktaAuthKey = System.getenv("OKTA_authKey")
            ?: error("A valid OKTA_authKey environment variable is needed for this command")

        val accessTokenFile = if (!forceRefreshToken) {
            readAccessTokenFile()
        } else {
            null
        }

        if (accessTokenFile != null && isValidToken(accessTokenFile)) {
            echo("Has a valid token until ${accessTokenFile.expiresAt}")
        } else {
            val accessTokenJson = if (useApiKey) {
                echo("Logging in to Okta via the Client Credentials Api...")
                clientCredentialsAuthorize()
            } else {
                echo("About to launch a browser to log in to Okta...")
                launchSignIn()
            }

            val newAccessTokenFile = writeAccessTokenFile(accessTokenJson)
            echo("Login valid until ${newAccessTokenFile.expiresAt}")
        }
    }

    private fun launchSignIn(): JSONObject {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateCodeVerifier() // random number
        val authorizeUrl = authorizeUrl(state, codeChallenge = codeChallenge)
        startRedirectServer()
        Desktop.getDesktop().browse(URI(authorizeUrl))
        waitForRedirect()
        val queryParams = redirectResult ?: error("Authentication error")
        val code = queryParams
            .split("&")
            .find { it.startsWith("code=") }
            ?.substringAfter("code=")
            ?: error("Invalid authentication")
        val foundState = queryParams
            .split("&")
            .find { it.startsWith("state=") }
            ?.substringAfter("state=")
        if (foundState != state) error("Okta didn't return a valid state. Possible problem.")
        return fetchAccessToken(code, codeVerifier)
    }

    private fun authorizeUrl(state: String, codeChallenge: String): String {
        return "$oktaBaseUrl$oktaAuthorizePath?" +
            "client_id=$oktaClientId&" +
            "response_type=code&" +
            "scope=$oktaScope&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "state=$state&" +
            "code_challenge_method=S256&" +
            "code_challenge=$codeChallenge"
    }

    private fun clientCredentialsAuthorize(): JSONObject {
        val (_, _, result) = Fuel
            .post("$oktaBaseUrl$oktaTokenPath?")
            .header(
                Headers.CONTENT_TYPE to "application/x-www-form-urlencoded",
                "Authorization" to "Basic $oktaAuthKey"
            )
            .body("grant_type=client_credentials&scope=$oktaScope")
            .responseJson()

        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                result.value.obj()
            }
        }
    }

    private fun startRedirectServer() {
        val server = HttpServer.create(InetSocketAddress(redirectPort), 0)
        server.createContext(redirectPath) { transaction ->
            redirectResult = transaction.requestURI.query
            val body = "<html><body><h2>Success. You can close this tab.</h2></body></html>"
                .toByteArray()
            transaction.sendResponseHeaders(200, body.size.toLong())
            transaction.responseBody.use { it.write(body) }
        }
        server.executor = null // use the default executor
        server.start()
        this.server = server
    }

    private fun waitForRedirect() {
        val server = this.server ?: error("Server hasn't started")
        while (redirectResult == null) {
            Thread.sleep(1000 /* millis */)
        }
        server.stop(1 /* seconds */)
    }

    private fun fetchAccessToken(
        code: String,
        codeVerifier: String
    ): JSONObject {
        val body = "grant_type=authorization_code&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "client_id=$oktaClientId&" +
            "code=$code&" +
            "code_verifier=$codeVerifier"
        val (_, _, result) = Fuel.post("$oktaBaseUrl$oktaTokenPath")
            .header(Headers.CONTENT_TYPE to "application/x-www-form-urlencoded")
            .body(body)
            .responseJson()
        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                result.value.obj()
            }
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = Random.nextBytes(32)
        return Base64.encodeBase64URLSafeString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes: ByteArray = verifier.toByteArray(StandardCharsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        return Base64.encodeBase64URLSafeString(digest)
    }
}

/**
 * Logs out of the Okta account by deleting the saved access token file
 */
class LogoutCommand : OktaCommand(
    name = "logout",
    help = "Logout of the HHS-PRIME authorization service"
) {
    override fun run() {
        deleteAccessTokenFile()
        echo("Logged out")
    }
}

/**
 * Shared stuff between login and logout
 */
abstract class OktaCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
    enum class OktaApp { DH_TEST, DH_PROD, DH_STAGE, DH_DEV }

    data class AccessTokenFile(val token: String, val clientId: String, val expiresAt: LocalDateTime)

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    companion object {
        /**
         * Base endpoint for the Okta account
         */
        lateinit var oktaBaseUrl: String

        /**
         * Client ID for the Okta account
         * Required for Okta API calls to work
         */
        lateinit var oktaClientId: String

        /**
         * Api key for the Okta account
         * Required for Okta API calls to work
         */
        lateinit var oktaAuthKey: String

        /**
         * Dummy access token for when use with development.
         */
        internal const val dummyOktaAccessToken = "dummy"

        internal val clientIds = mapOf(
            OktaApp.DH_PROD to oktaProdClientId,
            OktaApp.DH_TEST to oktaPreviewClientId,
            OktaApp.DH_STAGE to oktaPreviewClientId,
            OktaApp.DH_DEV to oktaPreviewClientId,
        )
        internal val oktaBaseUrls = mapOf(
            OktaApp.DH_PROD to oktaProdBaseUrl,
            OktaApp.DH_TEST to oktaPreviewBaseUrl,
            OktaApp.DH_STAGE to oktaPreviewBaseUrl,
            OktaApp.DH_DEV to oktaPreviewBaseUrl,
        )

        private val jsonMapper = JacksonMapperUtilities.allowUnknownsMapper

        /**
         * Returns the access token saved from the last login if valid given [app].
         * @return the Okta access token, a dummy token if [app] is null. or null if there is no valid token
         */
        fun fetchAccessToken(app: OktaApp?): String? {
            return if (app == null) dummyOktaAccessToken
            else {
                val accessTokenFile = readAccessTokenFile()
                if (accessTokenFile != null && isValidToken(accessTokenFile))
                    accessTokenFile.token
                else null
            }
        }

        fun readAccessTokenFile(): AccessTokenFile? {
            val filePath = primeAccessFilePath()
            if (!Files.exists(filePath)) return null
            val file = filePath.toFile()
            return try {
                jsonMapper.readValue(file, AccessTokenFile::class.java)
            } catch (ex: Exception) {
                // Any errors will be treated like the file doesn't exist
                null
            }
        }

        fun isValidToken(accessTokenFile: AccessTokenFile): Boolean {
            // Make sure the token is for the right okta app
            if (oktaClientId != accessTokenFile.clientId) return false
            // Make sure the token will not expire soon
            if (accessTokenFile.expiresAt <= LocalDateTime.now().plusMinutes(5)) return false

            // Try out the token with Okta for the final confirmation
            val (_, _, result) = Fuel.post("$oktaBaseUrl$oktaIntrospectPath")
                .header(
                    Headers.CONTENT_TYPE to "application/x-www-form-urlencoded",
                    "Authorization" to "Basic $oktaAuthKey"
                )
                .body(
                    "token_type_hint=access_token&" +
                        "token=${accessTokenFile.token}"
                )
                .responseJson()
            return when (result) {
                is Result.Failure -> throw result.getException()
                is Result.Success -> {
                    result.value.obj().get("active") == true
                }
            }
        }

        fun writeAccessTokenFile(accessTokenJson: JSONObject): AccessTokenFile {
            val token = accessTokenJson.getString("access_token")
            val expiresIn = accessTokenJson.getLong("expires_in")
            val expiresAt = LocalDateTime.now().plusSeconds(expiresIn)
            val accessTokenFile = AccessTokenFile(token, oktaClientId, expiresAt)

            val directoryPath = primeFolderPath()
            if (Files.notExists(directoryPath)) Files.createDirectory(directoryPath)
            val file = primeAccessFilePath().toFile()
            if (file.exists()) file.delete()
            file.createNewFile()
            jsonMapper.writeValue(file, accessTokenFile)
            return accessTokenFile
        }

        fun deleteAccessTokenFile() {
            val file = primeAccessFilePath().toFile()
            if (file.exists()) {
                file.delete()
            }
        }

        private fun primeFolderPath(): Path {
            return Path.of(System.getProperty("user.home"), localPrimeFolder)
        }

        private fun primeAccessFilePath(): Path {
            return Path.of(System.getProperty("user.home"), localPrimeFolder, localTokenFileName)
        }
    }
}