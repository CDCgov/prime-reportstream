package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.Sender
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts

/**
 * This represents a set of claims coming from Okta that have been authenticated (but may or may not
 * have been authorized). For convenience, certain claims have been pulled out into more readable/usable forms,
 * using helper methods in the class, called during object construction.
 */
class AuthenticatedClaims {
    /**
     * Raw claims map, typically as sent in a JWT
     */
    val jwtClaims: Map<String, Any>

    // For Okta, these below are all derived from the raw jwtClaims.

    /** Name of user as extracted from the subject claim.  Usually an email address */
    val userName: String
    /** Does this user have the prime administrator claim? */
    var isPrimeAdmin: Boolean
    /** Name of the organization found in these claims */
    val organizationNameClaim: String?
    /** Is this a Sender claim, of the form DHSender_orgname  ? */
    val isSenderOrgClaim: Boolean

    /**
     * This constructor assumes certain claims that are found in Okta. Only use this for Okta claims.
     */
    constructor(_jwtClaims: Map<String, Any>) {
        this.jwtClaims = _jwtClaims
        this.userName = _jwtClaims[oktaSubjectClaim]?.toString() ?: error("No username in claims")
        @Suppress("UNCHECKED_CAST")
        val memberships = jwtClaims[oktaMembershipClaim] as? Collection<String> ?: error("No memberships in claims")
        val orgNamePair = extractOrganizationNameFromOktaMembership(memberships)
        this.isPrimeAdmin = isPrimeAdmin(memberships)
        this.organizationNameClaim = orgNamePair?.first // might be null
        this.isSenderOrgClaim = orgNamePair?.second ?: false
    }

    /**

     * Use this for claims not coming from a human being (that is, not using Okta).
     * For example, use this for claims for server-to-server auth.
     * Each value passed must have been authenticated.  These are *not* for requested or required authorizations.
     *
     * [_jwtClaims]  The raw list of claims.
     * [_organizationNameClaim] The organization Name of this server. (Not the full name, eg oh-doh, not oh-doh.elr)
     * [_isPrimeAdmin] true if this server claims to act as a PrimeAdmin
     * [_isSenderOrgClaim] true if this claims set is from a Sender organization.  Certain queries (eg, give me
     * a list of submissions) only make sense if the Organization has at least one Sender in it.
     *
     * The latter args are optional.   If not provided, this constructor will attempt to extract the values in the jwt
     * claims, following our Okta claim forms (eg, "DH...", etc).   Otherwise it will default to least privilege.
     */
    constructor(
        _jwtClaims: Map<String, Any>,
        _organizationNameClaim: String? = null,
        _isPrimeAdmin: Boolean? = null,
        _isSenderOrgClaim: Boolean? = null,
    ) {
        this.jwtClaims = _jwtClaims
        this.userName = _jwtClaims[oktaSubjectClaim]?.toString() ?: error("No username in claims")
        @Suppress("UNCHECKED_CAST")
        val memberships = jwtClaims[oktaMembershipClaim] as? Collection<String> ?: emptyList()
        val orgNamePair = extractOrganizationNameFromOktaMembership(memberships)
        this.isPrimeAdmin = _isPrimeAdmin ?: isPrimeAdmin(memberships)
        this.organizationNameClaim = _organizationNameClaim ?: orgNamePair?.first // might be null
        this.isSenderOrgClaim = _isSenderOrgClaim ?: orgNamePair?.second ?: false
    }

    /**
     * Derive useful info from jwtClaims on [memberships].
     * @return the *first* well-formed ReportStream organizationName found in [memberships].
     * (So this won't work if user has many organization claims.)
     *
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
     * @return true if a well-formed Administrator claim is in [memberships].  False otherwise.
     */
    private fun isPrimeAdmin(memberships: Collection<String>): Boolean {
        memberships.forEach {
            if (it == oktaSystemAdminGroup) return true
        }
        return false
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
                "organization" to listOf("$oktaSenderGroupPrefix$tmpOrg", oktaSystemAdminGroup),
                "sub" to "local@test.com",
            )
            return AuthenticatedClaims(jwtClaims)
        }

        /**
         * Create fake twolegged TokenAuthentication claims, for testing.
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