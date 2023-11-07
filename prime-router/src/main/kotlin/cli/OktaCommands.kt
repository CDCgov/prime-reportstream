package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.sun.net.httpserver.HttpServer
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.transport.TokenInfo
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Base64
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
 * Implements a PKCE OAUTH2 authorization workflow with the HHS-PRIME Okta account. A browser is launched
 * for the user to enter credentials. A local server is setup to handle the Oauth2 redirect and to capture
 * the authorization code.
 *
 * Based on Okta article https://developer.okta.com/blog/2018/12/13/oauth-2-for-native-and-mobile-apps
 */
private const val oktaProdBaseUrl = "https://hhs-prime.okta.com"
const val oktaPreviewBaseUrl = "https://hhs-prime.oktapreview.com"
private const val oktaProdClientId = "0oa6kt4j3tOFz5SH84h6"
private const val oktaPreviewClientId = "0oa2fs6vp3W5MTzjh1d7"
private const val oktaAuthorizePath = "/oauth2/default/v1/authorize" // Default authorization server
private const val oktaTokenPath = "/oauth2/default/v1/token"
private const val oktaUserInfoPath = "/oauth2/default/v1/userinfo"
private const val oktaScope = "openid"
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

    override fun run() {
        val oktaApp = Environment.get(env).oktaApp ?: abort("No need to login in this environment")
        val accessTokenFile = readAccessTokenFile()
        if (accessTokenFile != null && isValidToken(oktaApp, accessTokenFile)) {
            echo("Has a valid token until ${accessTokenFile.expiresAt}")
        } else {
            echo("About to launch a browser to log in to Okta...")
            val accessTokenJson = launchSignIn(oktaApp)
            val newAccessTokenFile = writeAccessTokenFile(oktaApp, accessTokenJson)
            if (newAccessTokenFile != null) {
                echo("Login valid until ${newAccessTokenFile.expiresAt}")
            } else {
                echo("Malformed token detected: access token file not written")
            }
        }
    }

    private fun launchSignIn(app: OktaApp): TokenInfo {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateCodeVerifier() // random number
        val clientId = clientIds[app] ?: error("Invalid app")
        val oktaBaseUrl = oktaBaseUrls[app] ?: error("Invalid app - Okta url")
        val authorizeUrl = authorizeUrl(clientId, oktaBaseUrl, state, codeChallenge = codeChallenge)
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
        return fetchAccessToken(code, codeVerifier, clientId, oktaBaseUrl)
    }

    private fun authorizeUrl(clientId: String, oktaUrl: String, state: String, codeChallenge: String): String {
        return "$oktaUrl$oktaAuthorizePath?" +
            "client_id=$clientId&" +
            "response_type=code&" +
            "scope=$oktaScope&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "state=$state&" +
            "code_challenge_method=S256&" +
            "code_challenge=$codeChallenge"
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
        codeVerifier: String,
        clientId: String,
        oktaBaseUrl: String,
    ): TokenInfo {
        return runBlocking {
            val clientObj = CommandUtilities.createDefaultHttpClient(bearerTokens = null)
            clientObj.use { client ->
                val response: HttpResponse = client.submitForm(
                    "$oktaBaseUrl$oktaTokenPath",
                    formParameters = Parameters.build {
                        append("grant_type", "authorization_code")
                        append("redirect_uri", "$redirectHost:$redirectPort$redirectPath")
                        append("client_id", clientId)
                        append("code", code)
                        append("code_verifier", codeVerifier)
                    }
                ) {
                    expectSuccess = true // throw an exception if not successful
                    accept(ContentType.Application.Json)
                }
                response.body()
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
         * Dummy access token for when use with development.
         */
        private const val DUMMY_OKTA_ACCESS_TOKEN = "dummy"
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
         * timeout for http calls
         */
        private const val TIMEOUT = 50_000

        /**
         * Returns the access token saved from the last login if valid given [app].
         * @return the Okta access token, a dummy token if [app] is null. or null if there is no valid token
         */
        fun fetchAccessToken(app: OktaApp?): String? {
            return if (app == null) {
                DUMMY_OKTA_ACCESS_TOKEN
            } else {
                val accessTokenFile = readAccessTokenFile()
                if (accessTokenFile != null && isValidToken(app, accessTokenFile)) {
                    accessTokenFile.token
                } else {
                    null
                }
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

        fun isValidToken(oktaApp: OktaApp, accessTokenFile: AccessTokenFile): Boolean {
            // Make sure the token is for the right okta app
            if (clientIds[oktaApp] != accessTokenFile.clientId) return false
            // Make sure the token will not expire soon
            if (accessTokenFile.expiresAt <= LocalDateTime.now().plusMinutes(5)) return false
            val oktaBaseUrl = getOktaUrlBase(oktaApp)
            // Try out the token with Otka for the final confirmation
            val client = CommandUtilities.createDefaultHttpClient(
                BearerTokens(accessTokenFile.token, refreshToken = "")
            )
            return runBlocking {
                val response: HttpResponse = client.request("$oktaBaseUrl$oktaUserInfoPath") {
                    method = HttpMethod.Get
                    expectSuccess = true
                }
                response.status.isSuccess()
            }
        }

        fun writeAccessTokenFile(oktaApp: OktaApp, accessToken: TokenInfo): AccessTokenFile? {
            val token = accessToken.accessToken
            val expiresIn = accessToken.expiresIn?.toLong()
            val expiresAt = expiresIn?.let { LocalDateTime.now().plusSeconds(it) }
            val clientId = clientIds.getValue(oktaApp)
            val accessTokenFile = expiresAt?.let { AccessTokenFile(token, clientId, it) }

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

        private fun getOktaUrlBase(oktaApp: OktaApp): String {
            return oktaBaseUrls.getValue(oktaApp)
        }

        private fun primeFolderPath(): Path {
            return Path.of(System.getProperty("user.home"), localPrimeFolder)
        }

        private fun primeAccessFilePath(): Path {
            return Path.of(System.getProperty("user.home"), localPrimeFolder, localTokenFileName)
        }
    }
}