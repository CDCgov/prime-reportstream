package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.KeyType
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.Environment
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.apache.logging.log4j.kotlin.Logging
import java.security.Key
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

/**
 * Implementation of two-legged auth, using a sender's public key pre-authorized
 * by a trusted party at the sender.  Per this guide:
 *    https://hl7.org/fhir/uv/bulkdata/authorization/index.html
 */
class Server2ServerAuthentication(val metadata: Metadata) : Logging {
    private val MAX_CLOCK_SKEW_SECONDS: Long = 60
    private val EXPIRATION_SECONDS = 300

    class Server2ServerAuthenticationException(message: String, val scope: String) : Exception(message) {
        override fun getLocalizedMessage(): String {
            return "AccessToken Request Denied: Error while requesting $scope: $message"
        }
    }

    data class ParsedJwt(val organization: Organization, val sender: Sender?, val kid: String?, val kty: KeyType)

    /**
     *  convenience method to log in two places
     */
    private fun logErr(actionHistory: ActionHistory?, msg: String) {
        actionHistory?.trackActionResult(msg)
        logger.error(msg)
    }

    private fun parseJwt(jwsString: String, scope: String, metadata: Metadata): ParsedJwt {
        val workflowEngine = WorkflowEngine.Builder().metadata(metadata).build()

        // parseClaimsJwt will throw an exception if the string includes a signature, even when just parsing
        // the claims and headers (which are just Base64 encoded).
        // See https://github.com/jwtk/jjwt/issues/86
        val i = jwsString.lastIndexOf('.')
        val withoutSignature = jwsString.substring(0, i + 1)
        val jwt = Jwts.parserBuilder().build().parseClaimsJwt(withoutSignature)

        val claims = jwt.body
        val headers = jwt.header
        val issuer = claims.issuer // client_id
        var maybeOrganization = workflowEngine.settings.findOrganization(issuer)
        val maybeSender = workflowEngine.settings.findSender(issuer)
        if (maybeOrganization == null && maybeSender != null) {
            maybeOrganization = workflowEngine.settings.findOrganization(maybeSender.organizationName)
        }

        if (maybeOrganization == null) {
            throw Server2ServerAuthenticationException("$issuer was not valid.", scope)
        }

        val maybeKid = headers["kid"] as String?
        val alg = headers["alg"] as String
        val kty = KeyType.forAlgorithm(Algorithm.parse(alg))

        return ParsedJwt(maybeOrganization, maybeSender, maybeKid, kty)
    }

    private fun getPossibleSigningKeys(parsedJwt: ParsedJwt, scope: String): List<Key> {
        val workflowEngine = WorkflowEngine.Builder().metadata(metadata).build()
        val keys =
            (
                if (parsedJwt.sender != null)
                    workflowEngine.settings.getKeys(parsedJwt.sender.fullName)
                else parsedJwt.organization.keys
                )
                ?: emptyList()

        val applicableJwkSets = keys.filter { jwkSet -> jwkSet.scope == scope }
        return applicableJwkSets.flatMap { it.keys }
            .filter { jwk -> (jwk.kid == parsedJwt.kid && jwk.kty == parsedJwt.kty.value) }.mapNotNull { jwk ->
                when (parsedJwt.kty) {
                    KeyType.EC -> jwk.toECPublicKey()
                    KeyType.RSA -> jwk.toRSAPublicKey()
                    else -> null
                }
            }
    }

    private fun verifyJwtWithKey(
        jwsString: String,
        key: Key,
        jtiCache: JtiCache,
        actionHistory: ActionHistory? = null
    ): Boolean {
        try {
            val jws = Jwts.parserBuilder()
                .setAllowedClockSkewSeconds(MAX_CLOCK_SKEW_SECONDS)
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwsString)
            val jti = jws.body.id
            val exp = jws.body.expiration
            if (jti == null) {
                logErr(actionHistory, "SenderToken has null JWT ID.  Rejecting.")
                return false
            }
            val expiresAt = exp.toInstant().atOffset(Environment.rsTimeZone)
            if (expiresAt.isBefore(OffsetDateTime.now())) {
                logErr(actionHistory, "SenderToken $jti has expired, at $expiresAt.  Rejecting.")
                return false
            }
            return jtiCache.isJTIOk(jti, expiresAt) // check for replay attacks
        } catch (ex: JwtException) {
            // Thrown if the Jws can be parsed but is invalid because the Jws is malformed,  the signature does match,
            // it is expired, or it does not represent any claims
            return false
        } catch (e: IllegalArgumentException) {
            // Thrown if the JwsString is null or empty
            return false
        }
    }

    /**
     * @return true if jwsString is a validly signed Sender token, false if it is unauthorized
     * If it is valid, then its ok to move to the next step, then give the sender an Access token.
     */
    fun checkSenderToken(
        jwsString: String,
        scope: String,
        jtiCache: JtiCache,
        actionHistory: ActionHistory? = null,
    ): Boolean {
        return try {
            val parsedJwt = parseJwt(jwsString, scope, metadata)
            if (!Scope.isValidScope(scope, parsedJwt.organization)) {
                throw Server2ServerAuthenticationException("Invalid scope for this issuer: $scope", scope)
            }
            val possibleKeys = getPossibleSigningKeys(parsedJwt, scope)
            if (possibleKeys.isEmpty()) {
                logErr(
                    actionHistory,
                    "AccessToken Request Denied: Error while requesting $scope:" +
                        " Unable to find auth key for ${parsedJwt.organization.name} with" +
                        " scope=$scope, kid=${parsedJwt.kid}, and alg=${parsedJwt.kty}"
                )
            }
            possibleKeys.any { key -> verifyJwtWithKey(jwsString, key, jtiCache, actionHistory) }
        } catch (ex: Server2ServerAuthenticationException) {
            logErr(actionHistory, ex.localizedMessage)
            false
        } catch (ex: JwtException) {
            logErr(actionHistory, "Rejecting SenderToken JWT: $ex")
            false
        } catch (e: IllegalArgumentException) {
            logErr(actionHistory, "Rejecting SenderToken JWT: $e")
            false
        } catch (e: NullPointerException) {
            logErr(actionHistory, "Rejecting SenderToken JWT: $e")
            false
        }
    }

    fun createAccessToken(
        scopeAuthorized: String,
        lookup: ReportStreamSecretFinder,
        actionHistory: ActionHistory? = null,
    ): AccessToken {
        if (scopeAuthorized.isEmpty() || scopeAuthorized.isBlank()) error("Empty or blank scope request")
        val secret = lookup.getReportStreamTokenSigningSecret()
        // Using Integer seconds to stay consistent with the JWT token spec, which uses seconds.
        // Search for 'NumericDate' in https://tools.ietf.org/html/rfc7519#section-2
        // This will break in 2038, which, actually, isn't that far off...
        val expiresInSeconds = EXPIRATION_SECONDS
        val expiresAtSeconds = ((System.currentTimeMillis() / 1000) + expiresInSeconds).toInt()
        val expirationDate = Date(expiresAtSeconds.toLong() * 1000)
        val subject = scopeAuthorized + "_" + UUID.randomUUID()
        // Keep it small:  The signed token we send back only has two claims in it.
        val token = Jwts.builder()
            .setExpiration(expirationDate) // exp
            .claim("scope", scopeAuthorized)
            .claim("sub", subject)
            .signWith(secret).compact()
        val msg = "AccessToken $subject successfully created for $scopeAuthorized. Expires at $expirationDate"
        actionHistory?.trackActionResult(msg)
        logger.info(msg)
        return AccessToken(subject, token, "bearer", expiresInSeconds, expiresAtSeconds, scopeAuthorized)
    }

    /**
     * This confirms that [accessToken]'s token is proper and authentic. This does not do authorization,
     * that is, it does not confirm that the claims authorize access to any particular scope.
     *
     * @return the ReportStream AuthenticatedClaims obj if authentication was successful.  Otherwise returns null.
     */
    fun authenticate(accessToken: String): AuthenticatedClaims? {
        return authenticate(accessToken, FindReportStreamSecretInVault())
    }

    /**
     * This confirms that [accessToken] is properly unexpired and signed by that secret. This does not do authorization,
     * that is, it does not confirm that the claims authorize access to any particular scope.
     *
     * [lookup] is a call back to get the ReportStream secret used to sign the [accessToken].  Using a callback
     * makes this easy to test - can pass in a static test secret
     * This does not need to be a public/private key.
     *
     * @return the authenticated JWT Claims set if authentication was both successful.  Otherwise returns null.
     */
    fun authenticate(accessToken: String, lookup: ReportStreamSecretFinder): AuthenticatedClaims? {
        try {
            if (accessToken.isNullOrEmpty()) {
                logger.error("Missing or bad format 'Authorization: Bearer <tok>' header. Not authenticated.")
                return null
            }
            val secret = lookup.getReportStreamTokenSigningSecret()
            // Check the signature.  Throws JwtException on problems.
            val jws = Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(accessToken)
            if (jws.body == null) {
                logger.error("AccessToken check failed - no claims.  Not authenticated.")
                return null
            }
            val subject = jws.body["sub"] as? String
            if (subject == null) {
                logger.error("AccessToken missing subject.  Not authenticated.")
                return null
            } else {
                logger.info("checking AccessToken $subject")
            }
            if (isExpiredToken(jws.body.expiration)) {
                logger.error("AccessToken $subject expired.  Not authenticated.")
                return null
            }
            logger.info("AccessToken $subject : authenticated.")
            // convert the JWS Claims obj to our ReportStream AuthenticatedClaims obj
            return AuthenticatedClaims(jws.body, AuthenticationType.Server2Server)
        } catch (ex: JwtException) {
            logger.error("AccessToken not authenticated: $ex")
            return null
        } catch (exc: RuntimeException) {
            logger.error("AccessToken not authenticated due to internal error: $exc")
            return null
        }
    }

    companion object : Logging {
        fun isExpiredToken(exp: Date): Boolean {
            return (Date().after(exp)) // no need to include clock skew, since we generated token ourselves
        }
    }
}

/**
 * Defined per the FHIR standard
 *    https://hl7.org/fhir/uv/bulkdata/authorization/index.html
 */
// todo get this to work:  @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
data class AccessToken(
    @JsonProperty("sub")
    val sub: String, // a unique subject / id for this token, suitable for tracing
    @JsonProperty("access_token")
    val accessToken: String, // access_token - required - The access token issued by the authorization server.
    @JsonProperty("token_type")
    val tokenType: String, // token_type	required - Fixed value: bearer.
    @JsonProperty("expires_in")
    val expiresIn: Int, // seconds until expiration, eg, 300
    @JsonProperty("expires_at_seconds")
    val expiresAtSeconds: Int, //  unix time IN SECONDS, of the expiration time.
    @JsonProperty("scope")
    val scope: String, // scope	required - Scope of access authorized. can be different from the scopes requested
)