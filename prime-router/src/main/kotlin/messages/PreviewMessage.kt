package gov.cdc.prime.router.messages

import gov.cdc.prime.router.DeepOrganization

/**
 * The [PreviewMessage] is the payload for the `preview` API end-point.
 * It contains an input report and settings.
 *
 * TODO: generate this message from OpenAPI
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
 * The response message returned by the `preview` API end-point
 *
 * TODO: generate this message from OpenAPI
 */
sealed interface PreviewResponseMessage {
    /**
     * The [Success] is the OK status payload.
     */
    data class Success(
        val receiverName: String,
        val externalFileName: String,
        val content: String,
        val warnings: List<String> = emptyList()
    ) : PreviewResponseMessage

    /**
     * The [PreviewErrorMessage] is the BAD_REQUEST status payload returned by the `preview` API end-point.
     */
    data class Error(
        val message: String,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    ) : PreviewResponseMessage
}