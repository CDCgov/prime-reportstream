package gov.cdc.prime.router.messageTracker

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import gov.cdc.prime.router.ActionLogDetail

/**
 * Detail action log class used to read the data from the database.
 *
 * @property trackingId id for identifying the test this log is related to
 * @property detail additional information for this log
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class MessageActionLog(
    val trackingId: String?,
    val detail: ActionLogDetail
)