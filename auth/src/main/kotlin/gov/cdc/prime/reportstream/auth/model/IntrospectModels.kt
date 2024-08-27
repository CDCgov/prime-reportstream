package gov.cdc.prime.reportstream.auth.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.LinkedMultiValueMap

data class IntrospectBody(
    val token: String,
) {
    val formDataMap = LinkedMultiValueMap(
        mapOf(
            "token" to listOf(token),
            "token_type_hint" to listOf("access_token")
        )
    )
}

data class IntrospectResponse(
    val active: Boolean,
    val aud: String?,
    @JsonProperty("client_id") val clientId: String?,
    @JsonProperty("device_id") val deviceId: String?,
    val exp: Int?,
    val iat: Int?,
    val iss: String?,
    val jti: String?,
    val nbf: Int?,
    val scope: String?,
    val sub: String?,
    @JsonProperty("token_type") val tokenType: String?,
    val uid: String?,
    val username: String?,
) {
    companion object {
        val localPassthrough =
            IntrospectResponse(
                active = true, null, null, null, null, null, null, null, null, null, null, null, null, null
            )
    }
}