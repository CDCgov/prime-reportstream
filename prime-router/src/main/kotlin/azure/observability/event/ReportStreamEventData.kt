package gov.cdc.prime.router.azure.observability.event

import com.fasterxml.jackson.annotation.JsonKey
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.google.common.base.CaseFormat
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Data class that is used in all ReportStream report events
 *
 * @param childReportId the report id of the outputted report
 * @param parentReportId the optional report if of the inputted report
 * @param submittedReportIds all the submitted reports that the outputted report ids has items from.
 * @param topic the [Topic] that the report is part of
 * @param blobUrl the blob url for the outputted report
 * @param pipelineStepName the step that produced the outputted report
 * @param timestamp when the event occurred
 */
data class ReportEventData(
    val childReportId: UUID,
    val parentReportId: UUID?,
    val submittedReportIds: List<UUID>,
    val topic: Topic?,
    val blobUrl: String,
    val pipelineStepName: TaskAction,
    val timestamp: OffsetDateTime,
    val commitId: String,
    val queueMessage: QueueMessage,
)

/**
 * Data class is used in all ReportStream item events
 *
 * @param childItemIndex the index of the outputted item in the child report
 * @param parentItemIndex the index the item in the input report
 * @param submittedItemIndex the index of the item in the submitted report
 * @param trackingId a unique identifier for the item as it goes through the pipeline
 * @param sender the sender of the item
 */
data class ItemEventData(
    val childItemIndex: Int,
    val parentItemIndex: Int?,
    val submittedItemIndex: Int?,
    val trackingId: String?,
    val sender: String,
)

/**
 * This enum contains properties values that can be used in creating params for ReportStream events
 * @see [AbstractReportStreamEventBuilder.params]
 */
enum class ReportStreamEventProperties {
    PROCESSING_ERROR,
    ITEM_FORMAT,
    ITEM_COUNT,
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
    INGESTION_TYPE,
    POISON_QUEUE_MESSAGE_ID,
    ENRICHMENTS,
    ORIGINAL_FORMAT,
    TARGET_FORMAT,
    RETRY_COUNT,
    NEXT_RETRY_TIME,
    QUEUE_MESSAGE,
    ;

    @JsonKey
    fun externalKey(): String = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name)
}

/**
 * The business event names
 *
 */
enum class ReportStreamEventName {
    ITEM_NOT_ROUTED,
    ITEM_FAILED_VALIDATION,
    ITEM_ACCEPTED,
    ITEM_FILTER_FAILED,
    REPORT_SENT,
    REPORT_RECEIVED,
    REPORT_NOT_RECEIVABLE,
    ITEM_ROUTED,
    REPORT_LAST_MILE_FAILURE,
    REPORT_NOT_PROCESSABLE,
    ITEM_SENT,
    PIPELINE_EXCEPTION,
    ITEM_TRANSFORMED,
    ITEM_LAST_MILE_FAILURE,
    ITEM_SEND_ATTEMPT_FAIL,
}

/**
 * A ReportStream report event that can be sent to azure
 *
 * @param reportEventData the [ReportEventData]
 * @param params additional properties that should be included with the event
 */
data class ReportStreamReportEvent(
    @JsonUnwrapped
    val reportEventData: ReportEventData,
    @JsonUnwrapped
    val params: Map<ReportStreamEventProperties, Any>,
) : AzureCustomEvent

/**
 * A ReportStream item event that can be sent to azure
 *
 * @param reportEventData the [ReportEventData]
 * @param itemEventData the [ItemEventData]
 * @param params additional properties that should be included with the event
 */
data class ReportStreamItemEvent(
    @JsonUnwrapped
    val reportEventData: ReportEventData,
    @JsonUnwrapped
    val itemEventData: ItemEventData,
    @JsonUnwrapped
    val params: Map<ReportStreamEventProperties, Any>,
) : AzureCustomEvent