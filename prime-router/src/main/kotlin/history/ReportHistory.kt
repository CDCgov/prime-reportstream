package gov.cdc.prime.router.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ErrorCode
import gov.cdc.prime.router.ItemActionLogDetail
import gov.cdc.prime.router.Topic
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This class provides a base structure for data reflected in the `report_file` table.
 * When a report is sent, received, processed or downloaded, one of these entries is created.
 * The small amount of data makes this ideal for lists.
 *
 * @property actionId reference to the `action` table for the action that created this file
 * @property createdAt when the file was created
 * @property externalName actual filename of the file
 * @property reportId unique identifier for this specific report file
 * @property topic the kind of data contained in the report (e.g. "covid-19")
 * @property reportItemCount number of tests (data rows) contained in the report
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class ReportHistory(
    val actionId: Long,
    @JsonProperty("timestamp")
    val createdAt: OffsetDateTime,
    @JsonInclude(Include.NON_NULL)
    var externalName: String? = "",
    @JsonProperty("id")
    var reportId: String? = null,
    var topic: Topic? = null,
    var reportItemCount: Int? = null
)

/**
 * This is a container for various bits of report data used by DetailedReportFileHistory
 *
 * @property reportId unique identifier for this specific report
 * @property receivingOrg where is this report going?
 * @property receivingOrgSvc what service is receiving this report?
 * @property sendingOrg who sent this report?
 * @property sendingOrgClient what service did the sender use to send this report?
 * @property schemaTopic the kind of data contained in the report (e.g. "covid-19")
 * @property externalName actual filename of the report's file
 * @property createdAt when the report was created
 * @property nextActionAt when the report is next expected to send or process
 * @property itemCount number of tests (data rows) contained in the report
 * @property itemCountBeforeQualFilter number of tests that were submitted by the sender
 * @property receiverHasTransport whether a receiver has set up a transport method
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedReport(
    @JsonIgnore
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
    val schemaTopic: Topic?,
    val externalName: String?,
    val createdAt: OffsetDateTime?,
    val nextActionAt: OffsetDateTime?,
    val itemCount: Int,
    @JsonIgnore
    val itemCountBeforeQualFilter: Int?,
    @JsonIgnore
    val receiverHasTransport: Boolean
)

/**
 * Consolidated action log class to be output to the API JSON response.
 * @param log the base log message to be consolidated
 */
@JsonInclude(Include.NON_NULL)
class ConsolidatedActionLog(log: DetailedActionLog) {
    /**
     * The scope of the log.
     */
    val scope: ActionLogScope

    /**
     * The list of indices for item logs. An index can be null if there was no index provided with the log.
     */
    val indices: MutableList<Int?>?

    /**
     * The list of tracking IDs for item logs. A tracking ID can be null if there was no ID provided with the log.
     */
    val trackingIds: MutableList<String?>?

    /**
     * The log level.
     */
    @JsonIgnore
    val type: ActionLogLevel

    /**
     * The field mapping for item logs.
     */
    val field: String?

    /**
     * The log message.
     */
    val message: String

    /**
     * The error code for the message.
     */
    val errorCode: ErrorCode

    init {
        scope = log.scope
        type = log.type
        message = log.detail.message
        errorCode = log.detail.errorCode
        if (log.detail.scope == ActionLogScope.item) {
            field = if (log.detail is ItemActionLogDetail) log.detail.fieldMapping else null
            indices = mutableListOf()
            trackingIds = mutableListOf()
        } else {
            indices = null
            trackingIds = null
            field = null
        }
        add(log)
    }

    /**
     * Add an action detail [log] to this consolidated log.
     */
    fun add(log: DetailedActionLog) {
        check(message == log.detail.message)
        if (indices != null && trackingIds != null) {
            indices.add(log.index)
            trackingIds.add(log.trackingId)
        }
    }

    /**
     * Tests if a detail action log [other] can be consolidated into this existing consolidated log.
     * @return true if the log can be consolidated, false otherwise
     */
    fun canBeConsolidatedWith(other: DetailedActionLog): Boolean {
        return this.message == other.detail.message && this.scope == other.scope && this.type == other.type
    }
}

/**
 * Detail action log class used to read the data from the database.
 *
 * @property scope the level in which this log occurred (e.g. report, item...)
 * @property reportId unique identifier for the report that owns this log
 * @property index position in the report of the item that caused this log
 * @property trackingId id for identifying the test this log is related to
 * @property type what kind of log is this? (e.g. filter, warning...)
 * @property detail additional information for this log
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class DetailedActionLog(
    val scope: ActionLogScope,
    @JsonIgnore
    val reportId: UUID?,
    val index: Int?,
    val trackingId: String?,
    val type: ActionLogLevel,
    val detail: ActionLogDetail
)