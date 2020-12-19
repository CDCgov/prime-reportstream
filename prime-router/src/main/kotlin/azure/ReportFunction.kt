package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.OrganizationClient
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction {
    private val clientName = "client"
    private val requestOption = "option"
    enum class Options {
        None,
        ValidatePayload,
        CheckConnections,
        SkipSend,
        SendTrainingFlag,
        SendDebugFlag,
    }

    data class ValidatedRequest(
        val options: Options,
        val rejected: List<Int>,
        val errors: List<String>,
        val warnings: List<String>,
        val report: Report?
    )

    /**
     * Run ./test-ingest.sh to get an example curl call that calls this function.
     */
    @FunctionName("reports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        try {
            val workflowEngine = WorkflowEngine()
            val validatedRequest = validateRequest(workflowEngine, request)
            when {
                validatedRequest.options == Options.CheckConnections -> {
                    workflowEngine.checkConnections()
                    return okResponse(request, validatedRequest)
                }
                validatedRequest.report == null || validatedRequest.errors.isNotEmpty() -> {
                    return badRequestResponse(request, validatedRequest)
                }
                validatedRequest.options == Options.ValidatePayload -> {
                    return okResponse(request, validatedRequest)
                }
            }
            context.logger.info("Successfully reported: ${validatedRequest.report!!.id}.")
            val destinations = mutableListOf<String>()
            routeReport(context, workflowEngine, validatedRequest, destinations)
            return createdResponse(request, validatedRequest, destinations)
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            return internalErrorResponse(request)
        }
    }

    private fun validateRequest(engine: WorkflowEngine, request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val rejected = mutableListOf<Int>()

        val optionsText = request.queryParameters.getOrDefault(requestOption, "")
        val options = if (optionsText.isNotBlank()) {
            try {
                Options.valueOf(optionsText)
            } catch (e: IllegalArgumentException) {
                errors.add("Error: '$optionsText' is not a valid '$requestOption' query parameter")
                Options.None
            }
        } else {
            Options.None
        }
        if (options == Options.CheckConnections) {
            return ValidatedRequest(options, rejected, errors, warnings, null)
        }

        val name = request.headers[clientName] ?: request.queryParameters.getOrDefault(clientName, "")
        if (name.isBlank()) errors.add("Error: Expected a '$clientName' query parameter")
        val client = WorkflowEngine.metadata.findClient(name)
        if (client == null) {
            errors.add("Error: '$name' is not a valid client name")
        }

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.toLowerCase(), "")
        if (contentType.isBlank()) {
            errors.add("Error: expecting a content-type header")
        } else if (client != null && client.format.mimeType != contentType) {
            errors.add("Error: expecting a '${client.format.mimeType}' content-type header")
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            errors.add("Error: expecting a post message with content")
        }

        if (client == null || content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(options, rejected, errors, warnings, null)
        }

        val report = createReport(engine, client, content, rejected, errors, warnings)
        return ValidatedRequest(options, rejected, errors, warnings, report)
    }

    private fun createReport(
        engine: WorkflowEngine,
        client: OrganizationClient,
        content: String,
        rejected: MutableList<Int>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): Report? {
        engine.metadata.findSchema(client.schema) ?: error("missing schema for $clientName")
        return when (client.format) {
            OrganizationClient.Format.CSV -> {
                try {
                    val readResult = engine.csvConverter.read(
                        client.schema,
                        ByteArrayInputStream(content.toByteArray()),
                        ClientSource(organization = client.organization.name, client = client.name)
                    )
                    errors += readResult.errors
                    warnings += readResult.warnings
                    rejected += readResult.rejected
                    readResult.report
                } catch (e: Exception) {
                    errors.add("Error: ${e.message}")
                    null
                }
            }
        }
    }

    private fun routeReport(
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        validatedRequest: ValidatedRequest,
        destinations: MutableList<String>,
    ) {
        if (validatedRequest.options == Options.ValidatePayload ||
            validatedRequest.options == Options.CheckConnections
        ) return
        val defaultValues = when (validatedRequest.options) {
            Options.SendDebugFlag -> mapOf("processing_mode_code" to "D")
            Options.SendTrainingFlag -> mapOf("processing_mode_code" to "T")
            else -> emptyMap()
        }
        workflowEngine.db.transact { txn ->
            workflowEngine
                .translator
                .filterAndTranslateByService(validatedRequest.report!!, defaultValues)
                .forEach { (report, service) ->
                    sendToDestination(report, service, context, workflowEngine, validatedRequest, destinations, txn)
                }
        }
    }

    private fun sendToDestination(
        report: Report,
        service: OrganizationService,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        validatedRequest: ValidatedRequest,
        destinations: MutableList<String>,
        txn: DataAccessTransaction
    ) {
        val event = when {
            validatedRequest.options == Options.SkipSend -> {
                ReportEvent(Event.Action.NONE, report.id)
            }
            service.batch != null -> {
                val time = service.batch.nextBatchTime()
                val destination = "Sending ${report.itemCount} items to ${service.organization.description} at $time"
                destinations += destination
                ReceiverEvent(Event.Action.BATCH, service.fullName, time)
            }
            else -> {
                val destination = "Sending ${report.itemCount} items to ${service.organization.description} immediately"
                destinations += destination
                ReportEvent(Event.Action.SEND, report.id)
            }
        }
        workflowEngine.dispatchReport(event, report, txn)
        context.logger.info("Queue: ${event.toMessage()}")
    }

    private fun createResponseBody(result: ValidatedRequest, destinations: List<String> = emptyList()): String {
        val indentSeparator = ",\n    "
        val rejectedList = result.rejected.joinToString(indentSeparator) { "\"$it\"" }
        val destList = destinations.joinToString(indentSeparator) { "\"$it\"" }
        val errorsList = result.errors.joinToString(indentSeparator) { "\"$it\"" }
        val warningsList = result.warnings.joinToString(indentSeparator) { "\"$it\"" }
        val start = if (result.report != null)
"""
{
  "id": "${result.report.id}",
  "itemCount": ${result.report.itemCount}, 
  "destinations": [
    $destList
  ],
  "rejected": [
    $rejectedList
  ],"""
        else
"""
{"""
        val end =
"""
  "errors": [
    $errorsList
  ],
  "warnings": [
    $warningsList
  ] 
}                 
"""
        return start + end
    }

    private fun okResponse(request: HttpRequestMessage<String?>, validatedRequest: ValidatedRequest): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.OK)
            .body(createResponseBody(validatedRequest))
            .build()
    }

    private fun badRequestResponse(
        request: HttpRequestMessage<String?>,
        validatedRequest: ValidatedRequest,
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body(createResponseBody(validatedRequest))
            .build()
    }

    private fun createdResponse(
        request: HttpRequestMessage<String?>,
        validatedRequest: ValidatedRequest,
        destinations: List<String>
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.CREATED)
            .body(createResponseBody(validatedRequest, destinations))
            .build()
    }

    private fun internalErrorResponse(
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal error at ${OffsetDateTime.now()}")
            .build()
    }
}