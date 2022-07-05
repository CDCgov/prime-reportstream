package gov.cdc.prime.router.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import java.time.OffsetDateTime

/**
 * This class handles ReportFileHistory for Deliveries for a receiver.
 *
 * @property actionId reference to the `action` table for the action that created this file
 * @property createdAt when the file was created
 * @property externalName actual filename of the file
 * @property reportId unique identifier for this specific report file
 * @property topic the kind of data contained in the report (e.g. "covid-19")
 * @property reportItemCount number of tests (data rows) contained in the report
 * @property receivingOrg the name of the organization that's receiving this submission
 * @property receivingOrgSvc the name of the organization's service that's receiving this submission
 * @property bodyUrl url used for generating the filename
 * @property schemaName schema used for generating the filename
 * @property bodyFormat filetype, used for generating the filename
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(
    value = [
        "deliveryId", "sent", "expires", "receivingOrg", "receivingOrgSvc", "httpStatus",
        "reportId", "topic", "reportItemCount", "fileName", "fileType",
    ]
)
class DeliveryHistory(
    @JsonProperty("deliveryId")
    actionId: Long,
    @JsonProperty("sent")
    createdAt: OffsetDateTime,
    externalName: String? = "",
    reportId: String? = null,
    topic: String? = null,
    @JsonProperty("reportItemCount")
    reportItemCount: Int? = null,
    val receivingOrg: String,
    val receivingOrgSvc: String,
    @JsonIgnore
    val bodyUrl: String? = null,
    @JsonIgnore
    val schemaName: String,
    @JsonProperty("fileType")
    val bodyFormat: String,
) : ReportHistory(
    actionId,
    createdAt,
    externalName,
    reportId,
    topic,
    reportItemCount,
) {
    @JsonIgnore
    private val DAYS_TO_SHOW = 30L

    /**
     * The time that the report is expected to no longer be available.
     */
    val expires: OffsetDateTime get() {
        return this.createdAt.plusDays(DAYS_TO_SHOW)
    }

    /**
     * The actual download path for the file.
     */
    val fileName: String get() {
        return Report.formExternalFilename(
            this.bodyUrl,
            ReportId.fromString(this.reportId),
            this.schemaName,
            Report.Format.safeValueOf(this.bodyFormat),
            this.createdAt
        )
    }
}