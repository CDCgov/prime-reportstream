package gov.cdc.prime.router.transport

import kotlinx.serialization.Serializable

@Serializable
data class TokenInfo(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val scope: String? = null,
    val token_type: String,
    val id_token: String? = null,
)