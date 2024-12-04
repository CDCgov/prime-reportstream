package gov.cdc.prime.reportstream.auth.service

import gov.cdc.prime.reportstream.auth.client.OktaGroupsClient
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWT
import org.springframework.stereotype.Service

@Service
class OktaGroupsService(
    private val oktaGroupsClient: OktaGroupsClient,
    private val oktaGroupsJWTWriter: OktaGroupsJWTWriter,
) {

    /**
     * Grab Okta groups from the Okta API and write the JWT
     */
    suspend fun generateOktaGroupsJWT(appId: String): String {
        val groups = oktaGroupsClient.getApplicationGroups(appId)
        return oktaGroupsJWTWriter.write(OktaGroupsJWT(appId, groups))
    }
}