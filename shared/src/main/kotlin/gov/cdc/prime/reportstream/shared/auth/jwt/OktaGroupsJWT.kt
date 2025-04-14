package gov.cdc.prime.reportstream.shared.auth.jwt

/**
 * Model containing the useful fields from our Okta Groups JWT
 */
data class OktaGroupsJWT(val appId: String, val groups: List<String>)