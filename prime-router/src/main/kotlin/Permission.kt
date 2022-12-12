package gov.cdc.prime.router

import java.time.OffsetDateTime

data class Permission(
    val permissionId: Int,
    val name: String,
    val description: String?,
    val enabled: Boolean? = true,
    val createdBy: String?,
    val createdAt: OffsetDateTime? = null
)