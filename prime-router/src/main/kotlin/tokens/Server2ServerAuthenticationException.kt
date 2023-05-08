package tokens

enum class OAuthErrorType {
    INVALID_REQUEST,
    INVALID_CLIENT,
    INVALID_GRANT,
    UNAUTHORIZED_CLIENT,
    UNSUPPORTED_GRANT_TYPE,
    INVALID_SCOPE
}

enum class Server2ServerError(uri: String?, oAuthErrorType: OAuthErrorType) {
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
class Server2ServerAuthenticationException(error: Server2ServerError, val scope: String) : Exception() {
    override fun getLocalizedMessage(): String {
        return "AccessToken Request Denied: Error while requesting $scope"
    }
}