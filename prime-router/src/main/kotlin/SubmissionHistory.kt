package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
class SubmissionHistory(
    @JsonProperty("submissionId")
    actionId: Long,
    createdAt: OffsetDateTime,
    @JsonProperty("sender")
    val sendingOrg: String,
    httpStatus: Int,
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