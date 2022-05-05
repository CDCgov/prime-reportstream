package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import java.util.UUID

/**
 * Detail action log class used to read the data from the database.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class DetailActionLog(
    val scope: ActionLogScope,
    @JsonIgnore
    val reportId: UUID?,
    val index: Int?,
    val trackingId: String?,
    val type: ActionLogLevel,
    val detail: ActionLogDetail,
)