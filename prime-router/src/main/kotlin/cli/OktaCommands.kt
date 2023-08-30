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
 * Implements a PKCE OAUTH2 authorization workflow with the HHS-PRIME Okta account.
 * A browser is launched for the user to enter credentials.
 * A local server is setup to handle the Oauth2 redirect and to capture
 * the authorization code.
 *
 * By adding the "--useApiKey" parameter, the login process makes a direct call to Okta's Client Credentials Api
 *
 * By default, the application checks locally for active Okta access tokens
 * If no active access tokens exist or the "--force" parameter is included,
 * the system attempts to get a new token from the Okta API and saves it
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
private const val oktaDummyAccessToken = "dummy"
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
        help = "Enable to sign in to Okta via an API key instead of a login page"
    ).flag(default = false)

    private val forceRefreshToken by option(
        "--force",
        help = "Enable to do a login regardless of any current access tokens"
    ).flag(default = false)

    /**
     * Attempt to log into Okta if the environment needs it
     */
    override fun run() {
        val oktaApp = Environment.get(env).oktaApp ?: abort("No need to login in this environment")
        val oktaConfig = OktaConfig(oktaApp)

        val accessTokenFile = if (!forceRefreshToken) {
            readAccessTokenFile()
        } else {
            null
        }

        if (accessTokenFile != null && isValidToken(oktaConfig, accessTokenFile)) {
            echo("Has a valid token until ${accessTokenFile.expiresAt}")
        } else {
            val accessTokenJson = if (useApiKey) {
                echo("Logging in to Okta via the Client Credentials Api...")
                clientCredentialsAuthorize(oktaConfig)
            } else {
                echo("About to launch a browser to log in to Okta...")
                launchSignIn(oktaConfig)
            }

            val newAccessTokenFile = writeAccessTokenFile(oktaConfig, accessTokenJson)
            echo("Login valid until ${newAccessTokenFile.expiresAt}")
        }
    }

    /**
     * Launch the Login page for Okta.
     * @param oktaConfig Configuration value wrapper
     * @return Response from Okta as a JSONObject
     */
    private fun launchSignIn(oktaConfig: OktaConfig): JSONObject {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateCodeVerifier() // random number
        val authorizeUrl = authorizeUrl(oktaConfig, state, codeChallenge = codeChallenge)
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
        return fetchSignInAccessToken(code, codeVerifier, oktaConfig)
    }

    /**
     * Builds url for Okta authorization.
     * @param oktaConfig Configuration value wrapper
     * @param state Generated code required by Okta validation
     * @param codeChallenge Used in Okta's validation
     * @return
     */
    private fun authorizeUrl(oktaConfig: OktaConfig, state: String, codeChallenge: String): String {
        return "${oktaConfig.baseUrl}$oktaAuthorizePath?" +
            "client_id=${oktaConfig.clientId}&" +
            "response_type=code&" +
            "scope=$oktaScope&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "state=$state&" +
            "code_challenge_method=S256&" +
            "code_challenge=$codeChallenge"
    }

    /**
     * Make a call to Okta's Client Credentials Api for an access token.
     * @return JSONObject of the Api response.
     */
    private fun clientCredentialsAuthorize(oktaConfig: OktaConfig): JSONObject {
        if (oktaConfig.authKey == null) error("A valid OKTA_authKey environment variable is needed for this command")
        val (_, _, result) = Fuel
            .post("${oktaConfig.baseUrl}$oktaTokenPath?")
            .header(
                Headers.CONTENT_TYPE to "application/x-www-form-urlencoded",
                "Authorization" to "Basic ${oktaConfig.authKey}"
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

    /**
     * Create and start the redirect server
     */
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

    /**
     * Waiter for when the redirect server is ready
     */
    private fun waitForRedirect() {
        val server = this.server ?: error("Server hasn't started")
        while (redirectResult == null) {
            Thread.sleep(1000 /* millis */)
        }
        server.stop(1 /* seconds */)
    }

    /**
     * Fetch access token for the sign-in process.
     * @param code Generated code required by Okta validation
     * @param codeVerifier Okta-required verifier
     * @param oktaConfig Configuration value wrapper
     * @return
     */
    private fun fetchSignInAccessToken(
        code: String,
        codeVerifier: String,
        oktaConfig: OktaConfig
    ): JSONObject {
        val body = "grant_type=authorization_code&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "client_id=${oktaConfig.clientId}&" +
            "code=$code&" +
            "code_verifier=$codeVerifier"
        val (_, _, result) = Fuel.post("${oktaConfig.baseUrl}$oktaTokenPath")
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

    /**
     * Creates code verifier as part of Okta's auth validation.
     * @return String of the generated code
     */
    private fun generateCodeVerifier(): String {
        val bytes = Random.nextBytes(32)
        return Base64.encodeBase64URLSafeString(bytes)
    }

    /**
     * Creates code challenge as part of Okta's auth validation.
     * @param verifier Code verifier used to prepare the challenge
     * @return String of the generated challenge
     */
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
        private val jsonMapper = JacksonMapperUtilities.allowUnknownsMapper

        /**
         * Returns the access token saved from the last login if valid given [app].
         * @return the Okta access token, a dummy token if [app] is null. or null if there is no valid token
         */
        fun fetchAccessToken(app: OktaApp?): String? {
            return if (app == null) oktaDummyAccessToken
            else {
                val accessTokenFile = readAccessTokenFile()
                if (accessTokenFile != null && isValidToken(OktaConfig(app), accessTokenFile))
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

        fun isValidToken(oktaConfig: OktaConfig, accessTokenFile: AccessTokenFile): Boolean {
            // Make sure the token is for the right okta app
            if (oktaConfig.clientId != accessTokenFile.clientId) return false
            // Make sure the token will not expire soon
            if (accessTokenFile.expiresAt <= LocalDateTime.now().plusMinutes(5)) return false

            // Try out the token with Okta for the final confirmation
            val (_, _, result) = Fuel.post("${oktaConfig.baseUrl}$oktaIntrospectPath")
                .header(
                    Headers.CONTENT_TYPE to "application/x-www-form-urlencoded",
                    "Authorization" to "Basic ${oktaConfig.authKey}"
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

        fun writeAccessTokenFile(oktaConfig: OktaConfig, accessTokenJson: JSONObject): AccessTokenFile {
            val token = accessTokenJson.getString("access_token")
            val expiresIn = accessTokenJson.getLong("expires_in")
            val expiresAt = LocalDateTime.now().plusSeconds(expiresIn)
            val accessTokenFile = AccessTokenFile(token, oktaConfig.clientId, expiresAt)

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

/**
 * Container for configurable values regarding Okta interactions.
 * These values are required for Okta API calls to work.
 *
 * @property baseUrl Base endpoint for the Okta account
 * @property clientId Client ID for the Okta account
 * @property authKey Api key for the Okta account
 */
data class OktaConfig(
    var baseUrl: String = oktaPreviewBaseUrl,
    var clientId: String = oktaPreviewClientId,
    var authKey: String? = null
) {
    /**
     * Load environment variables for the Okta Api
     * @param oktaApp Configuration values
     */
    constructor(
        oktaApp: OktaCommand.OktaApp
    ) : this() {
        baseUrl = oktaBaseUrls[oktaApp] ?: error("Invalid app - Okta url")
        clientId = clientIds[oktaApp] ?: error("Invalid app")
        authKey = System.getenv("OKTA_authKey")
    }

    /**
     * Container for Okta endpoints by environment
     */
    private val oktaBaseUrls = mapOf(
        OktaCommand.OktaApp.DH_PROD to oktaProdBaseUrl,
        OktaCommand.OktaApp.DH_TEST to oktaPreviewBaseUrl,
        OktaCommand.OktaApp.DH_STAGE to oktaPreviewBaseUrl,
        OktaCommand.OktaApp.DH_DEV to oktaPreviewBaseUrl,
    )

    /**
     * Container for Okta client ids by environment
     */
    private val clientIds = mapOf(
        OktaCommand.OktaApp.DH_PROD to oktaProdClientId,
        OktaCommand.OktaApp.DH_TEST to oktaPreviewClientId,
        OktaCommand.OktaApp.DH_STAGE to oktaPreviewClientId,
        OktaCommand.OktaApp.DH_DEV to oktaPreviewClientId,
    )
}