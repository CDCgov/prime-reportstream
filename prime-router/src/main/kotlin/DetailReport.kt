package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailReport(
    val reportId: UUID,
    @JsonIgnore
    val receivingOrg: String?,
    @JsonIgnore
    val receivingOrgSvc: String?,
    @JsonIgnore
    val sendingOrg: String?,
    @JsonIgnore
    val sendingOrgClient: String?,
    @JsonIgnore
    val schemaTopic: String?,
    val externalName: String?,
    val createdAt: OffsetDateTime?,
    val nextActionAt: OffsetDateTime?,
    val itemCount: Int,
    @JsonIgnore
    val itemCountBeforeQualFilter: Int?,
)