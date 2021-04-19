package gov.cdc.prime.router.transport

import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.apache.logging.log4j.kotlin.Logging

class BlobStoreTransport : ITransport, Logging {
    override fun startSession(receiver: Receiver): TransportSession? {
        return null
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: TransportSession?,
        actionHistory: ActionHistory
    ): RetryItems? {
        val blobTransportType = header.receiver?.transport as BlobStoreTransportType
        val envVar: String = blobTransportType.containerName
        val storageName: String = blobTransportType.storageName
        return try {
            val receiver = header.receiver
            val bodyUrl = header.reportFile.bodyUrl ?: error("Report ${header.reportFile.reportId} has no blob to copy")
            logger.info("About to copy $bodyUrl to $envVar:$storageName")
            val newUrl = WorkflowEngine().blob.copyBlob(bodyUrl,envVar, storageName)
            val msg = "Successfully copied $bodyUrl to $newUrl"
            logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                newUrl,
                blobTransportType.toString(),
                msg,
                header.reportFile.itemCount
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (t: Throwable) {
            val msg =
                "FAILED Blob copy of inputReportId ${header.reportFile.reportId} to " +
                    "$blobTransportType ($envVar:$storageName)" +
                    ", Exception: ${t.localizedMessage}"
            logger.warn(msg, t)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }
}