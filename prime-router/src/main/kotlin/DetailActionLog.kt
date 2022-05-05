package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import java.util.UUID

/**
 * Detail action log class used to read the data from the database.
 *
 * @param scope the level in which this log ocurred (e.g. report, item...)
 * @param reportId unique identifier for the report that owns this log
 * @param index position in the report of the item that caused this log
 * @param trackingId id for identifying the test this log is related to
 * @param type what kind of log is this? (e.g. filter, warning...)
 * @param detail additional information for this log
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