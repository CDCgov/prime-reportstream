package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * jijijijkinokjnjkojnk
 *
 * @param actionId of the Submission is `action_id` from the `action` table
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class ReportFileHistory(
    val actionId: Long,
    @JsonProperty("timestamp")
    val createdAt: OffsetDateTime,
    val httpStatus: Int,
    @JsonInclude(Include.NON_NULL)
    val externalName: String? = "",
    @JsonIgnore
    val reportId: String? = null,
    @JsonIgnore
    val schemaTopic: String? = null,
    @JsonIgnore
    val itemCount: Int? = null
) {
    /**
     * The report ID.
     */
    val id = reportId

    /**
     * The topic.
     */
    val topic = schemaTopic

    /**
     * The number of items in the report.
     */
    val reportItemCount = itemCount
}