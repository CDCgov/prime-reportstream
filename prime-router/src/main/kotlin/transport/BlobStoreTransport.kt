package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction

class BlobStoreTransport() : ITransport {
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val blobTransportType = transportType as BlobStoreTransportType
        val envVar: String = blobTransportType.containerName
        val storageName: String = blobTransportType.storageName
        return try {
            val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
            val bodyUrl = header.reportFile.bodyUrl ?: error("Report ${header.reportFile.reportId} has no blob to copy")
            context.logger.info("About to copy $bodyUrl to $envVar:$storageName")
            var newUrl = BlobAccess.copyBlob(bodyUrl, envVar, storageName)
            val msg = "Successfully copied $bodyUrl to $newUrl"
            context.logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                newUrl,
                blobTransportType.toString(),
                msg,
                header
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (t: Throwable) {
            val msg =
                "FAILED Blob copy of inputReportId ${header.reportFile.reportId} to " +
                    "$blobTransportType ($envVar:$storageName)" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.warning(msg)
            actionHistory.setActionType(TaskAction.send_warning)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }
}