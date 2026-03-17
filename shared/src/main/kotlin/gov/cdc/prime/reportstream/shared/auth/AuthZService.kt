package gov.cdc.prime.reportstream.shared.auth

import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWTReader

/**
 * Shared authorization service to allow routes to check if an incoming request should be allowed access
 */
class AuthZService(private val oktaGroupsJWTReader: OktaGroupsJWTReader) {

    private val adminGroup = "DHPrimeAdmins"
    private val senderPrefix = "DHSender_"

    /**
     * Simpler sender authorization check function that assumings you have the JWT parsing already completed
     */
    fun isSenderAuthorized(
        clientId: String,
        oktaGroups: List<String>,
    ): Boolean = oktaGroups.any { senderAuthorized(clientId, it) }

    /**
     * Check that a sender matches our client id
     *
     * A user with an admin group will always be authorized
     *
     * For other users, we will ensure that our group name suffix matches the organization name prefix
     * ex:
     * clientId=org.test, oktaGroup=DHSender_org, authorized=true
     * clientId=org.test, oktaGroup=DHSender_differentOrg, authorized=false
     */
    private fun senderAuthorized(clientId: String, oktaGroup: String): Boolean = if (oktaGroup == adminGroup) {
            true
        } else if (oktaGroup.startsWith(senderPrefix)) {
            val oktaOrganization = oktaGroup
                .substringAfter(senderPrefix)
                .trim()

            val parsedClientId = clientId
                .split(".")
                .first()
                .trim()

            parsedClientId == oktaOrganization
        } else {
            false
        }
}