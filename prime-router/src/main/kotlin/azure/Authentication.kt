package gov.cdc.prime.router.azure

import com.okta.jwt.JwtVerifiers

// These constants match how PRIME Okta subscription is configured
const val oktaGroupPrefix = "DH"
const val oktaSenderGroupPrefix = "DHSender_"
const val oktaAdminGroupSuffix = "Admins"
const val oktaSystemAdminGroup = "DHPrimeAdmins"
const val oktaSubjectClaim = "sub"
const val oktaMembershipClaim = "organization"
const val envVariableForOktaBaseUrl = "OKTA_baseUrl"

enum class PrincipalLevel {
    SYSTEM_ADMIN,
    ORGANIZATION_ADMIN,
    USER
}

data class AuthenticatedClaims(
    val userName: String,
    val principalLevel: PrincipalLevel,
    val organizationName: String?
)

interface AuthenticationVerifier {
    /**
     * If present, limit the scope that this verifier can be used
     */
    val requiredHosts: List<String>

    /**
     * Return AuthenticatedClaims if accessToken is valid. Null otherwise.
     * @param accessToken
     * @param minimumLevel
     * @param organizationName
     * @param oktaSender (optional) We are expecting the user to be part of a group in Okta called "DHSender_<organization name>.<sender name>
     */
    fun checkClaims(
        accessToken: String,
        minimumLevel: PrincipalLevel,
        organizationName: String? = null,
        oktaSender: Boolean = false
    ): AuthenticatedClaims?
}

class TestAuthenticationVerifier : AuthenticationVerifier {
    override val requiredHosts = listOf("localhost", "prime_dev")

    override fun checkClaims(
        accessToken: String,
        minimumLevel: PrincipalLevel,
        organizationName: String?,
        oktaSender: Boolean
    ): AuthenticatedClaims {
        return AuthenticatedClaims("local@test.com", minimumLevel, organizationName)
    }
}

class OktaAuthenticationVerifier : AuthenticationVerifier {
    private val issuerBaseUrl: String = System.getenv(envVariableForOktaBaseUrl) ?: ""

    override val requiredHosts = emptyList<String>()

    override fun checkClaims(
        accessToken: String,
        minimumLevel: PrincipalLevel,
        organizationName: String?,
        oktaSender: Boolean
    ): AuthenticatedClaims? {
        val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
            .setIssuer("https://$issuerBaseUrl/oauth2/default")
            .build()
        val jwt = jwtVerifier.decode(accessToken)

        val userName = jwt.claims[oktaSubjectClaim]?.toString() ?: return null
        @Suppress("UNCHECKED_CAST")
        val memberships = jwt.claims[oktaMembershipClaim] as? Collection<String> ?: return null

        if (!checkMembership(memberships, minimumLevel, organizationName, oktaSender)) return null
        return AuthenticatedClaims(userName, minimumLevel, organizationName)
    }


    internal fun checkMembership(
        memberships: Collection<String>,
        minimumLevel: PrincipalLevel,
        organizationName: String?,
        oktaSender: Boolean = false
    ): Boolean {
        // We are expecting a group name of:
        // DH<org name> if oktaSender is false
        // DHSender_<org name>.<sender name> if oktaSender is true
        // Example receiver: If the receiver org name is "ignore", the Okta group name will be "DHignore
        // Example sender: If the sender org name is "ignore", and the sender name is "ignore-waters",
        // the Okta group name will be "DHSender_ignore.ignore_waters
        val groupName = organizationName?.replace('-', '_')
        val lookupMemberships = when (minimumLevel) {
            PrincipalLevel.SYSTEM_ADMIN -> listOf(oktaSystemAdminGroup)
            PrincipalLevel.ORGANIZATION_ADMIN -> {
                listOf(
                    "$oktaGroupPrefix$groupName$oktaAdminGroupSuffix",
                    oktaSystemAdminGroup
                )
            }
            PrincipalLevel.USER ->
                listOf(
                    "${if (oktaSender) oktaSenderGroupPrefix else oktaGroupPrefix}$groupName",
                    "$oktaGroupPrefix$groupName$oktaAdminGroupSuffix",
                    oktaSystemAdminGroup
                )
        }
        lookupMemberships.forEach {
            if (memberships.contains(it)) return true
        }
        return false
    }
}