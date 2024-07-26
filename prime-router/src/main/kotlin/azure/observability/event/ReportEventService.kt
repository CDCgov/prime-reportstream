package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.report.ReportService
import java.time.OffsetDateTime
import java.util.UUID

interface IReportEventService {
    fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    )

    fun sendReportEvent(
        eventName: ReportStreamEventName,
        report: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    )

    fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    )

    fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    )

    fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    )

    fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    )

    fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    )

    fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic,
    ): ReportEventData

    fun getItemEventData(
        childItemIndex: Int,
        parentReportId: UUID,
        parentItemIndex: Int,
        trackingId: String?,
    ): ItemEventData
}

class ReportEventService(
    private val dbAccess: DatabaseAccess,
    private val azureEventService: AzureEventService,
    private val reportService: ReportService,
) : IReportEventService {

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
        report: ReportFile,
        pipelineStepName: TaskAction,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ) {
        ReportStreamReportEventBuilder(
            this,
            azureEventService,
            eventName,
            report.reportId,
            report.bodyUrl,
            report.schemaTopic,
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
        topic: Topic,
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