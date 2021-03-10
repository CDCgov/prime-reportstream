package gov.cdc.prime.router.transport

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import org.apache.logging.log4j.kotlin.Logging
import java.io.Closeable

/**
 * The Null transport is intended for testing and benchmarking purposes.
 */
class NullTransport : ITransport, Logging {
    override fun startSession(receiver: Receiver): Closeable? {
        return null
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: Any?,
        actionHistory: ActionHistory,
    ): RetryItems? {
        if (header.content == null) error("No content for report ${header.reportFile.reportId}")
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val transportType = receiver.transport as NullTransport
        val fileName = Report.formExternalFilename(header)
        val msg = "Sending to Null Transport"
        actionHistory.trackActionResult(msg)
        actionHistory.trackSentReport(
            receiver,
            sentReportId,
            fileName,
            transportType.toString(),
            msg,
            header.reportFile.itemCount
        )
        actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
        return null
    }
}