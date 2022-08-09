package gov.cdc.prime.router.tokens

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.Sender
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.apache.logging.log4j.kotlin.Logging

/**
 * This represents a set of claims coming from Okta that have been authenticated (but may or may not
 * have been authorized). For convenience, certain claims have been pulled out into more readable/usable forms,
 * using helper methods in the class, called during object construction.
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

    /**
     * [_jwtClaims]  The raw list of claims
     * [isOktaAuth] true if the claims came from Okta (eg, groups like "DHmd-phd").  false if the claims came from
     * server2server auth (eg, scopes like md-phd.default.report)
     *
     * Each value passed must have been already authenticated.  This is for claims, *not* for required authorizations.
     */
    constructor(_jwtClaims: Map<String, Any>, isOktaAuth: Boolean) {
        this.jwtClaims = _jwtClaims
        this.userName = _jwtClaims[subjectClaim]?.toString() ?: error("No username in claims")
        if (isOktaAuth) {
            @Suppress("UNCHECKED_CAST")
            val memberships = jwtClaims[oktaMembershipClaim] as? Collection<String> ?: error("No memberships in claims")
            this.scopes = Scope.mapOktaGroupsToScopes(memberships)
        } else {
            // todo this assumes a single scope.  Need to add support for multiple scopes.
            val claimsScope = _jwtClaims["scope"] as String
            if (claimsScope.isEmpty())
                error("For $userName, server2server token had no scope defined. Not authenticated")
            if (!Scope.isValidScope(claimsScope))
                error("For $userName, server2server scope $claimsScope: invalid format")
            this.scopes = setOf(claimsScope)
        }
        this.isPrimeAdmin = scopes.contains(Scope.primeAdminScope)
        // todo : have the TokenAuthentication.authenticate return type AuthenticatedClaims.
    }

    /**
     * This uses the [claims] to determine if access is authorized to call the /validate and /waters endpoints for
     * sender [requiredSender].  The [request] is only used for logging.
     * @return true if the request to submit data on behalf of [requiredSender]
     * is authorized based on the [claims], false otherwise.
     */
    fun authorizedForSubmission(
        requiredSender: Sender,
        request: HttpRequestMessage<String?>
    ): Boolean {
        return authorizedForSubmission(requiredSender.organizationName, requiredSender.name, request)
    }

    /**
     * This uses the [claims] to determine if access is authorized to call the /validate and /waters endpoints for
     * org [requiredOrganization] and optional sender [requiredSender].  The [request] is only used for logging.
     *
     * @return true if the request to submit data on behalf of the [requiredOrganization] is
     * authorized based on the [claims], false otherwise.
     */
    fun authorizedForSubmission(
        requiredOrganization: String,
        requiredSender: String? = null,
        request: HttpRequestMessage<String?>
    ): Boolean {
        if (requiredOrganization.isBlank()) {
            logger.warn(
                "Unauthorized.  Missing required Org" +
                    " for user ${this.userName}: ${request.httpMethod}:${request.uri.path}."
            )
            return false
        }
        // User must have one of these scopes to be authorized
        val requiredScopes = mutableSetOf(
            Scope.primeAdminScope,
            "$requiredOrganization.*.user", // eg, md-phd.*.user
            "$requiredOrganization.*.admin", // eg, md-phd.*.admin
            "$requiredOrganization.*.report", // eg, md-phd.default.report
        )
        if (requiredSender != null) {
            requiredScopes += "$requiredOrganization.$requiredSender.user" // eg, md-phd.default.user
            requiredScopes += "$requiredOrganization.$requiredSender.admin" // eg, md-phd.default.admin
            requiredScopes += "$requiredOrganization.$requiredSender.report" // eg, md-phd.default.report
        }
        return if (Scope.authorized(this.scopes, requiredScopes)) {
            logger.info(
                "Authorized request by user with claims ${this.scopes}" +
                    " to submit data via client id $requiredOrganization.  Beginning to ingest report"
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

    companion object {
        /**
         * Create fake Okta Auth claims, for testing
         * @return fake claims, for testing.
         * Uses the organizationName in the [sender] if one is passed in, otherwise uses the `ignore` org.
         */
        fun generateTestClaims(sender: Sender? = null): AuthenticatedClaims {
            val tmpOrg = sender?.organizationName ?: "ignore"
            val jwtClaims: Map<String, Any> = mapOf(
                oktaMembershipClaim to listOf("$oktaSenderGroupPrefix$tmpOrg", oktaSystemAdminGroup),
                "sub" to "local@test.com",
            )
            return AuthenticatedClaims(jwtClaims, isOktaAuth = true)
        }

        /**
         * Create fake server2server TokenAuthentication claims, for testing.
         * Extract the orgname from the [scope] which must be well-formed per rules in [Scope]
         * @return fake claims, for testing.
         */
        fun generateTestJwtClaims(): Claims {
            val jwtClaims = Jwts.claims().setSubject("local@test.com")
            jwtClaims["organization"] = listOf(oktaSystemAdminGroup)
            jwtClaims["scope"] = Scope.primeAdminScope
            return jwtClaims
        }
    }
}