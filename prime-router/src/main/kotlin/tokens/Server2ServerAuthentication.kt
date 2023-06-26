package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.KeyType
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.Environment
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import org.apache.logging.log4j.kotlin.Logging
import tokens.Server2ServerAuthenticationException
import tokens.Server2ServerError
import java.security.Key
import java.security.SignatureException
import java.util.Date
import java.util.UUID

/**
 * Implementation of two-legged auth, using a sender's public key pre-authorized
 * by a trusted party at the sender.  Per this guide:
 *    https://hl7.org/fhir/uv/bulkdata/authorization/index.html
 */
class Server2ServerAuthentication(val workflowEngine: WorkflowEngine) : Logging {
    private val MAX_CLOCK_SKEW_SECONDS: Long = 60
    private val EXPIRATION_SECONDS = 300

    /**
     * Data class that holds the data from a parsed JWT.  The organization and sender fields are derived from
     * the issuer in the JWT claims
     */
    data class ParsedJwt(val organization: Organization, val kid: String?, val kty: KeyType, val issuer: String)

    /**
     *  convenience method to log in two places
     */
    private fun logErr(actionHistory: ActionHistory?, msg: String) {
        actionHistory?.trackActionResult(msg)
        logger.error(msg)
    }

    /**
     * Parses the claims and header from a signed JWT and attempts to find the organization or sender
     * from the issuer
     *
     * @param jwsString - signed JWT to be parsed
     * @param scope - the scope that is being requested
     * @throws Server2ServerAuthenticationException - thrown if no organization or sender is founder for the issuer
     *
     */
    internal fun parseJwt(jwsString: String, scope: String,): ParsedJwt {
        // parseClaimsJwt will throw an exception if the string includes a signature, even when just parsing
        // the claims and headers (which are just Base64 encoded).
        // See https://github.com/jwtk/jjwt/issues/86
        val i = jwsString.lastIndexOf('.')
        val withoutSignature = jwsString.substring(0, i + 1)
        val jwt = Jwts.parserBuilder()
            .setAllowedClockSkewSeconds(MAX_CLOCK_SKEW_SECONDS)
            .build()
            .parseClaimsJwt(withoutSignature)

        val claims = jwt.body
        val headers = jwt.header
        val issuer = claims.issuer // client_id
        val maybeKid = headers["kid"] as String?
        val alg = headers["alg"] as String
        val kty = KeyType.forAlgorithm(Algorithm.parse(alg))

        // The issuer should always be the organization name as this most closely matches the client_id
        // description from the FHIR spec
        // http://hl7.org/fhir/uv/bulkdata/authorization/index.html#signature-verification:~:text=Upon%20registration%2C%20the%20client%20SHALL%20be%20assigned%20a%20client_id%2C%20which%20the%20client%20SHALL%20use%20when%20requesting%20an%20access%20token
        // However, in order to be backwards compatible, the issuer claim can be either the name of the sender
        // or the name of the organization.
        var maybeOrganization = workflowEngine.settings.findOrganization(issuer)
        if (maybeOrganization != null) {
            return ParsedJwt(maybeOrganization, maybeKid, kty, issuer)
        }
        val maybeSender = workflowEngine.settings.findSender(issuer)
        if (maybeSender != null) {
            maybeOrganization = workflowEngine.settings.findOrganization(maybeSender.organizationName)
        }

        if (maybeOrganization == null) {
            throw Server2ServerAuthenticationException(Server2ServerError.NO_ORG_FOUND_FOR_ISS, scope, issuer)
        }

        return ParsedJwt(maybeOrganization, maybeKid, kty, issuer)
    }

    /**
     * Uses a ParsedJwt to find all the keys for the organization that matches
     * the kid and kty
     *
     * @param parsedJwt - The parsed signed JWT
     * @param scope - the scope being requested
     */
    internal fun getPossibleSigningKeys(parsedJwt: ParsedJwt, scope: String): List<Key> {
        val keys = parsedJwt.organization.keys ?: emptyList()

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

    /**
     * Uses a resolved public key to verify the signature of a JWT
     *
     * @param jwsString - the signed JWT
     * @param key -  the public key to use to verify the signature
     * @param jtiCache -  the cache used to check for replace attacks
     * @param actionHistory -  action history to record access issues
     * @return whether or not the signature was verified
     *
     */
    internal fun verifyJwtWithKey(
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
                logErr(actionHistory, "AccessToken Request Denied: SenderToken has null JWT ID.  Rejecting.")
                return false
            }
            val expiresAt = exp.toInstant().atOffset(Environment.rsTimeZone)
            return jtiCache.isJTIOk(jti, expiresAt) // check for replay attacks
        } catch (ex: JwtException) {
            // Thrown if the Jws can be parsed but is invalid because the Jws is malformed,  the signature does match,
            // it is expired, or it does not represent any claims
            logErr(actionHistory, "AccessToken Request Denied: ${ex.localizedMessage}")
            return false
        }
    }

    /**
     * This function implements on the SMART on FHIR authentication protocol
     * http://hl7.org/fhir/uv/bulkdata/authorization/index.html#signature-verification
     *
     * @param jwsString - Base64 encoded JWS
     * @param scope - the scope that is being requested
     * @param jtiCache - the cache of used tokens to prevent replay attacks
     * @param actionHistory - action history to capture events during the auth process
     *
     * @return Unit if jwsString is a validly signed Organization token,
     * @throws Server2ServerAuthenticationException if it is the token can not be verified
     *
     * If it is valid, then it's ok to move to the next step, then give the sender an Access token.
     */
    fun checkSenderToken(
        jwsString: String,
        scope: String,
        jtiCache: JtiCache,
        actionHistory: ActionHistory? = null,
    ) {
        try {
            val parsedJwt = parseJwt(jwsString, scope)
            if (!Scope.isWellFormedScope(scope) || !Scope.isValidScope(scope, parsedJwt.organization)) {
                throw Server2ServerAuthenticationException(Server2ServerError.INVALID_SCOPE, scope, parsedJwt.issuer)
            }
            val possibleKeys = getPossibleSigningKeys(parsedJwt, scope)
            if (possibleKeys.isEmpty()) {
                logErr(
                    actionHistory,
                    "AccessToken Request Denied: Error while requesting $scope:" +
                        " Unable to find auth key for ${parsedJwt.organization.name} with" +
                        " scope=$scope, kid=${parsedJwt.kid}, and alg=${parsedJwt.kty}"
                )
                throw Server2ServerAuthenticationException(Server2ServerError.NO_VALID_KEYS, scope, parsedJwt.issuer)
            }
            if (!possibleKeys.any { key -> verifyJwtWithKey(jwsString, key, jtiCache, actionHistory) }) {
                throw Server2ServerAuthenticationException(Server2ServerError.NO_VALID_KEYS, scope, parsedJwt.issuer)
            }
        } catch (ex: Exception) {
            logErr(actionHistory, "AccessToken Request Denied: ${ex.localizedMessage}")
            when (ex) {
                is Server2ServerAuthenticationException -> throw ex
                is ExpiredJwtException -> throw Server2ServerAuthenticationException(
                    Server2ServerError.EXPIRED_TOKEN,
                    scope
                )

                is UnsupportedJwtException -> throw Server2ServerAuthenticationException(
                    Server2ServerError.UNSIGNED_JWT,
                    scope
                )

                is MalformedJwtException -> throw Server2ServerAuthenticationException(
                    Server2ServerError.MALFORMED_JWT,
                    scope
                )

                is SignatureException -> throw Server2ServerAuthenticationException(
                    Server2ServerError.NO_VALID_KEYS,
                    scope
                )

                is IllegalArgumentException -> throw Server2ServerAuthenticationException(
                    Server2ServerError.MALFORMED_JWT,
                    scope
                )

                is NullPointerException -> throw Server2ServerAuthenticationException(
                    Server2ServerError.MALFORMED_JWT,
                    scope
                )

                else -> throw ex
            }
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