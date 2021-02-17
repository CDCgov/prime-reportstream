package gov.cdc.prime.router.azure

enum class PrincipalLevel {
    SYSTEM_ADMIN,
    ORGANIZATION_ADMIN,
    USER
}

data class AuthenticationClaims(
    val userName: String,
    val principalLevel: PrincipalLevel,
    val organizationName: String
)

interface AuthenticationVerifier {
    fun checkClaims(
        accessToken: String,
        principalLevel: PrincipalLevel,
        organizationName: String? = null
    ): AuthenticationClaims?
}

class TestAuthenticationVerifier : AuthenticationVerifier {
    override fun checkClaims(
        accessToken: String,
        principalLevel: PrincipalLevel,
        organizationName: String?
    ): AuthenticationClaims {
        return AuthenticationClaims("todo", principalLevel, organizationName ?: "prime")
    }
}