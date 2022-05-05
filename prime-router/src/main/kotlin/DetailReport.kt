package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This is a container for various bits of report data used by DetailedReportFileHistory
 *
 * @param reportId unique identifier for this specific report
 * @param receivingOrg where is this report going?
 * @param receivingOrgSvc what service is receiving this report?
 * @param sendingOrg who sent this report?
 * @param sendingOrgClient what service did the sender use to send this report?
 * @param schemaTopic the kind of data contained in the report (e.g. "covid-19")
 * @param externalName actual filename of the report's file
 * @param createdAt when the report was created
 * @param nextActionAt when the report is next expected to send or process
 * @param itemCount number of tests (data rows) contained in the report
 * @param itemCountBeforeQualFilter number of tests that were submitted by the sender
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailReport(
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
    val schemaTopic: String?,
    val externalName: String?,
    val createdAt: OffsetDateTime?,
    val nextActionAt: OffsetDateTime?,
    val itemCount: Int,
    @JsonIgnore
    val itemCountBeforeQualFilter: Int?,
)