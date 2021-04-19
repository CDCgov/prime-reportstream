package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.apache.logging.log4j.kotlin.Logging
import java.lang.RuntimeException
import java.nio.file.Paths
import java.security.Key
import java.security.KeyPair
import java.util.Date
import javax.crypto.SecretKey
import io.jsonwebtoken.io.Decoders




/* Vocabulary note:
 * There are two different tokens, which we're calling the SenderToken and the AccessToken.
 * Upon validation of a SenderToken, ReportStream will return a short duration AccessToken, which can be used
 * to call one of our endpoints, assuming a valid 'scope' for that endpoint.
 *
 * The details:
 * Step 1:  The Sender signs a SenderToken using their private key.
 *          Implemented by generateSenderToken()
 * Step 2:  ReportStream checks the SenderToken using the corresponding public key it has in Settings.
 *          Implemented by checkSenderToken()
 * Step 3:  If validated, ReportStream returns a short-duration AccessToken, signed by the TokenSigningSecret
 *          Implemented by createAccessToken()
 * Step 4:  The Sender uses the AccessToken to make a request to a ReportStream endpoint.
 *
 * Step 5:  The called ReportStream endpoint checks the validity of the AccessToken signature, etc.
            Implemented by checkAccessToken()
 */
fun main(args: Array<String>) {
    println("Code is running at " + Paths.get("").toAbsolutePath().toString())
    val baseUrl = "http://localhost:7071/api/token"
    val path = "./prime-router/"
    val metadata = Metadata(path + Metadata.defaultMetadataDirectory)
    val settings = FileSettings(path + FileSettings.defaultSettingsDirectory)
    TokenAuthentication.tokenSigningSecret =  TokenAuthentication.generateTokenSigningSecret()

    // Step 1: This is the sender
    val sender = settings.findSender("ignore.ignore-waters")
    val senderToken = SenderUtils.generateSenderToken(sender!!, baseUrl, TokenAuthentication.testKey.private)
    println("The sender URL is " + SenderUtils.generateSenderUrl(baseUrl, senderToken))

    // Step 2: ReportStream gets the token and checks it.
    val accessToken = if (TokenAuthentication.checkSenderToken(senderToken, "report")) {
        // Step 3:  Report stream creates a new accessToken
        TokenAuthentication.createAccessToken("report")
    } else null

    // Step 4: Now the sender uses the accessToken to make a request

    // Step 5: And ReportStream checks it:
    val claims = TokenAuthentication.checkAccessToken(accessToken!!.accessToken, "report")
    if (claims != null) {
        println("The Sender's accessToken is valid")
    } else {
        println("UNAUTHORIZED.  The Sender's accessToken is not valid")
    }
}

// Currently not a child of AuthenticationVerifier
// since this auth has a 'scope', not a PrincipalLevel
/**
 * Confirm that a token is a valid unexpired signed JWT
 */
class TokenAuthentication {
    companion object: Logging {
        private val MAX_CLOCK_SKEW_SECONDS: Long = 60
        private val TOKEN_SIGNING_SECRET_NAME = "TokenSigningSecret"
        private val TOKEN_SIGNING_KEY_ALGORITHM = SignatureAlgorithm.HS384

        val testKey: KeyPair = Keys.keyPairFor(SignatureAlgorithm.ES384)


        fun checkSenderToken(jwsString: String, scope: String): Boolean {
            return try {
                val jws = Jwts.parserBuilder()
                    .setAllowedClockSkewSeconds(MAX_CLOCK_SKEW_SECONDS)
                    .setSigningKeyResolver(FindSenderKeyInSettings(scope))
                    .build()
                    .parseClaimsJws(jwsString)
                val jti = jws.body.id
                val exp = jws.body.expiration
                if (!isNewSenderToken(jti, exp)) error("JTI was previously used.   Auth denied.")
                true
            } catch (ex: JwtException) {
                println("Cannot accept the JWT: ${ex}")
                false
            }
        }

        fun createAccessToken(scopeAuthorized: String): AccessToken {
            val secret = lookupTokenSigningSecret()
            val expiresSeconds = 300
            val token = Jwts.builder()
                .setExpiration(Date(System.currentTimeMillis() + expiresSeconds * 1000))  // exp
                // removed  .setId(UUID.randomUUID().toString())   // jti
                .claim("scope", scopeAuthorized)
                .signWith(secret).compact()
            return AccessToken(token, "bearer", expiresSeconds, scopeAuthorized)
        }

        fun checkAccessToken(request: HttpRequestMessage<String?>, desiredScope: String): Claims? {
            val bearerComponent = request.headers["Authorization"]
            if (bearerComponent == null) {
                logger.error("Missing Authorization header.  Unauthorized.")
                return null
            }
            val accessToken = bearerComponent.split(" ")[1]
            if (accessToken.isEmpty()) {
                logger.error("Request has Authorization header but no token.  Unauthorized")
                return null
            }
            return checkAccessToken(accessToken, desiredScope)
        }

        fun checkAccessToken(accessToken: String, desiredScope: String): Claims? {
            try {
                val secret = lookupTokenSigningSecret()
                val jws = Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(accessToken)
                if (jws.body == null) {
                    logger.error("AccessToken check failed - no claims.  Unauthorized.")
                    return null
                }
                if (isExpiredToken(jws.body.expiration)) {
                    logger.error("AccessToken expired.  Unauthorized.")
                    return null
                }
                val scope = jws.body["scope"] as? String
                if (scope == null) {
                    logger.error("Missing scope claim.  Unauthorized.")
                    return null
                }
                if (!scope.contains(desiredScope)) {
                    logger.error("Sender has scope $scope, but wants $desiredScope.  Unauthorized")
                    return null
                }
                return jws.body
            } catch (ex: JwtException) {
                logger.error("Unauthorized: $ex")
                return null
            } catch (exc: RuntimeException) {
                logger.error("Unauthorized: $exc")
                return null
            }
        }

        /**
         * This is used during validation of a SenderToken.
         *
         * Implementation of a callback function used to find the public key for
         * a given Sender, kid, and alg.   Lookup in the Settings table.
         *
         */
        class FindSenderKeyInSettings(scope: String) : SigningKeyResolverAdapter() {
            override fun resolveSigningKey(jwsHeader: JwsHeader<*>?, claims: Claims): Key {
                if (jwsHeader == null) error("JWT has missing header")
                val issuer = claims.issuer
                val kid = jwsHeader.keyId
                val alg = jwsHeader.algorithm
                // this is a lookup to settings.
                //     val sender = WorkflowEngine().settings.findSender(issuer) ?: error("No such sender $issuer")
                // Need to map alg to kty
                return TokenAuthentication.testKey.public
            }
        }

        /**
         * Prevent replay attacks
         */
        fun isNewSenderToken(jti: String?, exp: Date): Boolean {
            // todo need to check for re-use.  NOT IMPLEMENTED!
            return !isExpiredToken(exp)
        }
        fun isExpiredToken(exp: Date): Boolean {
            return (Date().after(exp))   // no need to include clock skew, since we generated token ourselves
        }

        var tokenSigningSecret: String? = null
        fun lookupTokenSigningSecret(): SecretKey {
            /* val secretServiceAgent = SecretHelper.getSecretService()
            val secret = secretServiceAgent.fetchSecret(TOKEN_SIGNING_SECRET_NAME)
                ?: error("Unable to find $TOKEN_SIGNING_SECRET_NAME") */
            val secret =  tokenSigningSecret ?: error("tokenSigningSecret not set")
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
        }

        fun generateTokenSigningSecret(): String {
            return Encoders.BASE64.encode(Keys.secretKeyFor(TOKEN_SIGNING_KEY_ALGORITHM).encoded);
        }

    }

    data class AccessToken(
        val accessToken: String,  // access_token - required - The access token issued by the authorization server.
        val tokenType: String, // token_type	required - Fixed value: bearer.
        val expiresIn: Int,// expires_in - required - lifetime in seconds of the access token. The recommended value is 300
        val scope: String, // scope	required - Scope of access authorized. can be different from the scopes requested
    )


}
