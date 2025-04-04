package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.report.ReportService

interface ITransport {
    /**
     * Send the content on the specific transport. Return retry information, if needed. Null, if not.
     *
     * @param transportType the type of the transport (should always match the class)
     * @param header container of all info needed about report being sent.
     * @param sentReportId ID representing the report as sent externally.
     * @param retryItems the retry items from the last effort, if it was unsuccessful
     * @return null, if successful. RetryItems if not successful.
     */
    fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        externalFileName: String,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
        reportEventService: IReportStreamEventService,
        reportService: ReportService,
        lineages: List<ItemLineage>?,
    ): RetryItems?
}