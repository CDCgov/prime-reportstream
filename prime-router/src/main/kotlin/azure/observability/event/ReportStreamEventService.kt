package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.version.Version
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
     * Sends any events that have been queued up if the client specified sending them can be deferred.
     *
     * This is useful in contexts where the events should only be sent after all the business logic has
     * executed and the DB transaction has been committed.
     */
    fun sendQueuedEvents()

    /**
     * Creates a report event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param queueMessage the original azure queue message
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    )

    /**
     * Creates a report event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param queueMessage the original azure queue message
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    )

    /**
     * Creates a report processing error event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param queueMessage the azure queue message associated with this event
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates a report processing error event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param queueMessage the azure queue message associated with this event
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates an item event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param queueMessage the azure queue message associated with this event
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    )

    /**
     * Creates an item event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param queueMessage the azure queue message associated with this event
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    )

    /**
     * Creates an item processing error event from an [ReportFile]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param queueMessage the original azure queue message
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    )

    /**
     * Creates an item processing error event from an [Report]
     *
     * @param eventName the business event value from [ReportStreamEventName]
     * @param childReport the report that is getting emitted from the pipeline step
     * @param pipelineStepName the pipeline step that is emitting the event
     * @param error the error description
     * @param shouldQueue whether to send the event immediately or defer it to be sent later
     * @param queueMessage the original azure queue message
     * @param initializer additional data to initialize the creation of the event. See [AbstractReportStreamEventBuilder]
     */
    fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String = "",
        shouldQueue: Boolean = false,
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
     * @param queueMessage the azure queue message associated with this event
     * @return [ReportEventData]
     */
    fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic?,
        queueMessage: String = "",
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

    /**
     * Retrieves submission data relevant to items and reports.
     *
     * @param childItemIndex the index of this item in the outputted report
     * @param parentReportId the parent report id
     * @param isItemEvent flags if the data being retrieved is for a specific item or an entire report.
     * @return [SubmissionEventData]
     */
    fun getSubmissionEventData(
        childItemIndex: Int,
        parentReportId: UUID?,
        isItemEvent: Boolean = false,
    ): SubmissionEventData
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

    private val builtEvents = mutableListOf<AbstractReportStreamEventBuilder<*>>()

    override fun sendQueuedEvents() {
        builtEvents.forEach {
            it.send()
        }
        builtEvents.clear()
    }

    override fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            queueMessage
        ).apply(
            initializer
        )
        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            queueMessage
        ).apply(
            initializer
        )

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamReportProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            queueMessage,
            error,
        ).apply(
            initializer
        )

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamReportProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            queueMessage,
            error
        ).apply(
            initializer
        )

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamItemEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            queueMessage
        ).apply(initializer)

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamItemEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            queueMessage
        ).apply(initializer)

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamItemProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.reportId,
            childReport.bodyUrl,
            childReport.schemaTopic,
            pipelineStepName,
            queueMessage,
            error
        ).apply(initializer)

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ) {
        val builder = ReportStreamItemProcessingErrorEventBuilder(
            this,
            azureEventService,
            eventName,
            childReport.id,
            childReport.bodyURL,
            childReport.schema.topic,
            pipelineStepName,
            queueMessage,
            error
        ).apply(initializer)

        if (shouldQueue) {
            builtEvents.add(builder)
        } else {
            builder.send()
        }
    }

    override fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic?,
        queueMessage: String,
    ): ReportEventData = ReportEventData(
        childReportId,
        parentReportId,
        topic,
        childBodyUrl,
        pipelineStepName,
        OffsetDateTime.now(),
        Version.commitId,
        queueMessage
    )

    override fun getItemEventData(
        childItemIndex: Int,
        parentReportId: UUID,
        parentItemIndex: Int,
        trackingId: String?,
    ): ItemEventData {
        val submittedIndex = reportService.getRootItemIndex(parentReportId, parentItemIndex) ?: parentItemIndex

        return ItemEventData(
            childItemIndex,
            parentItemIndex,
            submittedIndex,
            trackingId
        )
    }

    override fun getSubmissionEventData(
        childItemIndex: Int,
        parentReportId: UUID?,
        isItemEvent: Boolean,
    ): SubmissionEventData {
        val submittedReports = if (!isItemEvent && parentReportId != null) {
            reportService.getRootReports(parentReportId)
        } else if (isItemEvent && parentReportId != null) {
            val rootReport = reportService.getReportForItemAtTask(parentReportId, childItemIndex, TaskAction.receive)
            if (rootReport == null) {
                // When invoked from the Convert step, the parent report is the submitted report.
                listOf(dbAccess.fetchReportFile(parentReportId))
            } else {
                listOf(rootReport)
            }
        } else {
            // When invoked from the Receive step, the parent report id will be null.
            emptyList()
        }

        val submittedReportIds = submittedReports
            .map { it.reportId }
            .ifEmpty { if (parentReportId != null) listOf(parentReportId) else emptyList() }
        val submittedReportSenders = submittedReports.joinToString(separator = ", ", prefix = "[", postfix = "]") {
            "\"${it.sendingOrg}.${it.sendingOrgClient}\""
        }

        return SubmissionEventData(submittedReportIds as List<UUID>, submittedReportSenders)
    }
}