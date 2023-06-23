package gov.cdc.prime.router.transport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TokenInfo models OAuth2 access token as used by NY
 * OAuth token: https://datatracker.ietf.org/doc/html/rfc6749#section-4.4
 * based on ktor sample: https://github.com/ktorio/ktor-documentation/blob/main/codeSnippets/snippets/client-auth-oauth-google/src/main/kotlin/com/example/models/TokenInfo.kt
 *
 * @param accessToken token used in Authorization header
 * @param expiresIn seconds until token expires
 * @param refreshToken get new access tokens without having to log in again
 * @param scope what the token grants access to
 * @param tokenType type of token, usually 'Bearer'
 */
@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
    @SerialName("token_type") val tokenType: String? = null
)

/**
 * IdToken models the authentication token used by OK
 * https:/labupload.health.ok.gov/api/auth/token
 *
 * @param email username used to log in to the app
 * @param idToken id-token used in Authorization header
 * @param expiresIn seconds until token expires
 * @param refreshToken get new access tokens without having to log in again
 */
@Serializable
data class IdToken(
    val email: String,
    val idToken: String,
    val expiresIn: Int? = null,
    val refreshToken: String? = null
)