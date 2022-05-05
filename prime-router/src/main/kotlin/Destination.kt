package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import gov.cdc.prime.router.azure.WorkflowEngine
import java.time.OffsetDateTime

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