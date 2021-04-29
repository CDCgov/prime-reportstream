package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.secrets.SecretHelper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.jsonwebtoken.security.Keys
import org.apache.logging.log4j.kotlin.Logging
import java.lang.RuntimeException
import java.security.Key
import java.util.Date
import javax.crypto.SecretKey
import io.jsonwebtoken.io.Decoders


/* Currently not implemented as a child of AuthenticationVerifier
 * since this auth has a 'scope', not a PrincipalLevel
 */

class TokenAuthentication {
    companion object: Logging {
        private val MAX_CLOCK_SKEW_SECONDS: Long = 60

        fun checkSenderToken(jwsString: String, senderPublicKeyFinder: SigningKeyResolverAdapter): Boolean {
            return try {
                val jws = Jwts.parserBuilder()
                    .setAllowedClockSkewSeconds(MAX_CLOCK_SKEW_SECONDS)
                    .setSigningKeyResolver(senderPublicKeyFinder)
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

        fun createAccessToken(scopeAuthorized: String, lookup: ReportStreamSecretFinder): AccessToken {
            val secret = lookup.getReportStreamTokenSigningSecret()
            // Using Integer seconds to stay consistent with the JWT token spec, which uses seconds.
            // Search for 'NumericDate' in https://tools.ietf.org/html/rfc7519#section-2
            // This will break in 2038, which, actually, isn't that far off...
            val expiresAtSeconds = ((System.currentTimeMillis()/1000) + 300).toInt()
            val token = Jwts.builder()
                .setExpiration(Date(expiresAtSeconds.toLong() * 1000))  // exp
                // removed  .setId(UUID.randomUUID().toString())   // jti
                .claim("scope", scopeAuthorized)
                .signWith(secret).compact()
            return AccessToken(token, "bearer", expiresAtSeconds, scopeAuthorized)
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
            return checkAccessToken(accessToken, desiredScope, FindReportStreamSecretInVault())
        }

        /**
         * lookup is a call back to get the ReportStream secret used to sign the token.  Using a callback
         * makes this easy to test - can pass in a static test secret
         * This does not need to be a public/private key.
         */
        fun checkAccessToken(accessToken: String, desiredScope: String,
            lookup: ReportStreamSecretFinder): Claims? {
            try {
                val secret = lookup.getReportStreamTokenSigningSecret()
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
         * Prevent replay attacks
         */
        fun isNewSenderToken(jti: String?, exp: Date): Boolean {
            // todo need to check for re-use.  NOT IMPLEMENTED!
            return !isExpiredToken(exp)
        }
        fun isExpiredToken(exp: Date): Boolean {
            return (Date().after(exp))   // no need to include clock skew, since we generated token ourselves
        }


        class FindReportStreamSecretInVault: ReportStreamSecretFinder {
            private val TOKEN_SIGNING_SECRET_NAME = "TokenSigningSecret"

            override fun getReportStreamTokenSigningSecret(): SecretKey {
                val secretServiceAgent = SecretHelper.getSecretService()
                val secret = secretServiceAgent.fetchSecret(TOKEN_SIGNING_SECRET_NAME)
                    ?: error("Unable to find $TOKEN_SIGNING_SECRET_NAME")
                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
            }
        }
    }
}

data class AccessToken(
    val accessToken: String,  // access_token - required - The access token issued by the authorization server.
    val tokenType: String,    // token_type	required - Fixed value: bearer.
    val expiresAtSeconds: Int,      //  unix time IN SECONDS, of expiration time.
    val scope: String, // scope	required - Scope of access authorized. can be different from the scopes requested
)

interface ReportStreamSecretFinder {
    fun getReportStreamTokenSigningSecret(): SecretKey
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
        // todo USELESS:  NEW KEY EVERY TIME!
        return Keys.keyPairFor(SignatureAlgorithm.ES384).public
    }
}

