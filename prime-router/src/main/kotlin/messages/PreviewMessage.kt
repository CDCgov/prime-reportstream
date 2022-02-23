package gov.cdc.prime.router.messages

import gov.cdc.prime.router.ActionLog
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

/**
 * The [PreviewResponseMessage] is the OK status payload returned by the `preview` API end-point.
 */
data class PreviewResponseMessage(
    val receiverName: String,
    val externalFileName: String,
    val content: String,
    val warnings: List<ActionLog> = emptyList()
)

/**
 * The [PreviewErrorMessage] is the BAD_REQUEST status payload returned by the `preview` API end-point.
 */
data class PreviewErrorMessage(
    val message: String,
    val errors: List<ActionLog> = emptyList(),
    val warnings: List<ActionLog> = emptyList()
)