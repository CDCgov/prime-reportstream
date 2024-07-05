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
data class ReportSentEvent(
    val rootReportId: List<ReportId>,
    val sentReportId: ReportId,
    val topic: Topic,
    val senderName: List<String>,
    val receiverName: String,
    val transportType: String?,
    val externalFilename: String,
) : AzureCustomEvent {

    constructor(
        receiver: Receiver,
        reportFiles: List<ReportFile>,
        reportId: ReportId,
        externalFilename: String,
    ) :
        this(
            reportFiles.map { it.reportId },
            reportId,
            receiver.topic,
            reportFiles.mapNotNull { Sender.createFullName(it.sendingOrg, it.sendingOrgClient) },
            receiver.fullName,
            receiver.transport?.type,
            externalFilename
        )
}