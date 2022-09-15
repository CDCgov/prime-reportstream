package gov.cdc.prime.router.tokens

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.okta.jwt.Jwt
import com.okta.jwt.JwtVerificationException
import com.okta.jwt.JwtVerifiers
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import org.apache.logging.log4j.kotlin.Logging

// These constants match how PRIME Okta subscription is configured
const val oktaGroupPrefix = "DH"
const val oktaSenderGroupPrefix = "DHSender_"
const val oktaAdminGroupSuffix = "Admins"
const val oktaSystemAdminGroup = "DHPrimeAdmins"
const val oktaMembershipClaim = "organization"
const val envVariableForOktaBaseUrl = "OKTA_baseUrl"

/**
 * Allowed roles for human interaction with ReportStream
 */
enum class PrincipalLevel {
    SYSTEM_ADMIN,
    ORGANIZATION_ADMIN,
    USER
}

class OktaAuthentication(private val minimumLevel: PrincipalLevel = PrincipalLevel.USER) : Logging {
    companion object : Logging {
        val issuerBaseUrl: String = System.getenv(envVariableForOktaBaseUrl) ?: ""

        /**
         * Perform authentication on a human user.  Confirm this [accessToken] is a valid Okta token.
         * [httpMethod] and [path] are just for logging.
         * Optional [client] is the 'client' header as passed in API POSTs submissions. Only if running local,
         * use this as the sender in the claims.  Otherwise, ignore [client].   OK if null.
         */
        fun authenticate(
            accessToken: String,
            httpMethod: HttpMethod,
            path: String,
        ): AuthenticatedClaims? {
            // Confirm the token exists.
            if (accessToken.isNullOrEmpty()) {
                logger.info("Missing or badly formatted Authorization Header: $httpMethod:$path}")
                return null
            }
            try {
                // Perform authentication.  Throws exception if authentication fails.
                val jwt = decodeJwt(accessToken)

                // Extract claims into a more usable form
                val claims = AuthenticatedClaims(jwt.claims, AuthenticationType.Okta)
                logger.info("Authenticated request by ${claims.userName}: $httpMethod:$path")
                return claims
            } catch (e: JwtVerificationException) {
                logger.warn("JWT token failed to authenticate for call: $httpMethod: $path", e)
                return null
            } catch (e: Exception) {
                logger.warn("Failure while authenticating, for call: $httpMethod: $path", e)
                return null
            }
        }

        fun decodeJwt(accessToken: String): Jwt {
            val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                .setIssuer("https://$issuerBaseUrl/oauth2/default")
                .build()
            // Perform authentication.  Throws exception if authentication fails.
            return jwtVerifier.decode(accessToken)
        }

        /**
         * This is DEPRECATED.
         *
         * @return true iff at least one of the user's [claims] exactly matches one of the required memberships.
         * The set of required memberships is calculated here based on the [requiredMinimumLevel],
         * the [requiredOrganizationName], and the [requireSenderClaim].
         *
         * If [requireSenderClaim] is true, then this user must have a claim of the form DHSender_organizationName,
         * or be an admin.
         * If [requireSenderClaim] is false,then this user must have a claim of the form DHorganizationName, or
         * DHSender_organizationName, or be an admin.
         * [requiredOrganizationName] is the optional organization the caller desires to be associated with.
         * Note:  underscores and dashes in both claims and [requiredOrganizationName] are treated as identical.
         * That is, for example, DHa_b-c is the same organization as DHa-b_c.  This is to cover for widespread
         * inconsistencies between how Okta and Settings handle "-" and "_".
         */
        fun authorizeByMembership(
            claims: AuthenticatedClaims,
            requiredMinimumLevel: PrincipalLevel,
            requiredOrganizationName: String?,
            requireSenderClaim: Boolean = false
        ): Boolean {
            @Suppress("UNCHECKED_CAST")
            val membershipsFromOkta = (claims.jwtClaims[oktaMembershipClaim] as? Collection<String> ?: return false)
                .filter { !it.isNullOrBlank() }
                .map { it.replace("-", "_") }
            // Requirement: User's claims must exactly match one of these strings to be authorized.
            val requiredMemberships = when (requiredMinimumLevel) {
                PrincipalLevel.SYSTEM_ADMIN -> listOf(oktaSystemAdminGroup)
                PrincipalLevel.ORGANIZATION_ADMIN -> {
                    listOf(
                        "$oktaGroupPrefix$requiredOrganizationName$oktaAdminGroupSuffix",
                        oktaSystemAdminGroup
                    )
                }
                PrincipalLevel.USER ->
                    listOfNotNull(
                        if (!requireSenderClaim) "$oktaGroupPrefix$requiredOrganizationName" else null,
                        "$oktaSenderGroupPrefix$requiredOrganizationName",
                        "$oktaGroupPrefix$requiredOrganizationName$oktaAdminGroupSuffix",
                        oktaSystemAdminGroup
                    )
            }.map { it.replace("-", "_") }

            requiredMemberships.forEach {
                if (membershipsFromOkta.contains(it)) {
                    logger.info(
                        "User ${claims.userName}" +
                            " memberships $membershipsFromOkta matched requested membership $it"
                    )
                    return true
                }
            }
            logger.warn(
                "User ${claims.userName}" +
                    " memberships $membershipsFromOkta did NOT match requested $requiredMemberships"
            )
            return false
        }
    }

    /**
     * This is DEPRECATED.  Please stop using this, as it only does Okta auth, and does not allow for
     * server-to-server connections.
     *
     * Check BOTH authentication and authorization for this [request], and if both succeed, execute [block].
     * When an [organizationName] is provided, this allows access at the User principal level for users with a
     * claim to that [organizationName].
     * If [requireSenderClaim] is true, then only allow access if that organization is a Sender org.
     * If [requireSenderClaim] is false, then both Sender orgs and non-Sender orgs are allowed access.
     * @return a suitable HttpResponse.
     */
    fun checkAccess(
        request: HttpRequestMessage<String?>,
        organizationName: String = "",
        requireSenderClaim: Boolean = false,
        actionHistory: ActionHistory? = null,
        block: (AuthenticatedClaims) -> HttpResponseMessage
    ): HttpResponseMessage {
        try {
            val accessToken = AuthenticatedClaims.getAccessToken(request)
            if (accessToken.isNullOrEmpty()) {
                logger.error("Missing or bad format 'Authorization: Bearer <tok>' header. Not authenticated.")
                return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
            }
            val claims = if (AuthenticatedClaims.isLocal(accessToken)) {
                logger.info("Granted test auth request for ${request.httpMethod}:${request.uri.path}")
                val client = request.headers["client"]
                val sender = if (client == null)
                    null
                else
                    WorkflowEngine().settings.findSender(client)
                AuthenticatedClaims.generateTestClaims(sender)
            } else {
                authenticate(accessToken, request.httpMethod, request.uri.path)
                    ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
            }
            actionHistory?.trackUsername(claims.userName)

            if (!authorizeByMembership(claims, minimumLevel, organizationName, requireSenderClaim)) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info("Authorized request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")
            return block(claims)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
    }
}