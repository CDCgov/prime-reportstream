package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.okta.jwt.Jwt
import com.okta.jwt.JwtVerificationException
import com.okta.jwt.JwtVerifiers
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

// These constants match how PRIME Okta subscription is configured
const val oktaGroupPrefix = "DH"
const val oktaSenderGroupPrefix = "DHSender_"
const val oktaAdminGroupSuffix = "Admins"
const val oktaSystemAdminGroup = "DHPrimeAdmins"
const val oktaSubjectClaim = "sub"
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
    private val issuerBaseUrl: String = System.getenv(envVariableForOktaBaseUrl) ?: ""

    companion object : Logging {

        val authenticationFailure = HttpUtilities.errorJson("Authentication Failed")
        val authorizationFailure = HttpUtilities.errorJson("Unauthorized")

        /**
         * Extract and @return the bearer access token from the [request] Authorization header, if there is one
         * Otherwise return null.
         */
        fun getAccessToken(request: HttpRequestMessage<String?>): String? {
            // RFC6750 defines the access token
            val authorization = request.headers[HttpHeaders.AUTHORIZATION.lowercase()] ?: return null
            return authorization.substringAfter("Bearer ", "")
        }
    }

    /**
     * Perform authentication on a human user.
     *
     * @return the claims found in the jwt if the jwt token in [request] is validated.
     * Return null if not authenticated.
     * Always performs authentication using Okta, unless running locally.
     */
    fun authenticate(request: HttpRequestMessage<String?>): AuthenticatedClaims? {
        val accessToken = getAccessToken(request)
        return authenticate(accessToken, request.httpMethod, request.uri.path)
    }

    /**
     * @see [authenticate].
     */
    fun authenticate(
        accessToken: String?,
        httpMethod: HttpMethod,
        path: String,
    ): AuthenticatedClaims? {
        if (isLocal(accessToken)) {
            logger.info("Granted test auth request for $httpMethod:$path")
            return AuthenticatedClaims.generateTestClaims()
        }

        // Confirm the token exists.
        if (accessToken == null) {
            logger.info("Missing Authorization Header: $httpMethod:$path}")
            return null
        }

        try {
            // Perform authentication.  Throws exception if authentication fails.
            val jwt = decodeJwt(accessToken)

            // Extract claims into a more usable form
            val claims = AuthenticatedClaims(jwt.claims)
            logger.info("Authenticated request by ${claims.userName}: $httpMethod:$path")
            return claims
        } catch (e: JwtVerificationException) {
            logger.info("JWT token failed to authenticate for call: $httpMethod: $path", e)
            return null
        } catch (e: Exception) {
            logger.info("Failure while authenticating, for call: $httpMethod: $path", e)
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
            val claims = authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
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

    /**
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

    /**
     * For endpoints that need to check if the organization is in the database
     */
    fun checkOrganizationExists(context: ExecutionContext, userName: String, orgName: String?): Organization? {
        var organization: Organization? = null
        if (orgName != null) {
            organization = WorkflowEngine().settings.findOrganization(orgName.replace('_', '-'))
            if (organization != null) {
                return organization
            } else {
                context.logger.info("User $userName failed auth: Organization $orgName is unknown to the system.")
            }
        }
        return organization
    }
}