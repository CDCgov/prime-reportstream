package tokens

enum class OAuthErrorType {
    INVALID_REQUEST,
    INVALID_CLIENT,
    INVALID_GRANT,
    UNAUTHORIZED_CLIENT,
    UNSUPPORTED_GRANT_TYPE,
    INVALID_SCOPE
}

enum class Server2ServerError(val uri: String, val oAuthErrorType: OAuthErrorType) {
    EXPIRED_TOKEN("expired", OAuthErrorType.INVALID_CLIENT),
    UNSIGNED_JWT("signed-jwt", OAuthErrorType.INVALID_REQUEST),
    MALFORMED_JWT("valid-jwt", OAuthErrorType.INVALID_REQUEST),
    INVALID_SCOPE("valid-scope", OAuthErrorType.INVALID_SCOPE),
    NO_VALID_KEYS("adding-public-key", OAuthErrorType.INVALID_CLIENT),
    KID_DOES_NOT_MATCH_ORG("kid-must-match-org", OAuthErrorType.INVALID_CLIENT)
}

/**
 * Exception class that captures errors from internal logic performed when authenticating; i.e.
 * the issuer does not match an organization or sender
 */
class Server2ServerAuthenticationException(
    val server2ServerError: Server2ServerError,
    val scope: String,
    val iss: String? = null
) :
    Exception() {
    override fun getLocalizedMessage(): String {
        val message = "AccessToken Request Denied: ${server2ServerError.name} while generating token for scope: $scope"
        if (iss != null) {
            return "$message for issuer: $iss"
        }
        return message
    }
}