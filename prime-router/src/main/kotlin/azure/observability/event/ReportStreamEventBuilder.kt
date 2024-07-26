package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.context.withLoggingContext
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
import java.util.UUID

abstract class AbstractReportStreamEventBuilder<T : AzureCustomEvent>(
    protected val reportEventService: IReportEventService,
    val azureEventService: AzureEventService,
    private val theName: ReportStreamEventName,
    private val childReportId: UUID,
    private val childBodyUrl: String,
    private val theTopic: Topic,
    private val pipelineStepName: TaskAction,
) : Logging {

    constructor(
        reportEventService: IReportEventService,
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
        reportEventService: IReportEventService,
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

    fun send() {
        val event = buildEvent()
        sendToAzure(event)
        logEvent(event)
    }

    private fun sendToAzure(event: T): AbstractReportStreamEventBuilder<T> {
        azureEventService.trackEvent(theName, event)
        return this
    }

    private fun logEvent(event: T): AbstractReportStreamEventBuilder<T> {
        withLoggingContext(event) {
            logger.info("$theName event occurred")
        }
        return this
    }
}

open class ReportStreamReportEventBuilder(
    reportEventService: IReportEventService,
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
    reportEventService: IReportEventService,
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
    var theParentITemIndex = 1
    var theChildIndex = 1
    var theTrackingId: String? = null

    fun trackingId(bundle: Bundle) {
        theTrackingId = AzureEventUtils.getIdentifier(bundle).value
    }

    fun parentItemIndex(parentItemIndex: Int) {
        theParentITemIndex = parentItemIndex
    }

    protected fun getItemEventData(): ItemEventData {
        if (theParentReportId == null) {
            throw IllegalStateException("Parent Report ID must be set to generate an ItemEvent")
        }
        return reportEventService.getItemEventData(
            theChildIndex,
            theParentReportId!!,
            theParentITemIndex,
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

class ReportStreamReportProcessingErrorEventBuilder(
    reportEventService: IReportEventService,
    azureEventService: AzureEventService,
    theName: ReportStreamEventName,
    childReportId: UUID,
    childBodyUrl: String,
    theTopic: Topic,
    pipelineStepName: TaskAction,
    private val error: String,
) : ReportStreamReportEventBuilder(
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
            theParams + mapOf(ReportStreamEventProperties.PROCESSING_ERROR to error)
        )
    }
}

class ReportStreamItemProcessingErrorEventBuilder(
    reportEventService: IReportEventService,
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