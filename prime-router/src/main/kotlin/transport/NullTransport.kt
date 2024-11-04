package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.report.ReportService

/**
 * The Null transport is used for testing and benchmarking purposes or when a transport is not configured for a receiver.
 */
class NullTransport : ITransport {
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        externalFileName: String,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
        reportEventService: IReportStreamEventService,
        reportService: ReportService,
    ): RetryItems? {
        if (header.content == null) error("No content for report ${header.reportFile.reportId}")
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val msg = "Sending to Null Transport. File can be downloaded by Receiver until it expires."
        actionHistory.trackActionResult(msg)
        actionHistory.trackSentReport(
            receiver,
            sentReportId,
            externalFileName,
            transportType.toString(),
            msg,
            header,
            reportEventService,
            reportService,
            this::class.java.simpleName
        )
        actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
        return null
    }
}