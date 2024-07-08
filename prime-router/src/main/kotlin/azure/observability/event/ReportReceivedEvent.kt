package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.ActionHistory

/**
 * Azure Event to capture successfully receiving a submission
 */
data class ReportReceivedEvent(
    val reportId: ReportId,
    val sender: Sender,
    val submissionDetails: ActionHistory.ReceivedReportSenderParameters,
    val senderIP: String,
    val fileSize: String,
) : AzureCustomEvent