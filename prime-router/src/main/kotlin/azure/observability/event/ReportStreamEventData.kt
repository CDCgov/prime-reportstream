package gov.cdc.prime.router.azure.observability.event

import com.fasterxml.jackson.annotation.JsonUnwrapped
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.util.UUID

data class ReportEventData(
    val childReportId: UUID,
    val parentReportId: UUID?,
    val submittedReportIds: List<UUID>,
    val topic: Topic?,
    val blobUrl: String,
    val pipelineStepName: TaskAction,
    val timestamp: OffsetDateTime,
)

data class ItemEventData(
    val childItemIndex: Int,
    val parentItemIndex: Int?,
    val submittedItemIndex: Int?,
    val trackingId: String?,
    val sender: String,
)

enum class ReportStreamEventProperties {
    PROCESSING_ERROR,
    ITEM_FORMAT,
    VALIDATION_PROFILE,
    FAILING_FILTERS,
    FILTER_TYPE,
    FILENAME,
    TRANSPORT_TYPE,
    RECEIVER_NAME,
    REQUEST_PARAMETERS,
    SENDER_IP,
    FILE_LENGTH,
    SENDER_NAME,
    BUNDLE_DIGEST,
}

enum class ReportStreamEventName {
    ITEM_NOT_ROUTED,
    ITEM_FAILED_VALIDATION,
    ITEM_ACCEPTED,
    ITEM_FILTER_FAILED,
    REPORT_SENT,
    REPORT_RECEIVED_EVENT,
    ITEM_ROUTED,
    REPORT_LAST_MILE_FAILURE,
}

data class ReportStreamReportEvent(
    @JsonUnwrapped
    val reportEventData: ReportEventData,
    @JsonUnwrapped
    val params: Map<ReportStreamEventProperties, Any>,
) : AzureCustomEvent

data class ReportStreamItemEvent(
    @JsonUnwrapped
    val reportEventData: ReportEventData,
    @JsonUnwrapped
    val itemEventData: ItemEventData,
    @JsonUnwrapped
    val params: Map<ReportStreamEventProperties, Any>,
) : AzureCustomEvent