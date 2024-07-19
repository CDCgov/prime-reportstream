package gov.cdc.prime.router.azure.observability.event

import com.fasterxml.jackson.annotation.JsonUnwrapped
import gov.cdc.prime.router.azure.ActionHistory

/**
 * Azure Event to capture successfully receiving a submission
 */
data class ReportReceivedEvent(
    override val reportEventData: ReportEventData,
    val sender: String,
    @JsonUnwrapped
    val submissionDetails: ActionHistory.ReceivedReportSenderParameters,
    val senderIP: String,
    val fileSize: String,
) : AzureCustomEvent, IReportEvent