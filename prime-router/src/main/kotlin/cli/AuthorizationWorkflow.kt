package gov.cdc.prime.router.cli

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.sun.net.httpserver.HttpServer
import org.apache.commons.codec.binary.Base64
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Implements a PKCE OAUTH2 authorization workflow with the HHS-PRIME Okta account. A browser is launched
 * for the user to enter credentials. A local server is setup to handle the Oauth2 redirect and to capture
 * the authorization code.
 *
 * Based on Okta article https://developer.okta.com/blog/2018/12/13/oauth-2-for-native-and-mobile-apps
 */
private const val oktaBaseUrl = "https://hhs-prime.okta.com"
private const val oktaAuthorizePath = "/oauth2/default/v1/authorize" // Default authorization server
private const val oktaTokenPath = "/oauth2/default/v1/token"
private const val oktaScope = "openid"
private const val redirectPort = 9988
private const val redirectHost = "http://localhost"
private const val redirectPath = "/callback"

class AuthorizationWorkflow {
    private var redirectResult: String? = null
    private val server: HttpServer = HttpServer.create(InetSocketAddress(redirectPort), 0)
    private val clientIds = mapOf(OktaApp.DH_TEST to "0oa6fm8j4G1xfrthd4h6", OktaApp.DH_PROD to "0oa6kt4j3tOFz5SH84h6")

    // Correspond to the Data Hub's Okta app
    enum class OktaApp { DH_TEST, DH_PROD }

    fun launchSignIn(app: OktaApp): String {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateCodeVerifier() // random number
        val clientId = clientIds[app] ?: error("Invalid app")
        val authorizeUrl = authorizeUrl(clientId, state, codeChallenge = codeChallenge)
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
        return fetchAccessToken(code, codeVerifier, clientId)
    }

    private fun authorizeUrl(clientId: String, state: String, codeChallenge: String): String {
        return "$oktaBaseUrl$oktaAuthorizePath?" +
            "client_id=$clientId&" +
            "response_type=code&" +
            "scope=$oktaScope&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "state=$state&" +
            "code_challenge_method=S256&" +
            "code_challenge=$codeChallenge"
    }

    private fun startRedirectServer() {
        server.createContext(redirectPath) { transaction ->
            redirectResult = transaction.requestURI.query
            val body = "<html><body><h2>Success. You can close this tab.</h2></body></html>"
                .toByteArray()
            transaction.sendResponseHeaders(200, body.size.toLong())
            transaction.responseBody.use { it.write(body) }
        }
        server.executor = null // use the default executor
        server.start()
    }

    private fun waitForRedirect() {
        while (redirectResult == null) {
            Thread.sleep(1000 /* millis */)
        }
        server.stop(1 /* seconds */)
    }

    private fun fetchAccessToken(code: String, codeVerifier: String, clientId: String): String {
        val body = "grant_type=authorization_code&" +
            "redirect_uri=$redirectHost:$redirectPort$redirectPath&" +
            "client_id=$clientId&" +
            "code=$code&" +
            "code_verifier=$codeVerifier"
        val (_, _, result) = Fuel.post("$oktaBaseUrl$oktaTokenPath")
            .header(Headers.CONTENT_TYPE to "application/x-www-form-urlencoded")
            .body(body)
            .responseJson()
        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                result.value.obj().getString("access_token")
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