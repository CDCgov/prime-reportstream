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
    val receiver: Receiver,
    val reportFile: ReportFile,
    val reportId: ReportId,
    val externalFilename: String,
) {
    fun createEvent(
        receiver: Receiver = this.receiver,
        reportFile: ReportFile = this.reportFile,
        reportId: ReportId = this.reportId,
        externalFilename: String = this.externalFilename,
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

data class CustomReportSentEvent(
    val sentReportId: ReportId,
    val reportId: ReportId,
    val topic: Topic,
    val sender: String,
    val receiverName: String,
    val transportType: String?,
    val externalFilename: String,
) : AzureCustomEvent