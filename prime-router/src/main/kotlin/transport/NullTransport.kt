package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.observability.event.ReportEventService

/**
 * The Null transport is intended for testing and benchmarking purposes.
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
        reportEventService: ReportEventService,
    ): RetryItems? {
        if (header.content == null) error("No content for report ${header.reportFile.reportId}")
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val msg = "Sending to Null Transport"
        actionHistory.trackActionResult(msg)
        actionHistory.trackSentReport(
            receiver,
            sentReportId,
            externalFileName,
            transportType.toString(),
            msg,
            header,
            reportEventService,
            this::class.java.simpleName
        )
        actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
        return null
    }
}