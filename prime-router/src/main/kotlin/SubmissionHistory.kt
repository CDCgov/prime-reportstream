package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

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