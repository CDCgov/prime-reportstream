package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
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

class AuthenticatedClaims(
    val jwtClaims: Map<String, Any>,
//    val principalLevel: PrincipalLevel,
//    val organizationName: String?,
) {
    // These are all derived from the raw jwtClaims.
    val userName: String
    val isPrimeAdmin: Boolean
    val organizationNameClaim: String?
    val isSenderOrgClaim: Boolean?

    init {
        userName = jwtClaims[oktaSubjectClaim]?.toString() ?: error("No username in claims")
        @Suppress("UNCHECKED_CAST")
        val memberships = jwtClaims[oktaMembershipClaim] as? Collection<String> ?: error("No memberships in claims")
        val orgNamePair = extractOrganizationNameFromOktaMembership(memberships)
        isPrimeAdmin = isPrimeAdmin(memberships)
        organizationNameClaim = orgNamePair?.first // might be null
        isSenderOrgClaim = orgNamePair?.second // might be null
    }

    /**
     * Derive useful info from jwtClaims on [memberships].
     * Return the first well-formed ReportStream organizationName found in [memberships].
     * At the same time, this determines if it's a Sender or Receiver claim.
     * Returns Pair(organizationName, true) if this organizationName claim is a Sender claim.
     * Returns Pair(organizationName, false) if this organizationName claim is a Receiver claim.
     * Returns null if no well-formed organizationName is found in [memberships]
     * Ignores the PrimeAdmins claim.
     */
    private fun extractOrganizationNameFromOktaMembership(memberships: Collection<String>): Pair<String?, Boolean>? {
        if (memberships.isEmpty()) return null
        // examples:   DHSender_ignore, DHPrimeAdmins, DHSender_all-in-one-health-ca, DHaz_phd, DHignore
        // should return, resp.:  (ignore, true), <nothing>, (all-in-one-health-ca, true, (az_phd, false, (ignore, true)
        memberships.forEach {
            if (it == oktaSystemAdminGroup) return@forEach // skip
            if (it.startsWith(oktaSenderGroupPrefix)) return Pair(it.removePrefix(oktaSenderGroupPrefix), true)
            if (it.startsWith(oktaGroupPrefix)) return Pair(it.removePrefix(oktaGroupPrefix), false)
        }
        return null
    }

    /**
     * Derive whether this user is an Admin based on claims [memberships].
     * Returns true if a well-formed Administrator claim is in [memberships].  False otherwise.
     */
    private fun isPrimeAdmin(memberships: Collection<String>): Boolean {
        memberships.forEach {
            if (it == oktaSystemAdminGroup) return true
        }
        return false
    }

    companion object {
        /**
         * Create fake claims, for testing.
         */
        fun generateTestClaims(organizationName: String? = null): AuthenticatedClaims {
            val tmpOrg = if (organizationName.isNullOrEmpty()) "ignore" else organizationName
            val jwtClaims: Map<String, Any> = mapOf(
                "organization" to listOf("${oktaSenderGroupPrefix}$tmpOrg", oktaSystemAdminGroup),
                "sub" to "local@test.com",
            )
            return AuthenticatedClaims(jwtClaims)
        }
    }
}

class OktaAuthentication(private val minimumLevel: PrincipalLevel = PrincipalLevel.USER) : Logging {
    private val issuerBaseUrl: String = System.getenv(envVariableForOktaBaseUrl) ?: ""

    companion object : Logging {

        private val authenticationFailure = HttpUtilities.errorJson("Authentication Failed")
        private val authorizationFailure = HttpUtilities.errorJson("Unauthorized")

        fun getAccessToken(request: HttpRequestMessage<String?>): String? {
            // RFC6750 defines the access token
            val authorization = request.headers[HttpHeaders.AUTHORIZATION.lowercase()] ?: return null
            return authorization.substringAfter("Bearer ", "")
        }
    }

    /**
     * Perform authentication on a human user.
     *
     * Return the claims found in the jwt if the jwt token in [request] is validated.
     * Return null if not authenticated.
     * Always performs authentication using Okta, unless running locally.
     */
    fun authenticate(request: HttpRequestMessage<String?>): AuthenticatedClaims? {
        val accessToken = getAccessToken(request)
        return authenticate(accessToken, request.httpMethod, request.uri.path)
    }

    /**
     * See full comments above.
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
        } catch (e: Exception) {
            logger.info("accessToken failed to authenticate: $httpMethod: $path", e)
            return null
        }
    }

    /**
     * Helper method for authentication.
     * Check whether we are running locally
     */
    fun isLocal(accessToken: String?): Boolean {
        return if (!Environment.isLocal()) {
            false
        } else if (accessToken != null && accessToken.split(".").size == 3) {
            // For testing auth.  Running local, but test using the real production parser.
            // The above test is purposefully simple so that we can test all kinds of error conditions
            // further downstream.
            logger.info("Running locally, but will use the OktaAuthenticationVerifier")
            false
        } else {
            true
        }
    }

    /**
     * Check BOTH authentication and authorization for this [request].
     * [oktaSender] is true if we expect this call comes from a reportstream Sender.
     * [oktaSender] is false if we expect this call comes from a reportstream receiver.
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
                logger.info("Invalid Authorization Header: ${request.httpMethod}:${request.uri.path}")
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
     * Return a valid AuthenticatedClaims iff
     * 1) The okta group in the claim is at least as high as the [requiredMinimumLevel]
     * 2) The okta group in the claim matches the organizationName.
     * Return null otherwise
     */
    fun authorizeByMembership(
        claims: AuthenticatedClaims,
        requiredMinimumLevel: PrincipalLevel,
        organizationName: String?,
        oktaSender: Boolean = false
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        val memberships = claims.jwtClaims[oktaMembershipClaim] as? Collection<String> ?: return false
        // We are expecting a group name of:
        // DH<org name> if oktaSender is false
        // DHSender_<org name>.<sender name> if oktaSender is true
        // Example receiver: If the receiver org name is "ignore", the Okta group name will be "DHignore"
        // Example sender: If the sender org name is "ignore", and the sender name is "ignore-waters",
        // the Okta group name will be "DHSender_ignore.ignore-waters
        // If the sender is from Okta, their "organizationName" will match their group from Okta,
        // so do not replace anything in the string
        val groupName = if (oktaSender) organizationName else organizationName?.replace('-', '_')
        val lookupMemberships = when (requiredMinimumLevel) {
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
        lookupMemberships.forEach {
            if (memberships.contains(it)) return true
        }
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