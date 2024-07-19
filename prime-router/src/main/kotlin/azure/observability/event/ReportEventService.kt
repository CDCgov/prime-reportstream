package gov.cdc.prime.router.azure.observability.event

import com.fasterxml.jackson.annotation.JsonUnwrapped
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.report.ReportService
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
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

abstract class AbstractReportStreamEventBuilder<T : AzureCustomEvent>(
    protected val reportEventService: ReportEventService,
    val azureEventService: AzureEventService,
    private val theName: ReportStreamEventName,
    private val childReportId: UUID,
    private val childBodyUrl: String,
    private val theTopic: Topic,
    private val pipelineStepName: TaskAction,
) : Logging {

    constructor(
        reportEventService: ReportEventService,
        azureEventService: AzureEventService,
        theName: ReportStreamEventName,
        report: ReportFile,
        pipelineStepName: TaskAction,
    ) : this(
        reportEventService,
        azureEventService,
        theName,
        report.reportId,
        report.bodyUrl,
        report.schemaTopic,
        pipelineStepName
    )

    constructor(
        reportEventService: ReportEventService,
        azureEventService: AzureEventService,
        theName: ReportStreamEventName,
        report: Report,
        pipelineStepName: TaskAction,
    ) : this(
        reportEventService,
        azureEventService,
        theName,
        report.id,
        report.bodyURL,
        report.schema.topic,
        pipelineStepName
    )
    var theParams: Map<ReportStreamEventProperties, Any> = emptyMap()
    var theParentReportId: UUID? = null

    companion object {

        fun <Event, T : AbstractReportStreamEventBuilder<Event>> buildEvent(
            reportEventService: ReportEventService,
            azureEventService: AzureEventService,
            eventBuilderClass: Class<T>,
            initializer: T.() -> Unit,
        ): T {
            val constructor =
                eventBuilderClass.getDeclaredConstructor(ReportEventService::class.java, AzureEventService::class.java)
            return constructor.newInstance(reportEventService, azureEventService).apply(initializer)
        }
    }

    fun parentReportId(parentReportId: UUID?) {
        theParentReportId = parentReportId
    }

    fun params(params: Map<ReportStreamEventProperties, Any>) {
        theParams = params
    }

    abstract fun buildEvent(): T

    fun getReportEventData(): ReportEventData {
        return reportEventService.getReportEventData(
            childReportId,
            childBodyUrl,
            theParentReportId,
            pipelineStepName,
            theTopic
        )
    }

    fun sendToAzure(): AbstractReportStreamEventBuilder<T> {
        val event = buildEvent()
        azureEventService.trackEvent(theName, event)
        return this
    }

    fun logEvent(): AbstractReportStreamEventBuilder<T> {
        val event = buildEvent()
        withLoggingContext(event) {
            logger.info("$theName event occurred")
        }
        return this
    }
}

fun sendToActionLog(@Suppress("UNUSED_PARAMETER") actionHistory: ActionHistory) {
    throw NotImplementedError()
}

class ReportStreamReportEventBuilder(
    reportEventService: ReportEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic,
    pipelineStepName: TaskAction,
) : AbstractReportStreamEventBuilder<ReportStreamReportEvent>(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName
) {

    override fun buildEvent(): ReportStreamReportEvent {
        return ReportStreamReportEvent(
            getReportEventData(),
            theParams
        )
    }
}

open class ReportStreamItemEventBuilder(
    reportEventService: ReportEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic,
    pipelineStepName: TaskAction,
) : AbstractReportStreamEventBuilder<ReportStreamItemEvent>(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName
) {
    var theParentReportIndex = 1
    var theChildIndex = 1
    var theTrackingId: String? = null

    fun trackingId(bundle: Bundle) {
        theTrackingId = AzureEventUtils.getIdentifier(bundle).value
    }

    protected fun getItemEventData(): ItemEventData {
        if (theParentReportId == null) {
            throw IllegalStateException("Parent Report ID must be set to generate an ItemEvent")
        }
        return reportEventService.getItemEventData(
            theChildIndex,
            theParentReportId!!,
            theParentReportIndex,
            theTrackingId
        )
    }

    override fun buildEvent(): ReportStreamItemEvent {
        return ReportStreamItemEvent(
            getReportEventData(),
            getItemEventData(),
            theParams
        )
    }
}

class ReportStreamItemProcessingErrorEventBuilder(
    reportEventService: ReportEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic,
    pipelineStepName: TaskAction,
    private val error: String,
) : ReportStreamItemEventBuilder(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName
) {
    override fun buildEvent(): ReportStreamItemEvent {
        return ReportStreamItemEvent(
            getReportEventData(),
            getItemEventData(),
            theParams + mapOf(ReportStreamEventProperties.PROCESSING_ERROR to error)
        )
    }
}

class ReportEventService(
    private val reportService: ReportService = ReportService(),
    private val dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    private val azureEventService: AzureEventService,
) {

    fun createReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ): ReportStreamReportEventBuilder {
        return ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName
        ).apply(
            initializer
        )
    }

    fun createReportEvent(
        eventName: ReportStreamEventName,
        report: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ): ReportStreamReportEventBuilder {
        return ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            report.reportId,
            report.bodyUrl,
            report.schemaTopic,
            pipelineStepName
        ).apply(
            initializer
        )
    }

    fun createItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ): AbstractReportStreamEventBuilder<ReportStreamItemEvent> {
        return ReportStreamItemEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName
        ).apply(initializer)
    }

    fun createItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ): AbstractReportStreamEventBuilder<ReportStreamItemEvent> {
        return ReportStreamItemEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName
        ).apply(initializer)
    }

    fun createItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ): ReportStreamItemProcessingErrorEventBuilder {
        return ReportStreamItemProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            error
        ).apply(initializer)
    }

    fun createItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ): ReportStreamItemProcessingErrorEventBuilder {
        return ReportStreamItemProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            error
        ).apply(initializer)
    }

    fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic,
    ): ReportEventData {
        val submittedReportIds = if (parentReportId != null) {
            reportService.getRootReports(parentReportId)
        } else {
            emptyList()
        }.map { it.reportId }

        return ReportEventData(
            childReportId,
            parentReportId,
            submittedReportIds,
            topic,
            childBodyUrl,
            pipelineStepName,
            OffsetDateTime.now()
        )
    }

    fun getItemEventData(
        childItemIndex: Int,
        parentReportId: UUID,
        parentItemIndex: Int,
        trackingId: String?,
    ): ItemEventData {
        val submittedIndex = reportService.getRootItemIndex(parentReportId, parentItemIndex)
        val rootReport =
            reportService.getRootReports(parentReportId).firstOrNull() ?: dbAccess.fetchReportFile(parentReportId)
        return ItemEventData(
            childItemIndex,
            parentItemIndex,
            submittedIndex,
            trackingId,
            "${rootReport.sendingOrg}.${rootReport.sendingOrgClient}"
        )
    }
}