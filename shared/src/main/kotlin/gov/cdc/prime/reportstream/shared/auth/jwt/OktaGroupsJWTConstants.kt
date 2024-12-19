package gov.cdc.prime.reportstream.shared.auth.jwt

object OktaGroupsJWTConstants {

    // Custom okta groups header name
    const val OKTA_GROUPS_HEADER = "Okta-Groups"

    // Non-application users have okta groups automatically injected into this claim
    const val OKTA_GROUPS_JWT_GROUP_CLAIM = "groups"
}