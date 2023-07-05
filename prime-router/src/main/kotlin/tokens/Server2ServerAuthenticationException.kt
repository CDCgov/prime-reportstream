package tokens

/**
 * The possible error codes that can be returned by the OAuth server
 * Matches values found here: https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
 */
enum class OAuthErrorType {
    INVALID_REQUEST,
    INVALID_CLIENT,
    INVALID_SCOPE
}

/**
 * Enum maps the possible error conditions that can occur while verifying a JWS to the error code and the location
 * on the report stream site that can be used to debug the error.
 *
 * @param errorUri the URI that links to a location in the ReportStream site that provides information for debugging
 * @param oAuthErrorType the error code that should be returned for the specific issue
 */
enum class Server2ServerError(val errorUri: String, val oAuthErrorType: OAuthErrorType) {
    // Error is generated when the JWS sent to the server is expired
    EXPIRED_TOKEN("expired-token", OAuthErrorType.INVALID_CLIENT),
    // Error is generated when the JWT is not signed
    UNSIGNED_JWT("unsigned-jwt", OAuthErrorType.INVALID_REQUEST),
    // Error is generated when the client_assertion is not a valid JWS
    MALFORMED_JWT("malformed-jwt", OAuthErrorType.INVALID_REQUEST),
    // Error is generated when the scope requested is not valid, see Scope.isValidScope
    INVALID_SCOPE("invalid-scope", OAuthErrorType.INVALID_SCOPE),
    // Error is generated when there were no keys that could be used to verify the JWS which can occur in a variety
    // of situations such as no keys were associated with the scope, the JWS was already used, none of the keys with the
    // scope could be used to decrypt the JWS
    NO_VALID_KEYS("no-valid-keys", OAuthErrorType.INVALID_CLIENT),
    // There were no organizations that matches the iss claim in the JWS
    NO_ORG_FOUND_FOR_ISS("no-matching-organization", OAuthErrorType.INVALID_CLIENT),
    // Error is generated if the request does not include a client_assertion (the JWS)
    MISSING_CLIENT_ASSERTION("missing-jwt", OAuthErrorType.INVALID_REQUEST),
    // Error is generated if the request does not include a client_assertion (the JWS)
    MISSING_SCOPE("missing-scope", OAuthErrorType.INVALID_REQUEST)
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
        val message = "${server2ServerError.name} while generating token for scope: $scope"
        if (iss != null) {
            return "$message for issuer: $iss"
        }
        return message
    }
}