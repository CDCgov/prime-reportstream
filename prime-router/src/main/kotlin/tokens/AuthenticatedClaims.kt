package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

const val subjectClaim = "sub"
val authenticationFailure = HttpUtilities.errorJson("Authentication Failed")
val authorizationFailure = HttpUtilities.errorJson("Unauthorized")

/**
 * Ways in which authentication/authorization are implemented in ReportStream.
 * Note that x-functions-key auth is implemented directly in Azure, and so does not need to be represented here.
 */
enum class AuthenticationType {
    Okta,
    Server2Server;
}

/**
 * This represents a set of claims coming from Okta that have been authenticated (but may or may not
 * have been authorized). For convenience, certain claims have been pulled out of the [jwtClaims]
 * into more readable/usable forms, using helper methods in the class, called during object construction.
 *
 * The companion object has a number of helper methods that hide the Okta vs Server2Server differences.
 */
class AuthenticatedClaims : Logging {
    /**
     * Raw claims map, typically as sent in a JWT
     */
    val jwtClaims: Map<String, Any>

    // All of these below are _derived_ from the raw jwtClaims.

    /** Name of user as extracted from the subject claim.  Usually an email address */
    val userName: String

    /** Does this user have the prime administrator claim? */
    val isPrimeAdmin: Boolean

    /** Set of scopes associated with these claims.   Scopes are the "common language" between
     * Okta and server2server auth.   Okta "DH" claims are mapped to scopes,
     * or are created directly from server2server claims.
     */
    val scopes: Set<String>

    /** Which way was this authentication done? eg, okta, or, server2server token */
    val authenticationType: AuthenticationType

    /**
     * [_jwtClaims]  The raw list of claims
     * [isOktaAuth] true if the claims came from Okta (eg, groups like "DHmd-phd").  false if the claims came from
     * server2server auth (eg, scopes like md-phd.default.report)
     *
     * Each value passed must have been already authenticated.  This is for claims, *not* for required authorizations.
     */
    constructor(_jwtClaims: Map<String, Any>, authenticationType: AuthenticationType) {
        this.jwtClaims = _jwtClaims
        this.userName = _jwtClaims[subjectClaim]?.toString() ?: error("No username in claims")
        this.authenticationType = authenticationType
        when (authenticationType) {
            AuthenticationType.Okta -> {
                @Suppress("UNCHECKED_CAST")
                val memberships =
                    jwtClaims[oktaMembershipClaim] as? Collection<String> ?: error("No memberships in claims")
                this.scopes = Scope.mapOktaGroupsToScopes(memberships)
            }
            AuthenticationType.Server2Server -> {
                // todo this assumes a single scope.  Need to add support for multiple scopes.
                val claimsScope = _jwtClaims["scope"] as String
                if (claimsScope.isEmpty())
                    error("For $userName, server2server token had no scope defined. Not authenticated")
                if (!Scope.isValidScope(claimsScope))
                    error("For $userName, server2server scope $claimsScope: invalid format")
                this.scopes = setOf(claimsScope)
            }
            // else -> error("$authenticationType authentication is not implemented")
        }
        this.isPrimeAdmin = scopes.contains(Scope.primeAdminScope)
    }

    /**
     * Determine if these claims authorize access to the /validate and /waters endpoints for
     * org [requiredSender] obj.  The [request] is only used for logging.
     *
     * @return true if the request to submit data on behalf of [requiredSender]
     * is authorized based on the [claims], false otherwise.
     */
    fun authorizedForSendOrReceive(
        requiredSender: Sender,
        request: HttpRequestMessage<String?>
    ): Boolean {
        return authorizedForSendOrReceive(requiredSender.organizationName, requiredSender.name, request)
    }

    /**
     * Determine if these claims authorize access to submission related resources in the
     * org [requiredOrganization] and optional sender [requiredSenderOrReceiver] string.
     * The [request] is only used for logging.
     *
     * @return true if the request to submit data on behalf of the [requiredOrganization] is
     * authorized based on the [claims], false otherwise.
     */
    fun authorizedForSendOrReceive(
        requiredOrganization: String? = null,
        requiredSenderOrReceiver: String? = null,
        request: HttpRequestMessage<String?>
    ): Boolean {
        val requiredScopes = mutableSetOf(Scope.primeAdminScope)
        // Remember that authorized(...), called below, does an exact string match, char for char, only.
        // That is, the "*" is not treated as anything special.
        if (!requiredOrganization.isNullOrBlank()) {
            requiredScopes += setOf(
                "$requiredOrganization.*.user", // eg, md-phd.*.user
                "$requiredOrganization.*.admin", // eg, md-phd.*.admin
                "$requiredOrganization.*.report", // eg, md-phd.*.report
                "$requiredOrganization.default.user", // grandfather-in senders using 'default' in server2server scope.
                "$requiredOrganization.default.admin",
                "$requiredOrganization.default.report"
            )
            if (!requiredSenderOrReceiver.isNullOrBlank()) {
                requiredScopes += setOf(
                    "$requiredOrganization.$requiredSenderOrReceiver.user", // eg, md-phd.mysender.user
                    "$requiredOrganization.$requiredSenderOrReceiver.admin", // eg, md-phd.mysender.admin
                    "$requiredOrganization.$requiredSenderOrReceiver.report" // eg, md-phd.mysender.report
                )
            }
        }
        return if (authorized(requiredScopes)) {
            logger.info(
                "Authorized request by user with claims ${this.scopes}" +
                    " for org-related resources. client= $requiredOrganization."
            )
            true
        } else {
            logger.warn(
                "Invalid Authorization for user ${this.userName}: ${request.httpMethod}:${request.uri.path}." +
                    " ERR: Claims from user are ${this.scopes} but required scopes are $requiredScopes"
            )
            false
        }
    }

    /**
     * @return true if these claims authorize access to the [requiredScopes].  False if unauthorized.
     */
    fun authorized(requiredScopes: Set<String>): Boolean {
        return Scope.authorized(this.scopes, requiredScopes)
    }

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
                    logger.info("Running locally, but will do authentication")
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
         * @return a valid AuthenticatedClaims obj if authenticated.   Otherwise null if authentication failed.
         */
        fun authenticate(request: HttpRequestMessage<String?>): AuthenticatedClaims? {
            val accessToken = getAccessToken(request)
            if (isLocal(accessToken)) {
                logger.info("Granted test auth request for ${request.httpMethod}:${request.uri.path}")
                val client = request.headers["client"]
                val sender = if (client == null)
                    null
                else
                    WorkflowEngine().settings.findSender(client)
                return AuthenticatedClaims.generateTestClaims(sender)
            }

            if (accessToken.isNullOrEmpty()) {
                logger.error("Missing or bad format 'Authorization: Bearer <tok>' header. Not authenticated.")
                return null
            }

            // First try Okta, then try Server2Server, then give up.
            logger.info("Attempting Okta auth for request to ${request.uri}")
            var authenticatedClaims = OktaAuthentication.authenticate(accessToken, request.httpMethod, request.uri.path)
            if (authenticatedClaims == null) {
                logger.info("Okta: Unauthorized.  Now trying server2server auth for request to ${request.uri}.")
                authenticatedClaims = Server2ServerAuthentication(WorkflowEngine()).authenticate(accessToken)
                if (authenticatedClaims == null) {
                    logger.info("Server2Server: Also Unauthorized, for request to ${request.uri}. Giving up.")
                    return null
                }
            }
            logger.info(
                "Authenticated request by ${authenticatedClaims.userName}: " +
                    "(${request.httpMethod}:${request.uri.path})" +
                    " using ${authenticatedClaims.authenticationType.name} auth."
            )
            return authenticatedClaims
        }

        /**
         * Run authentication then filters out non-admin claims
         * @param request Http request to authenticate
         * @return Authenticated claims if primeadmin is authorized, else null
         */
        fun authenticateAdmin(
            request: HttpRequestMessage<String?>
        ): AuthenticatedClaims? {
            val claims = authenticate(request)
            return if (claims == null || !claims.authorized(setOf("*.*.primeadmin"))) {
                logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
                null
            } else {
                claims
            }
        }
        /**
         * Create fake AuthenticatedClaims, for testing
         * @return fake claims, for testing.
         * Uses the organizationName in the [sender] if one is passed in, otherwise uses the `ignore` org.
         */
        fun generateTestClaims(sender: Sender? = null): AuthenticatedClaims {
            val tmpOrg = sender?.organizationName ?: "ignore"
            val jwtClaims: Map<String, Any> = mapOf(
                oktaMembershipClaim to listOf("$oktaSenderGroupPrefix$tmpOrg", oktaSystemAdminGroup),
                "sub" to "local@test.com",
            )
            return AuthenticatedClaims(jwtClaims, AuthenticationType.Okta)
        }
    }
}