package gov.cdc.prime.router.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
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
        "deliveryId", "batchReadyAt", "expires", "receiver",
        "reportId", "topic", "reportItemCount", "fileName", "fileType"
    ]
)
class DeliveryHistory(
    @JsonProperty("deliveryId")
    actionId: Long,
    @JsonProperty("batchReadyAt")
    createdAt: OffsetDateTime,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    externalName: String? = "",
    reportId: String? = null,
    @JsonProperty("topic")
    schema_topic: Topic? = null,
    @JsonProperty("reportItemCount")
    itemCount: Int? = null,
    @JsonIgnore // Instead, use receiver, defined below.
    val receivingOrg: String,
    @JsonIgnore // Instead, use receiver, defined below.
    val receivingOrgSvc: String?,
    @JsonIgnore
    val bodyUrl: String? = null,
    @JsonIgnore
    val schemaName: String,
    @JsonProperty("fileType")
    val bodyFormat: String
) : ReportHistory(
    actionId,
    createdAt,
    externalName,
    reportId,
    schema_topic,
    itemCount
) {
    @JsonIgnore
    private val DAYS_TO_SHOW = 30L

    /**
     * The time that the report is expected to no longer be available.
     */
    val expires: OffsetDateTime
        get() {
            return this.createdAt.plusDays(DAYS_TO_SHOW)
        }

    /**
     * The actual download path for the file.
     */
    val fileName: String
        get() {
            return Report.formExternalFilename(
                this.bodyUrl,
                ReportId.fromString(this.reportId),
                this.schemaName,
                Report.Format.safeValueOf(this.bodyFormat),
                this.createdAt
            )
        }

    /**
     * The fullName of the recipient of the input report, or less, if missing some fields.
     */
    var receiver: String? = ""

    init {
        receiver = when {
            receivingOrg.isNullOrBlank() -> ""
            receivingOrgSvc.isNullOrBlank() -> receivingOrg
            else -> Receiver.createFullName(receivingOrg, receivingOrgSvc)
        }
    }
}

/**
 * Class containing information fetched from the DB when getting delivery facilities
 *
 * @property testingLabName the full name of the facility
 * @property testingLabCity city of the facility
 * @property testingLabState state of the facility
 * @property testingLabClia The CLIA number (10-digit alphanumeric) of the facility
 * @property positive the result (conclusion) of the test. 0 = negative (good usually)
 * @property countRecords number of facilities included in the object
 */
data class DeliveryFacility(
    val testingLabName: String?,
    val testingLabCity: String?,
    val testingLabState: String?,
    val testingLabClia: String?,
    val positive: Long?,
    val countRecords: Long?
) {
    /**
     * This is a combination of the city and state values
     * for easier conversion into an output format
     */
    val location: String?
        get() {
            var loc = this.testingLabCity

            if (this.testingLabState != null) {
                loc = if (loc != null) {
                    loc + ", " + this.testingLabState
                } else {
                    this.testingLabState
                }
            }

            return loc
        }
}