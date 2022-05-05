package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * This class provides a base structure for data reflected in the `report_file` table.
 * When a report is sent, received, processed or downloaded, one of these entries is created.
 * The small amount of data makes this ideal for lists.
 *
 * @param actionId reference to the `action` table for the action that created this file
 * @param createdAt when the file was created
 * @param httpStatus response code for the user fetching this report file
 * @param externalName actual filename of the file
 * @param reportId unique identifier for this specific report file
 * @param schemaTopic the kind of data contained in the report (e.g. "covid-19")
 * @param itemCount number of tests (data rows) contained in the report
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