package gov.cdc.prime.router.tokens

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.azure.DatabaseAccess
import org.apache.logging.log4j.kotlin.Logging

val USE_OKTA_AUTH = "okta"

class AuthenticationStrategy() : Logging {
    companion object : Logging {

        // Returns an OktaAuthentication strategy if the authenticationType is "okta"
        fun authStrategy(
            authenticationType: String?,
            requiredPrincipalLevel: PrincipalLevel,
            db: DatabaseAccess,
        ): Any {
            // Clients using Okta will send "authentication-type": "okta" in the request header
            if (authenticationType == USE_OKTA_AUTH) {
                return OktaAuthentication(requiredPrincipalLevel)
            }

            // default is TokenAuthentication
            return TokenAuthentication(DatabaseJtiCache(db))
        }

        /**
         * Authenticate a caller (which could be a human or machine).
         * This does not perform authorization!
         *
         * This is a wrapper around two mechanisms for authentication.  Depending on the [request], this will do either
         * 1) OktaAuthentication to confirm the token is a valid okta token, or,
         * 2) two-legged (aka 'Token') authentication to confirm the token is a valid two-legged token.
         *
         * Two-legged uses the [db] and enforces the [requiredScope].
         *
         * Since the two mechanisms were developed separately, they take those slighty different arguments, so there
         * remains some cleanup work:
         * 1) todo:  Have both okta and token auth use the same claims obj.  Below now we hack the token auth claims
         * to look like okta claims, below.
         * 2) todo: Factor out the authorization code from tok auth.  Right now it does both authn and authz.  Bleh.
         *
         * @return a valid claims obj if authenticated.   Otherwise null if authentication failed.
         */
        fun authenticate(
            request: HttpRequestMessage<String?>,
            requiredScope: String,
            db: DatabaseAccess,
        ): AuthenticatedClaims? {
            return when (request.headers["authentication-type"]) {
                USE_OKTA_AUTH -> { // Humans using Okta will send "authentication-type": "okta" in the request header
                    val oktaClaims = OktaAuthentication.authenticate(request) ?: return null
                    logger.info(
                        "Authenticated request by ${oktaClaims.userName}:" +
                            " ${request.httpMethod}:${request.uri.path}"
                    )
                    oktaClaims
                }
                else -> {
                    // In all other cases, do Server-to-server 'two-legged' auth.
                    val tokAuth = TokenAuthentication(DatabaseJtiCache(db))
                    // This will authenticate the token.
                    // todo this also checks authorization to access [requiredScope]. Mixing authz/n
                    val tokClaims = tokAuth.checkAccessToken(request, requiredScope)
                        ?: return null
                    val (orgName, _, _) = Scope.parseScope(requiredScope) ?: return null
                    logger.info("Authenticated request by ${tokClaims["sub"]} for scope $requiredScope")
                    // It is OK to allow the claim on orgName, because it was extracted from the requiredScope,
                    // which was authorized above.
                    // todo : have the TokenAuthentication.checkAccessToken return type AuthenticatedClaims.
                    AuthenticatedClaims(tokClaims, orgName)
                }
            }
        }
    }
}