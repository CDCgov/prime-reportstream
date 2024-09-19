package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.reportstream.shared.Topic
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.report.ReportService
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This service is responsible for creating and sending ReportStream business events.
 *
 * The service will generate two different kinds of event
 * - report event: this will be events about what is submitted or delivered to ReportStream
 * - item event: this will be events about a specific result, order, etc. A submitted or delivered report may have
 * multiple items
 *
 */
interface IReportStreamEventService {

    /**
     * Creates a report event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    )

    /**
     * Creates a report event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    )

    /**
     * Creates a report processing error event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates a report processing error event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates a general processing error event. This is not associated with a report or item.
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param submissionId the report id for the incoming report
     * @param bodyUrl the blob url for the incoming report
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendSubmissionProcessingError(
        eventName: ReportStreamEventName,
        pipelineStepName: TaskAction,
        error: String,
        submissionId: ReportId,
        bodyUrl: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates an item event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    )

    /**
     * Creates an item event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    )

    /**
     * Creates an item processing error event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates an item processing error event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Retrieves data about the input and output report for a particular pipeline step
     *
     * @param childReportId the id of the ReportFile
     * @param childBodyUrl the blob URL for the output report
     * @param parentReportId the optional parent report id.  A report outputted from the ReportFunction will not have a parent
     * @param pipelineStepName the pipeline step that is generated the child report
     * @param topic the [Topic] that the report is in
     * @return [ReportEventData]
     */
    fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic?,
    ): ReportEventData

    /**
     * Retrieves data about a specific "item".  Please note that item does not have a dedicated representation in the
     * database and the data here represents the concept.
     *
     * @param childItemIndex the index of this item in the outputted report
     * @param parentReportId the parent report id
     * @param parentItemIndex the index of this item in the parent report
     * @param trackingId an optional value that is a unique value for the item as it is processed by the pipeline
     * @return [ItemEventData]
     */
    fun getItemEventData(
        childItemIndex: Int,
        parentReportId: UUID,
        parentItemIndex: Int,
        trackingId: String?,
    ): ItemEventData
}

/**
 * Concrete implementation of [IReportStreamEventService].  This implementation is configured to send the event
 * to azure application insights as an event as well as logging the vent.
 *
 */
class ReportStreamEventService(
    private val dbAccess: DatabaseAccess,
    private val azureEventService: AzureEventService,
    private val reportService: ReportService,
) : IReportStreamEventService {

    override fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ) {
        ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName
        ).apply(
            initializer
        ).send()
    }

    override fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ) {
        ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName
        ).apply(
            initializer
        ).send()
    }

    override fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ) {
        ReportStreamReportProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            error
        ).apply(
            initializer
        ).send()
    }

    override fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ) {
        ReportStreamReportProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            error
        ).apply(
            initializer
        ).send()
    }

    override fun sendSubmissionProcessingError(
        eventName: ReportStreamEventName,
        pipelineStepName: TaskAction,
        error: String,
        submissionId: ReportId,
        bodyUrl: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ) {
        ReportStreamReportProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            submissionId,
            bodyUrl,
            theTopic = null,
            pipelineStepName,
            error
        ).apply(
            initializer
        ).send()
    }

    override fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ) {
        ReportStreamItemEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName
        ).apply(initializer).send()
    }

    override fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ) {
        ReportStreamItemEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName
        ).apply(initializer).send()
    }

    override fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ) {
        ReportStreamItemProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            error
        ).apply(initializer).send()
    }

    override fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ) {
        ReportStreamItemProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            error
        ).apply(initializer).send()
    }

    override fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic?,
    ): ReportEventData {
        val submittedReportIds = if (parentReportId != null) {
            val rootReports = reportService.getRootReports(parentReportId)
            rootReports.ifEmpty {
                listOf(dbAccess.fetchReportFile(parentReportId))
            }
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

    override fun getItemEventData(
        childItemIndex: Int,
        parentReportId: UUID,
        parentItemIndex: Int,
        trackingId: String?,
    ): ItemEventData {
        val submittedIndex = reportService.getRootItemIndex(parentReportId, parentItemIndex) ?: parentItemIndex
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