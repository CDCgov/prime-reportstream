package gov.cdc.prime.router.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
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
 * @property receivingOrgSvcStatus the customer status of the organization's service that's receiving this submission
 * @property originalIngestion the report ID and ingestion/creation time of all root reports for this record
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(
    value = [
        "deliveryId", "batchReadyAt", "expires", "receiver", "receivingOrgSvcStatus",
        "reportId", "topic", "reportItemCount", "fileName", "fileType", "originalIngestion"
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
    val bodyFormat: String,
    val receivingOrgSvcStatus: String? = null,
    var originalIngestion: List<Map<String, Any>>? = null,
) : ReportHistory(
    actionId,
    createdAt,
    externalName,
    reportId,
    schema_topic,
    itemCount
) {

    companion object {
        fun createDeliveryHistoryFromReportAndAction(
            reportFile: ReportFile,
            action: Action,
        ): DeliveryHistory = DeliveryHistory(
                actionId = action.actionId,
                createdAt = action.createdAt,
                receivingOrg = reportFile.receivingOrg,
                receivingOrgSvc = reportFile.receivingOrgSvc,
                externalName = action.externalName,
                reportId = reportFile.reportId.toString(),
                schema_topic = reportFile.schemaTopic,
                itemCount = reportFile.itemCount,
                bodyUrl = reportFile.bodyUrl,
                schemaName = reportFile.schemaName,
                bodyFormat = reportFile.bodyFormat
            )
    }

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
            return this.bodyUrl?.substringAfter("%2F").orEmpty()
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
    val countRecords: Long?,
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