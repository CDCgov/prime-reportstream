package gov.cdc.prime.reportstream.shared.auth

import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWTConstants
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWTReader

class AuthZService(
    private val oktaGroupsJWTReader: OktaGroupsJWTReader
) {

    private val adminGroup = "DHPrimeAdmins"
    private val senderPrefix = "DHSender_"

    fun isSenderAuthorized(clientId: String, requestHeaderFn: (String) -> String): Boolean {
        val oktaGroupsHeader = requestHeaderFn(OktaGroupsJWTConstants.OKTA_GROUPS_HEADER)
        val oktaGroupsJWT = oktaGroupsJWTReader.read(oktaGroupsHeader)

        return isSenderAuthorized(clientId, oktaGroupsJWT.groups)
    }

    fun isSenderAuthorized(clientId: String, oktaGroups: List<String>): Boolean {
        return oktaGroups.any { senderAuthorized(clientId, it) }
    }

    private fun senderAuthorized(clientId: String, oktaGroup: String): Boolean {
        return if (oktaGroup == adminGroup) {
            true
        } else if (oktaGroup.startsWith(senderPrefix)) {
            val oktaOrganization = oktaGroup
                .substringAfter(senderPrefix)
                .trim()

            clientId.trim().startsWith(oktaOrganization)
        } else {
            false
        }
    }


}