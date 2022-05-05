package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import gov.cdc.prime.router.azure.WorkflowEngine
import java.time.OffsetDateTime

/**
 * Represents the organizations that receive submitted reports from the point of view of a Submission.
 *
 * @param organizationId identifier for the organization that owns this destination
 * @param service the service used by the organization (e.g. elr)
 * @param filteredReportRows filters that were triggered by the contents of the report
 * @param filteredReportItems more structured version of filteredReportRows
 * @param sendingAt the time that this destination is next expecting to receive a report
 * @param itemCount final number of tests available in the report received by the destination
 * @param itemCountBeforeQualFilter total number of tests that were in the submitted report before any filtering
 * @param sentReports logs of reports for this submission sent to this destination
 * @param downloadedReports logs of reports for this submission downloaded for this destination
 */
@JsonPropertyOrder(
    value = [
        "organization", "organizationId", "service", "itemCount", "itemCountBeforeQualFilter", "sendingAt"
    ]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Destination(
    @JsonProperty("organization_id")
    val organizationId: String,
    val service: String,
    val filteredReportRows: List<String>?,
    val filteredReportItems: List<ReportStreamFilterResultForResponse>?,
    @JsonProperty("sending_at")
    @JsonInclude(Include.NON_NULL)
    val sendingAt: OffsetDateTime?,
    val itemCount: Int,
    @JsonProperty("itemCountBeforeQualityFiltering")
    val itemCountBeforeQualFilter: Int?,
    var sentReports: MutableList<DetailReport> = mutableListOf(),
    var downloadedReports: MutableList<DetailReport> = mutableListOf(),
) {
    val organization: String?
        get() = WorkflowEngine.settingsProviderSingleton.findOrganizationAndReceiver(
            "$organizationId.$service"
        )?.let { (org, _) ->
            org.description
        }
}