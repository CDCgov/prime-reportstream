package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * This class handles ReportFileHistory for Submissions from a sender.
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
class SubmissionHistory(
    @JsonProperty("submissionId")
    actionId: Long,
    @JsonProperty("timestamp")
    createdAt: OffsetDateTime,
    @JsonProperty("sender")
    val sendingOrg: String,
    httpStatus: Int,
    @JsonInclude(Include.NON_NULL)
    externalName: String? = "",
    reportId: String? = null,
    schemaTopic: String? = null,
    itemCount: Int? = null
) : ReportFileHistory(
    actionId,
    createdAt,
    httpStatus,
    externalName,
    reportId,
    schemaTopic,
    itemCount,
)