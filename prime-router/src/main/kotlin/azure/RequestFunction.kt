package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.DEFAULT_SEPARATOR
import gov.cdc.prime.router.HasSchema
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.ROUTE_TO_SEPARATOR
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender

const val CLIENT_PARAMETER = "client"
const val PAYLOAD_NAME_PARAMETER = "payloadname"
const val OPTION_PARAMETER = "option"
const val DEFAULT_PARAMETER = "default"
const val ROUTE_TO_PARAMETER = "routeTo"
const val ALLOW_DUPLICATES_PARAMETER = "allowDuplicate"
const val TOPIC_PARAMETER = "topic"

/**
 * Base class for ReportFunction and ValidateFunction
 */
abstract class RequestFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
) {
    /**
     * The data that wraps a request that we receive from a sender.
     */
    data class ValidatedRequest(
        val content: String = "",
        val defaults: Map<String, String> = emptyMap(),
        val routeTo: List<String> = emptyList(),
        val sender: Sender,
        val topic: String = "covid-19"
    )

    /**
     * Extract client header from request headers or query string parameters
     * @param request the http request message from the client
     */
    protected fun extractClient(request: HttpRequestMessage<String?>): String {
        // client can be in the header or in the url parameters:
        return request.headers[CLIENT_PARAMETER]
            ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
    }

    /**
     * Extract the optional payloadName (aka sender-supplied filename) from request headers or query string parameters
     * @param request the http request message from the client
     */
    protected fun extractPayloadName(request: HttpRequestMessage<String?>): String? {
        // payloadName can be in the header or in the url parameters.  Return null if not found.
        return request.headers[PAYLOAD_NAME_PARAMETER]
            ?: request.queryParameters[PAYLOAD_NAME_PARAMETER]
    }

    /**
     * Take the request and parse it into a [ValidatedRequest] object with some
     * sensible default values
     */
    internal fun validateRequest(request: HttpRequestMessage<String?>): ValidatedRequest {
        val actionLogs = ActionLogger()
        HttpUtilities.payloadSizeCheck(request)

        val topic = request.queryParameters.getOrDefault(TOPIC_PARAMETER, "covid-19")

        val receiverNamesText = request.queryParameters.getOrDefault(ROUTE_TO_PARAMETER, "")
        val routeTo = if (receiverNamesText.isNotBlank()) receiverNamesText.split(ROUTE_TO_SEPARATOR) else emptyList()
        routeTo.filter { workflowEngine.settings.findReceiver(it) == null }
            .forEach { actionLogs.error(InvalidParamMessage("Invalid receiver name: $it")) }

        val clientName = extractClient(request)
        if (clientName.isBlank()) {
            actionLogs.error(InvalidParamMessage("Expected a '$CLIENT_PARAMETER' query parameter"))
        }

        val sender = workflowEngine.settings.findSender(clientName)
        if (sender == null) {
            actionLogs.error(InvalidParamMessage("'$CLIENT_PARAMETER:$clientName': unknown sender"))
        }

        // verify schema if the sender is a topic sender
        var schema: Schema? = null
        if (sender != null && sender is HasSchema) {
            schema = workflowEngine.metadata.findSchema(sender.schemaName)
            if (schema == null) {
                actionLogs.error(
                    InvalidParamMessage("'$CLIENT_PARAMETER:$clientName': unknown schema '${sender.schemaName}'")
                )
            }
        }

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.lowercase(), "")
        if (contentType.isBlank()) {
            actionLogs.error(InvalidParamMessage("Missing ${HttpHeaders.CONTENT_TYPE} header"))
        } else if (sender != null && sender.format.mimeType != contentType) {
            actionLogs.error(InvalidParamMessage("Expecting content type of '${sender.format.mimeType}'"))
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            when (contentType) {
                "application/hl7-v2" ->
                    actionLogs.error(
                        InvalidParamMessage(
                            "Cannot parse empty HL7 message. Please refer to the HL7 specification and resubmit."
                        )
                    )
                else -> actionLogs.error(InvalidParamMessage("Expecting a post message with content"))
            }
        }

        if (sender == null || content.isEmpty() || actionLogs.hasErrors()) {
            throw actionLogs.exception
        }

        val defaultValues = if (request.queryParameters.containsKey(DEFAULT_PARAMETER)) {
            val values = request.queryParameters.getOrDefault(DEFAULT_PARAMETER, "").split(",")
            values.mapNotNull {
                val parts = it.split(DEFAULT_SEPARATOR)
                if (parts.size != 2) {
                    actionLogs.error(InvalidParamMessage("'$it' is not a valid default"))
                    return@mapNotNull null
                }

                // only non full ELR senders will have a schema
                if (sender is HasSchema && schema != null) {
                    val element = schema.findElement(parts[0])
                    if (element == null) {
                        actionLogs.error(InvalidParamMessage("'${parts[0]}' is not a valid element name"))
                        return@mapNotNull null
                    }
                    val error = element.checkForError(parts[1])
                    if (error != null) {
                        actionLogs.error(InvalidParamMessage(error.message))
                        return@mapNotNull null
                    }
                }
                Pair(parts[0], parts[1])
            }.toMap()
        } else {
            emptyMap()
        }

        return ValidatedRequest(
            content,
            defaultValues,
            routeTo,
            sender,
            topic
        )
    }
}