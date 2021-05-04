package gov.cdc.prime.router.azure

import com.okta.jwt.JwtVerifiers

// These constants match how PRIME Okta subscription is configured
const val oktaGroupPrefix = "DH"
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
     */
    fun checkClaims(
        accessToken: String,
        minimumLevel: PrincipalLevel,
        organizationName: String? = null
    ): AuthenticatedClaims?
}

class TestAuthenticationVerifier : AuthenticationVerifier {
    override val requiredHosts = listOf("localhost", "prime_dev")

    override fun checkClaims(
        accessToken: String,
        minimumLevel: PrincipalLevel,
        organizationName: String?
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
        organizationName: String?
    ): AuthenticatedClaims? {
        val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
            .setIssuer("https://$issuerBaseUrl/oauth2/default")
            .build()
        val jwt = jwtVerifier.decode(accessToken)

        val userName = jwt.claims[oktaSubjectClaim]?.toString() ?: return null
        val memberships = jwt.claims[oktaMembershipClaim] as? Collection<String> ?: return null
        if (!checkMembership(memberships, minimumLevel, organizationName)) return null
        return AuthenticatedClaims(userName, minimumLevel, organizationName)
    }

    internal fun checkMembership(
        memberships: Collection<String>,
        minimumLevel: PrincipalLevel,
        organizationName: String?
    ): Boolean {
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
                    "$oktaGroupPrefix$groupName",
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