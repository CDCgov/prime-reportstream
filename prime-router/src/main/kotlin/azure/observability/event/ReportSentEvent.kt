package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile

/**
 * Event definition for when a report has been successfully sent to a receiver.
 * [trasnportType] is nullable as some receivers will purposefully not have transports configured
 */

class ReportSentEvent(
    val defaultReceiver: Receiver,
    val defaultReportFile: ReportFile,
    val defaultReportId: ReportId,
    val defaultExternalFilename: String,
) {
    data class CustomReportSentEvent(
        val sentReportId: ReportId,
        val reportId: ReportId,
        val topic: Topic,
        val sender: String,
        val receiverName: String,
        val transportType: String?,
        val externalFilename: String,
    ) : AzureCustomEvent

    fun createEvent(
        receiver: Receiver = defaultReceiver,
        reportFile: ReportFile = defaultReportFile,
        reportId: ReportId = defaultReportId,
        externalFilename: String = defaultExternalFilename,
    ): CustomReportSentEvent {
        return CustomReportSentEvent(
            reportFile.reportId,
            reportId,
            receiver.topic,
            Sender.createFullName(reportFile.sendingOrg, reportFile.sendingOrgClient),
            receiver.fullName,
            receiver.transport?.type,
            externalFilename
        )
    }
}