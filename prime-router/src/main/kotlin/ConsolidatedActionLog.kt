package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include

/**
 * Consolidated action log class to be output to the API JSON response.
 * @param log the base log message to be consolidated
 */
@JsonInclude(Include.NON_NULL)
class ConsolidatedActionLog(log: DetailActionLog) {
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

    init {
        scope = log.scope
        type = log.type
        message = log.detail.message
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
    fun add(log: DetailActionLog) {
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
    fun canBeConsolidatedWith(other: DetailActionLog): Boolean {
        return this.message == other.detail.message && this.scope == other.scope && this.type == other.type
    }
}