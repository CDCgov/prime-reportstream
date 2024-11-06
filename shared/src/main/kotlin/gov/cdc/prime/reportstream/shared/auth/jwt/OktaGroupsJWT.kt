package gov.cdc.prime.reportstream.shared.auth.jwt

data class OktaGroupsJWT(
    val appId: String,
    val groups: List<String>
)
