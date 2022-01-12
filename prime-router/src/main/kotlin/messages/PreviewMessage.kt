package gov.cdc.prime.router.messages

import gov.cdc.prime.router.DeepOrganization

/**
 * The [PreviewMessage] is the payload for the `preview` API end-point.
 * It contains an input report and settings.
 */
data class PreviewMessage(
    val senderName: String,
    val sender: SenderMessage? = null,
    val receiverName: String,
    val receiver: ReceiverMessage? = null,
    val deepOrganizations: List<DeepOrganization>? = null,
    val inputContent: String,
)