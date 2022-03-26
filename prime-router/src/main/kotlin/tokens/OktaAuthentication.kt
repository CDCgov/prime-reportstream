package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
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
            val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                .setIssuer("https://$issuerBaseUrl/oauth2/default")
                .build()
            // Perform authentication.  Throws exception if authentication fails.
            val jwt = jwtVerifier.decode(accessToken)

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
     * Check BOTH authentication and authorization for this [request], and if both succeed, execute [block]
     * [oktaSender] is true if we expect this call comes from a reportstream Sender named [organizationName]
     * @return a suitable HttpResponse.
     */
    fun checkAccess(
        request: HttpRequestMessage<String?>,
        organizationName: String = "",
        oktaSender: Boolean = false,
        actionHistory: ActionHistory? = null,
        block: (AuthenticatedClaims) -> HttpResponseMessage
    ): HttpResponseMessage {
        try {
            val claims = authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
            actionHistory?.trackUsername(claims.userName)

            if (!authorizeByMembership(claims, minimumLevel, organizationName, oktaSender)) {
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
     * @return true iff
     * 1) The okta group in the [claims] is at least as high as the [requiredMinimumLevel]
     * 2) The okta group in the [claims] matches the organizationName.
     * Return false otherwise
     *
     * [otkaSender] means the caller desired permission to submit payloads to ReportStream.
     * [organizationName] is the optional organization the caller desires to be associated with.
     */
    fun authorizeByMembership(
        claims: AuthenticatedClaims,
        requiredMinimumLevel: PrincipalLevel,
        organizationName: String?,
        oktaSender: Boolean = false
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        val membershipsFromOkta = claims.jwtClaims[oktaMembershipClaim] as? Collection<String> ?: return false
        // We are expecting a group name of:
        // DH<org name> if oktaSender is false
        // DHSender_<org name>.<sender name> if oktaSender is true
        // Example receiver: If the receiver org name is "ignore", the Okta group name will be "DHignore"
        // Example sender: If the sender org name is "ignore", and the sender name is "ignore-waters",
        // the Okta group name will be "DHSender_ignore.ignore-waters
        // If the sender is from Okta, their "organizationName" will match their group from Okta,
        // so do not replace anything in the string
        val groupName = if (oktaSender) organizationName else organizationName?.replace('-', '_')
        val requestedMemberships = when (requiredMinimumLevel) {
            PrincipalLevel.SYSTEM_ADMIN -> listOf(oktaSystemAdminGroup)
            PrincipalLevel.ORGANIZATION_ADMIN -> {
                listOf(
                    "$oktaGroupPrefix$groupName$oktaAdminGroupSuffix",
                    oktaSystemAdminGroup
                )
            }
            PrincipalLevel.USER ->
                listOf(
                    "${if (oktaSender) oktaSenderGroupPrefix else oktaGroupPrefix}$groupName",
                    "$oktaGroupPrefix$groupName$oktaAdminGroupSuffix",
                    oktaSystemAdminGroup
                )
        }
        requestedMemberships.forEach {
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
                " memberships $membershipsFromOkta did NOT match requested $requestedMemberships"
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