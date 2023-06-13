package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DEFAULT_SEPARATOR
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.LegacyPipelineSender
import gov.cdc.prime.router.ROUTE_TO_SEPARATOR
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import java.lang.IllegalArgumentException
import java.security.InvalidParameterException

const val CLIENT_PARAMETER = "client"
const val PAYLOAD_NAME_PARAMETER = "payloadname"
const val OPTION_PARAMETER = "option"
const val DEFAULT_PARAMETER = "default"
const val ROUTE_TO_PARAMETER = "routeTo"
const val ALLOW_DUPLICATES_PARAMETER = "allowDuplicate"
const val TOPIC_PARAMETER = "topic"
const val SCHEMA_PARAMETER = "schema"
const val FORMAT_PARAMETER = "format"

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
        val sender: Sender?,
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

        var sender: Sender? = null
        val clientName = extractClient(request)
        if (clientName.isBlank()) {
            // Find schema via SCHEMA_PARAMETER parameter
            try {
                sender = getDummySender(
                    request.queryParameters.getOrDefault(SCHEMA_PARAMETER, null),
                    request.queryParameters.getOrDefault(FORMAT_PARAMETER, null)
                )
            } catch (e: InvalidParameterException) {
                actionLogs.error(
                    InvalidParamMessage(e.message.toString())
                )
            }
        } else {
            // Find schema via CLIENT_PARAMETER parameter
            sender = workflowEngine.settings.findSender(clientName)
            if (sender == null) {
                actionLogs.error(InvalidParamMessage("'$CLIENT_PARAMETER:$clientName': unknown sender"))
            }
        }

        // verify schema if the sender is a topic sender
        var schema: Schema? = null
        if (sender != null && sender is LegacyPipelineSender) {
            schema = workflowEngine.metadata.findSchema(sender.schemaName)
            if (schema == null) {
                actionLogs.error(
                    InvalidParamMessage("unknown schema '${sender.schemaName}'")
                )
            }
        }

        // validate content type
        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.lowercase(), "")
        if (contentType.isBlank()) {
            actionLogs.error(InvalidParamMessage("Missing ${HttpHeaders.CONTENT_TYPE} header"))
        } else if (sender != null && sender.format.mimeType != contentType) {
            actionLogs.error(
                InvalidParamMessage("Resubmit as '${sender.format.mimeType}'")
            )
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            when (contentType) {
                "application/hl7-v2" ->
                    actionLogs.error(
                        InvalidParamMessage(
                            "Blank message(s) found within file. Blank messages cannot be processed."
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

                // only topic sender schemas are relevant here
                if (sender is LegacyPipelineSender && schema != null) {
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

    /**
     * Return [LegacyPipelineSender] for a given schema if that schema exists. This lets us wrap the data needed by
     * processRequest without making changes to the method
     * @param schemaName the name or path of the schema
     * @param format the message format that the schema supports
     * @return LegacyPipelineSender if schema exists, null otherwise
     * @throws InvalidParameterException if [schemaName] or [formatName] is not valid
     */
    @Throws(InvalidParameterException::class)
    internal fun getDummySender(schemaName: String?, formatName: String?): LegacyPipelineSender {
        val errMsgPrefix = "No client found in header so expected valid " +
            "'$SCHEMA_PARAMETER' and '$FORMAT_PARAMETER' query parameters but found error: "
        if (schemaName != null && formatName != null) {
            val schema = workflowEngine.metadata.findSchema(schemaName)
                ?: throw InvalidParameterException("$errMsgPrefix The schema with name '$schemaName' does not exist")
            val format = try {
                Sender.Format.valueOf(formatName)
            } catch (e: IllegalArgumentException) {
                throw InvalidParameterException("$errMsgPrefix The format '$formatName' is not supported")
            }
            return LegacyPipelineSender(
                "ValidationSender",
                "Internal",
                format,
                CustomerStatus.TESTING,
                schemaName,
                schema.topic
            )
        } else {
            throw InvalidParameterException("$errMsgPrefix 'SchemaName' and 'format' parameters must not be null")
        }
    }
}