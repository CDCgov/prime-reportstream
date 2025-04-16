package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
import java.util.UUID

/**
 * Abstract class for building ReportStream events
 *
 * @param reportEventService service used to fetch more details for the event
 * @param azureEventService service used to dispatch the event to azure
 * @param name the [ReportStreamEventName] of the event
 * @param childReportId the report id of the outputted report/item
 * @param childBodyUrl the blob url of the outputted report
 * @param theTopic the topic that the report/item is part of
 * @param pipelineStepName the pipeline step producing the event
 *
 * Additional properties can be set for the event via an initializer
 * - [AbstractReportStreamEventBuilder.parentReportId] will configure the id of the inputted report
 * - [AbstractReportStreamEventBuilder.params] will configure additional properties to include with the event
 *
 * Events can be built and then delivered by invoking [AbstractReportStreamEventBuilder.send]
 *
 */
abstract class AbstractReportStreamEventBuilder<T : AzureCustomEvent>(
    protected val reportEventService: IReportStreamEventService,
    val azureEventService: AzureEventService,
    private val name: ReportStreamEventName,
    private val childReportId: UUID,
    private val childBodyUrl: String,
    private val theTopic: Topic?,
    private val pipelineStepName: TaskAction,
    protected val rootReports: List<ReportFile>?,
) : Logging {

    constructor(
        reportEventService: IReportStreamEventService,
        azureEventService: AzureEventService,
        theName: ReportStreamEventName,
        report: ReportFile,
        pipelineStepName: TaskAction,
        rootReports: List<ReportFile>?,
    ) : this(
        reportEventService,
        azureEventService,
        theName,
        report.reportId,
        report.bodyUrl,
        report.schemaTopic,
        pipelineStepName,
        rootReports
    )

    constructor(
        reportEventService: IReportStreamEventService,
        azureEventService: AzureEventService,
        theName: ReportStreamEventName,
        report: Report,
        pipelineStepName: TaskAction,
        rootReports: List<ReportFile>?,
    ) : this(
        reportEventService,
        azureEventService,
        theName,
        report.id,
        report.bodyURL,
        report.schema.topic,
        pipelineStepName,
        rootReports
    )
    var theParams: Map<ReportStreamEventProperties, Any> = emptyMap()
    var theParentReportId: UUID? = null

    fun parentReportId(parentReportId: UUID?) {
        theParentReportId = parentReportId
    }

    fun params(params: Map<ReportStreamEventProperties, Any>) {
        theParams = params
    }

    abstract fun buildEvent(): T

    fun getReportEventData(): ReportEventData = reportEventService.getReportEventData(
            childReportId,
            childBodyUrl,
            theParentReportId,
            pipelineStepName,
            theTopic,
            rootReports
        )

    fun send() {
            val event = buildEvent()
            sendToAzure(event)
            logEvent(event)
    }

    private fun sendToAzure(event: T): AbstractReportStreamEventBuilder<T> {
        azureEventService.trackEvent(name, event)
        return this
    }

    private fun logEvent(event: T): AbstractReportStreamEventBuilder<T> {
        withLoggingContext(event) {
            logger.info("$name event occurred")
        }
        return this
    }
}

/**
 * Concrete implementation for building ReportStream report event
 */
open class ReportStreamReportEventBuilder(
    reportEventService: IReportStreamEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic?,
    pipelineStepName: TaskAction,
    rootReports: List<ReportFile>?,
) : AbstractReportStreamEventBuilder<ReportStreamReportEvent>(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName,
    rootReports
) {

    override fun buildEvent(): ReportStreamReportEvent = ReportStreamReportEvent(
            getReportEventData(),
            theParams
        )
}

/**
 * Concrete implementation for creating a ReportStream item event
 *
 * Supports some additional configuration:
 * - [ReportStreamItemEventBuilder.childItemIndex] will set the index for the item in outputted report, defaults to 1
 * - [ReportStreamItemEventBuilder.parentItemIndex] will set the index for the item in the inputted report, defaults to 1
 * - [ReportStreamItemEventBuilder.trackingId] sets a unique identifier for the item
 */
open class ReportStreamItemEventBuilder(
    reportEventService: IReportStreamEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic,
    pipelineStepName: TaskAction,
    rootReports: List<ReportFile>?,
) : AbstractReportStreamEventBuilder<ReportStreamItemEvent>(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName,
    rootReports
) {
    private var theParentItemIndex = 1
    private var theChildIndex = 1
    private var theTrackingId: String? = null

    fun trackingId(bundle: Bundle) {
        theTrackingId = AzureEventUtils.getIdentifier(bundle).value
    }

    fun parentItemIndex(parentItemIndex: Int) {
        theParentItemIndex = parentItemIndex
    }

    fun childItemIndex(childItemIndex: Int) {
        theChildIndex = childItemIndex
    }

    protected fun getItemEventData(): ItemEventData {
        if (theParentReportId == null) {
            throw IllegalStateException("Parent Report ID must be set to generate an ItemEvent")
        }
        return reportEventService.getItemEventData(
            theChildIndex,
            theParentReportId!!,
            theParentItemIndex,
            theTrackingId,
            rootReports
        )
    }

    override fun buildEvent(): ReportStreamItemEvent = ReportStreamItemEvent(
            getReportEventData(),
            getItemEventData(),
            theParams
        )
}

/**
 * Subclass of [ReportStreamReportEventBuilder] that forces the caller to pass an error string
 */
class ReportStreamReportProcessingErrorEventBuilder(
    reportEventService: IReportStreamEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic?,
    pipelineStepName: TaskAction,
    rootReports: List<ReportFile>?,
    private val error: String,
) : ReportStreamReportEventBuilder(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName,
    rootReports
) {
    override fun buildEvent(): ReportStreamReportEvent = ReportStreamReportEvent(
            getReportEventData(),
            theParams + mapOf(ReportStreamEventProperties.PROCESSING_ERROR to error)
        )
}

/**
 * Subclass of [ReportStreamItemEventBuilder] that forces the caller to pass an error string
 */
class ReportStreamItemProcessingErrorEventBuilder(
    reportEventService: IReportStreamEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic,
    pipelineStepName: TaskAction,
    rootReports: List<ReportFile>?,
    private val error: String,
) : ReportStreamItemEventBuilder(
    reportEventService,
    azureEventService,
    theName,
    childReportId,
    childBodyUrl,
    theTopic,
    pipelineStepName,
    rootReports
) {
    override fun buildEvent(): ReportStreamItemEvent = ReportStreamItemEvent(
            getReportEventData(),
            getItemEventData(),
            theParams + mapOf(ReportStreamEventProperties.PROCESSING_ERROR to error)
        )
}