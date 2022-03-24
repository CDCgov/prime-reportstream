package gov.cdc.prime.router.tokens

/**
 * This represents a set of claims coming from Okta that have been authenticated (but may or may not
 * have been authorized). For convenience, certain claims have been pulled out into more readable/usable forms,
 * using helper methods in the class, called during object construction.
 */
class AuthenticatedClaims(
    val jwtClaims: Map<String, Any>,
) {
    // These are all derived from the raw jwtClaims.
    val userName: String
    val isPrimeAdmin: Boolean
    val organizationNameClaim: String?
    val isSenderOrgClaim: Boolean

    init {
        userName = jwtClaims[oktaSubjectClaim]?.toString() ?: error("No username in claims")
        @Suppress("UNCHECKED_CAST")
        val memberships = jwtClaims[oktaMembershipClaim] as? Collection<String> ?: error("No memberships in claims")
        val orgNamePair = extractOrganizationNameFromOktaMembership(memberships)
        isPrimeAdmin = isPrimeAdmin(memberships)
        organizationNameClaim = orgNamePair?.first // might be null
        isSenderOrgClaim = orgNamePair?.second ?: false
    }

    /**
     * Derive useful info from jwtClaims on [memberships].
     * Return the *first* well-formed ReportStream organizationName found in [memberships].
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
                "organization" to listOf("$oktaSenderGroupPrefix$tmpOrg", oktaSystemAdminGroup),
                "sub" to "local@test.com",
            )
            return AuthenticatedClaims(jwtClaims)
        }
    }
}