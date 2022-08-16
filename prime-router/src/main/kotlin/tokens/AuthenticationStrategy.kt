package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

const val DO_OKTA_AUTH = "okta"

val authenticationFailure = HttpUtilities.errorJson("Authentication Failed")
val authorizationFailure = HttpUtilities.errorJson("Unauthorized")

/**
 * Hides the fact that we support two different authentication protocols:  server2server(two-legged) and okta.
 */
class AuthenticationStrategy : Logging {
    companion object : Logging {
        /**
         * Helper method for authentication.
         * Check whether we are running locally.
         * Even if local, if the [accessToken] is there, then do real Okta auth.
         * @return true if we should do 'local' auth, false if we should do Okta auth.
         */
        fun isLocal(accessToken: String?): Boolean {
            return when {
                (!Environment.isLocal()) -> false
                (accessToken != null && accessToken.split(".").size == 3) -> {
                    // For testing auth.  Running local, but test using the real production parser.
                    // The above test is purposefully simple so that we can test all kinds of error conditions
                    // further downstream.
                    logger.info("Running locally, but will use the OktaAuthenticationVerifier")
                    false
                }
                else -> true
            }
        }

        /**
         * Utility function to extract and @return the bearer access token from the [request] Authorization header,
         * if there is one. Otherwise return null.  Fully case insensitive.
         */
        fun getAccessToken(request: HttpRequestMessage<String?>): String? {
            // RFC6750 defines the access token
            val caseInsensitiveHeaders = request.headers.mapKeys { it.key.lowercase() }
            val authorization = caseInsensitiveHeaders[HttpHeaders.AUTHORIZATION.lowercase()] ?: return null
            val tok = authorization.replace("Bearer ", "", ignoreCase = true)
            return tok.ifBlank { null }
        }

        /**
         * Authenticate a caller (which could be a human or machine).
         * This does not perform authorization!
         *
         * This is a wrapper around two mechanisms for authentication.  Depending on the [request], this will do either
         * 1) OktaAuthentication to confirm the token is a valid okta token, or,
         * 2) two-legged (aka 'Token') authentication to confirm the token is a valid two-legged token.
         *
         * Two-legged uses the [db]
         *
         * Since the two mechanisms were developed separately, they take those slighty different arguments, so there
         * remains some cleanup work:
         * todo:  Have both okta and token auth create the same claims obj.  Currently we hack the token auth claims
         * to look like okta claims, below.
         *
         * @return a valid claims obj if authenticated.   Otherwise null if authentication failed.
         */
        fun authenticate(request: HttpRequestMessage<String?>): AuthenticatedClaims? {
            return when (request.headers["authentication-type"]) {
                DO_OKTA_AUTH -> { // Humans using Okta will send "authentication-type": "okta" in the request header
                    val oktaClaims = OktaAuthentication.authenticate(request) ?: return null
                    logger.info(
                        "Authenticated request by ${oktaClaims.userName}:" +
                            " ${request.httpMethod}:${request.uri.path}"
                    )
                    oktaClaims
                }
                else -> {
                    // In all other cases, do server2server (also called 'two-legged' or 'FHIR') auth.
                    // Authenticate the token.
                    val tokClaims = TokenAuthentication().authenticate(request) ?: return null
                    // All the rest of this is to generate an AuthenticatedClaims obj from server2server claims.
                    val claimsScope = tokClaims["scope"] as String
                    if (claimsScope.isEmpty()) {
                        logger.warn("server2server token had no scope defined.   Not authenticated")
                        return null
                    }
                    val triple = Scope.parseScope(claimsScope)
                    if (triple == null) {
                        logger.warn("Malformed scope $claimsScope - no orgName found.  Not authenticated")
                        return null
                    }
                    val orgName = triple.first
                    logger.info("Authenticated request for scope $claimsScope by subject ${tokClaims["sub"]}")
                    // todo : have the TokenAuthentication.authenticate return type AuthenticatedClaims.
                    AuthenticatedClaims(tokClaims, _organizationNameClaim = orgName)
                }
            }
        }

        /**
         * This validates claims from the /validate and /waters endpoints
         */
        fun validateClaim(
            claims: AuthenticatedClaims,
            sender: Sender,
            request: HttpRequestMessage<String?>
        ): Boolean {
            // Do authorization based on org name in claim matching org name in client header

            if ((claims.organizationNameClaim == sender.organizationName) || claims.isPrimeAdmin) {
                logger.info(
                    "Authorized request by org ${claims.organizationNameClaim}" +
                        " to submit data via client id ${sender.organizationName}.  Beginning to ingest report"
                )
                return true
            }

            logger.warn(
                "Invalid Authorization for user ${claims.userName}:" +
                    " ${request.httpMethod}:${request.uri.path}." +
                    " ERR: Claim org is ${claims.organizationNameClaim} but client id is ${sender.organizationName}"
            )
            return false
        }
    }
}