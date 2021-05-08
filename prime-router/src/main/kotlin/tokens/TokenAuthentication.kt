package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.annotation.JsonProperty
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.secrets.SecretHelper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.jsonwebtoken.security.Keys
import org.apache.logging.log4j.kotlin.Logging
import java.lang.RuntimeException
import java.security.Key
import java.util.Date
import javax.crypto.SecretKey
import io.jsonwebtoken.io.Decoders
import java.lang.IllegalArgumentException
import java.time.OffsetDateTime
import java.time.ZoneOffset

/* Currently not implemented as a child of AuthenticationVerifier
 * since this auth has a 'scope', not a PrincipalLevel
 */

class TokenAuthentication(val jtiCache: JtiCache): Logging {
        private val MAX_CLOCK_SKEW_SECONDS: Long = 60

    fun checkSenderToken(jwsString: String, senderPublicKeyFinder: SigningKeyResolverAdapter): Boolean {
            try {
                // Note: this does an expired token check as well
                val jws = Jwts.parserBuilder()
                    .setAllowedClockSkewSeconds(MAX_CLOCK_SKEW_SECONDS)
                    .setSigningKeyResolver(senderPublicKeyFinder)
                    .build()
                    .parseClaimsJws(jwsString)
                val jti = jws.body.id
                val exp = jws.body.expiration
                if (jti == null) {
                    logger.error("Sender Token has null JWT ID.  Rejecting.")
                    return false
                }
                val expiresAt = exp.toInstant().atOffset(ZoneOffset.UTC)
                if (expiresAt.isBefore(OffsetDateTime.now())) {
                    logger.error("Sender Token has expired, at $expiresAt.  Rejecting.")
                    return false
                }
                return isNewSenderToken(jti, expiresAt)  // check for replays
            } catch (ex: JwtException) {
                logger.error("Rejecting JWT: ${ex}")
                return false
            } catch (e: IllegalArgumentException) {
                logger.error("Rejecting JWT: ${e}")
                return false
            }
            return false
        }

        fun createAccessToken(scopeAuthorized: String, lookup: ReportStreamSecretFinder): AccessToken {
            if (scopeAuthorized.isEmpty() || scopeAuthorized.isBlank()) error("Empty or blank scope request")
            val secret = lookup.getReportStreamTokenSigningSecret()
            // Using Integer seconds to stay consistent with the JWT token spec, which uses seconds.
            // Search for 'NumericDate' in https://tools.ietf.org/html/rfc7519#section-2
            // This will break in 2038, which, actually, isn't that far off...
            val expiresInSeconds = 300
            val expiresAtSeconds = ((System.currentTimeMillis()/1000) + expiresInSeconds).toInt()
            val expirationDate = Date(expiresAtSeconds.toLong() * 1000)
            logger.info("Token for $scopeAuthorized will expire at $expirationDate")
            val token = Jwts.builder()
                .setExpiration(expirationDate)  // exp
                // removed  .setId(UUID.randomUUID().toString())   // jti
                .claim("scope", scopeAuthorized)
                .signWith(secret).compact()
            return AccessToken(token, "bearer", expiresInSeconds, expiresAtSeconds, scopeAuthorized)
        }

        fun checkAccessToken(request: HttpRequestMessage<String?>, desiredScope: String): Claims? {
            val bearerComponent = request.headers["authorization"]
            if (bearerComponent == null) {
                logger.error("Missing Authorization header.  Unauthorized.")
                return null
            }
            val accessToken = bearerComponent.split(" ")[1]
            if (accessToken.isEmpty()) {
                logger.error("Request has Authorization header but no token.  Unauthorized")
                return null
            }
            return checkAccessToken(accessToken, desiredScope, GetStaticSecret())
//            return checkAccessToken(accessToken, desiredScope, FindReportStreamSecretInVault())
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
                if (!scopeListContainsScope(scope, desiredScope)) {
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
    fun isNewSenderToken(jti: String, exp: OffsetDateTime): Boolean {
        // Do not need to account for sender's clock skew.   If sender's clock skews such that exp is
        // too soon, we set expiration to a min time in JtiCache.  If sender's clock skews the other way,
        // then that's no problem.
        return jtiCache.isJTIOk(jti, exp)
    }

    companion object: Logging {
        // Should I turn this into a nice Scope class?   Its nice to just pass a string around
        fun isWellFormedScope(scope: String): Boolean {
            val splits = scope.split(".")
            if (splits.size != 3) {
                logger.warn("Scope should be org.sender.endpoint.  Instead got: $scope ")
                return false
            }
            return true
        }

        fun isValidScope(scope: String, expectedSender: Sender): Boolean {
            if (!isWellFormedScope(scope)) return false
            val splits = scope.split(".")
            if (splits[0] != expectedSender.organizationName) {
                logger.warn("Expected organization ${expectedSender.organizationName}. Instead got: ${splits[0]}")
                return false
            }
            if (splits[1] != expectedSender.name) {
                logger.warn("Expected sender ${expectedSender.name}. Instead got: ${splits[1]}")
                return false
            }
            return when (splits[2]) {
                "report" -> true
                else -> false
            }
        }

        fun generateValidScope(sender: Sender, endpoint: String): String {
            return "${sender.fullName}.$endpoint"
        }


        fun scopeListContainsScope(scopeList: String, desiredScope: String): Boolean {
            if (desiredScope.isBlank() || desiredScope.isEmpty()) return false
            // A scope is a set of strings separated by single spaces
            val scopesTrial: List<String> = scopeList.split(" ")
            return scopesTrial.contains(desiredScope)
        }

        fun isExpiredToken(exp: Date): Boolean {
            return (Date().after(exp))   // no need to include clock skew, since we generated token ourselves
        }
    }
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

/**
 * Defined per the FHIR standard
 *    https://hl7.org/fhir/uv/bulkdata/authorization/index.html
 */
// todo get this to work:  @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
data class AccessToken(
    @JsonProperty("access_token")
    val accessToken: String,   // access_token - required - The access token issued by the authorization server.
    @JsonProperty("token_type")
    val tokenType: String,     // token_type	required - Fixed value: bearer.
    @JsonProperty("expires_in")
    val expiresIn: Int,        // seconds until expiration, eg, 300
    @JsonProperty("expires_at_seconds")
    val expiresAtSeconds: Int, //  unix time IN SECONDS, of the expiration time.
    @JsonProperty("scope")
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
 *  todo:  the FHIR spec calls for allowing a set of keys.  This callback only allows for one.
 */
class FindSenderKeyInSettings(val scope: String) : SigningKeyResolverAdapter(), Logging {
    var errorMsg: String? = null

    fun fail(shortMsg: String): Key? {
        errorMsg = "Error while requesting $scope: $shortMsg"
        logger.error(errorMsg!!)
        return null
    }

    override fun resolveSigningKey(jwsHeader: JwsHeader<*>?, claims: Claims): Key? {
        errorMsg = null
        if (jwsHeader == null) return fail("JWT has missing header")
        val issuer = claims.issuer
        val kid = jwsHeader.keyId
        val alg = jwsHeader.algorithm
        val sender = WorkflowEngine().settings.findSender(issuer) ?: return fail("No such sender fullName $issuer")
        if (sender.keys == null) return fail("No auth keys associated with sender $issuer")
        if (!TokenAuthentication.isValidScope(scope, sender)) return fail("Invalid scope for this sender: $scope")
        sender.keys.forEach { jwkSet ->
            if (jwkSet.scope == scope) {
                jwkSet.keys.forEach { jwk ->
                    if (jwk.kid == kid) {   // todo add alg test!   && jwk.alg == alg.  Or kty???
                        return jwk.toECPublicKey()  // todo implement RSA
                    }
                }
            }
        }
        return fail("Unable to find auth key for $issuer with scope=$scope, kid=$kid, and alg=$alg")
    }
}


/**
 * Return a ReportStream secret, used by ReportStream to sign a short-lived token
 * This stores a secret in static memory.  For testing only.
 */
class GetStaticSecret: ReportStreamSecretFinder {
    var tokenSigningSecret = "UVD4QOJ3H295Zi9Ayl3ySuoXNKiE8WYuOsaXOZfug3dwTUVBC1ZIKRPpG5LEyZDZ"

    override fun getReportStreamTokenSigningSecret(): SecretKey {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(tokenSigningSecret))
    }
}



