package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * This class handles ReportFileHistory for Deliveries for a receiver.
 *
 * @property actionId reference to the `action` table for the action that created this file
 * @property createdAt when the file was created
 * @property sendingOrg the name of the organization that sent this submission
 * @property httpStatus response code for the user fetching this report file
 * @property externalName actual filename of the file
 * @property reportId unique identifier for this specific report file
 * @property schemaTopic the kind of data contained in the report (e.g. "covid-19")
 * @property itemCount number of tests (data rows) contained in the report
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DeliveryHistory(
    @JsonProperty("deliveryId")
    actionId: Long,
    @JsonProperty("timestamp")
    createdAt: OffsetDateTime,
    @JsonProperty("sender")
    val sendingOrg: String,
    httpStatus: Int,
    @JsonInclude(Include.NON_NULL)
    externalName: String? = "",
    @JsonProperty("id")
    reportId: String? = null,
    @JsonProperty("topic")
    schemaTopic: String? = null,
    @JsonProperty("reportItemCount")
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