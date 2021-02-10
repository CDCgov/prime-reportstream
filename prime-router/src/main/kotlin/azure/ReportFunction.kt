package gov.cdc.prime.router.azure

import com.fasterxml.jackson.core.JsonFactory
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
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction {
    private val clientParameter = "client"
    private val optionParameter = "option"
    private val defaultParameter = "default"
    private val defaultSeparator = ":"
    private val jsonMediaType = "application/json" // TODO: find a good media library

    enum class Options {
        None,
        ValidatePayload,
        CheckConnections,
        SkipSend,
        SkipInvalidItems,
    }

    data class ValidatedRequest(
        val options: Options,
        val defaults: Map<String, String>,
        val errors: List<ResultDetail>,
        val warnings: List<ResultDetail>,
        val report: Report?
    )

    /**
     * @see docs/openapi.yml
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
            val actionHistory = ActionHistory(TaskAction.receive, context)
            actionHistory.trackActionParams(request)
            val validatedRequest = validateRequest(workflowEngine, request)
            val httpResponseMessage = when {
                validatedRequest.options == Options.CheckConnections -> {
                    workflowEngine.checkConnections()
                    okResponse(request, validatedRequest)
                }
                validatedRequest.report == null -> {
                    badRequestResponse(request, validatedRequest)
                }
                validatedRequest.options == Options.ValidatePayload -> {
                    okResponse(request, validatedRequest)
                }
                else -> {
                    context.logger.info("Successfully reported: ${validatedRequest.report.id}.")
                    val destinations = mutableListOf<String>()
                    routeReport(context, workflowEngine, validatedRequest, destinations, actionHistory)
                    val responseBody = createResponseBody(validatedRequest, destinations)
                    workflowEngine.receiveReport(validatedRequest.report)
                    actionHistory.trackExternalInputReport(validatedRequest)
                    createdResponse(request, validatedRequest, responseBody)
                }
            }
            actionHistory.trackActionResult(httpResponseMessage)
            workflowEngine.recordAction(actionHistory)
            actionHistory.queueMessages() // Must be done after creating db records.
            return httpResponseMessage
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            return internalErrorResponse(request)
        }
    }

    private fun validateRequest(engine: WorkflowEngine, request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()

        val optionsText = request.queryParameters.getOrDefault(optionParameter, "")
        val options = if (optionsText.isNotBlank()) {
            try {
                Options.valueOf(optionsText)
            } catch (e: IllegalArgumentException) {
                errors.add(ResultDetail.param(optionParameter, "'$optionsText' is not valid"))
                Options.None
            }
        } else {
            Options.None
        }
        if (options == Options.CheckConnections) {
            return ValidatedRequest(options, emptyMap(), errors, warnings, null)
        }

        val clientName = request.headers[clientParameter] ?: request.queryParameters.getOrDefault(clientParameter, "")
        if (clientName.isBlank())
            errors.add(ResultDetail.param(clientParameter, "Expected a '$clientParameter' query parameter"))
        val sender = engine.metadata.findSender(clientName)
        if (sender == null)
            errors.add(ResultDetail.param(clientParameter, "'$clientName' is not a valid"))
        val schema = engine.metadata.findSchema(sender?.schemaName ?: "")

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.toLowerCase(), "")
        if (contentType.isBlank()) {
            errors.add(ResultDetail.param(HttpHeaders.CONTENT_TYPE, "missing"))
        } else if (sender != null && sender.format.mimeType != contentType) {
            errors.add(ResultDetail.param(HttpHeaders.CONTENT_TYPE, "expecting '${sender.format.mimeType}'"))
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            errors.add(ResultDetail.param("Content", "expecting a post message with content"))
        }

        if (sender == null || schema == null || content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(options, emptyMap(), errors, warnings, null)
        }

        val defaultValues = if (request.queryParameters.containsKey(defaultParameter)) {
            val values = request.queryParameters.getOrDefault(defaultParameter, "").split(",")
            values.mapNotNull {
                val parts = it.split(defaultSeparator)
                if (parts.size != 2) {
                    errors.add(ResultDetail.report("'$it' is not a valid default"))
                    return@mapNotNull null
                }
                val element = schema.findElement(parts[0])
                if (element == null) {
                    errors.add(ResultDetail.report("'${parts[0]}' is not a valid element name"))
                    return@mapNotNull null
                }
                val error = element.checkForError(parts[1])
                if (error != null) {
                    errors.add(ResultDetail.param(defaultParameter, error))
                    return@mapNotNull null
                }
                Pair(parts[0], parts[1])
            }.toMap()
        } else {
            emptyMap()
        }

        if (content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(options, defaultValues, errors, warnings, null)
        }

        var report = createReport(engine, sender, content, defaultValues, errors, warnings)
        if (options != Options.SkipInvalidItems && errors.isNotEmpty()) {
            report = null
        }
        return ValidatedRequest(options, defaultValues, errors, warnings, report)
    }

    private fun createReport(
        engine: WorkflowEngine,
        sender: Sender,
        content: String,
        defaults: Map<String, String>,
        errors: MutableList<ResultDetail>,
        warnings: MutableList<ResultDetail>
    ): Report? {
        return when (sender.format) {
            Sender.Format.CSV -> {
                try {
                    val readResult = engine.csvSerializer.read(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        sources = listOf(ClientSource(organization = sender.organizationName, client = sender.name)),
                        defaultValues = defaults
                    )
                    errors += readResult.errors
                    warnings += readResult.warnings
                    readResult.report
                } catch (e: Exception) {
                    errors.add(ResultDetail.report(e.message ?: ""))
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
        actionHistory: ActionHistory,
    ) {
        if (validatedRequest.options == Options.ValidatePayload ||
            validatedRequest.options == Options.CheckConnections
        ) return
        workflowEngine.db.transact { txn ->
            workflowEngine
                .translator
                .filterAndTranslateByReceiver(validatedRequest.report!!, validatedRequest.defaults)
                .forEach { (report, receiver) ->
                    val organization = workflowEngine.metadata.findOrganization(receiver.organizationName)!!
                    sendToDestination(
                        report,
                        organization,
                        receiver,
                        context,
                        workflowEngine,
                        validatedRequest,
                        destinations,
                        actionHistory,
                        txn
                    )
                }
        }
    }

    private fun sendToDestination(
        report: Report,
        organization: Organization,
        receiver: Receiver,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        validatedRequest: ValidatedRequest,
        destinations: MutableList<String>,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction
    ) {
        val receiverDescription = "${organization.description} (${receiver.name})"
        val loggerMsg: String
        when {
            validatedRequest.options == Options.SkipSend -> {
                val event = ReportEvent(Event.EventAction.NONE, report.id)
                workflowEngine.dispatchReport(event, report, txn)
                actionHistory.trackCreatedReport(event, report, receiver)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.timing != null -> {
                val time = receiver.timing.nextTime()
                // Always force a batched report to be saved in our INTERNAL format
                val batchReport = report.copy(bodyFormat = Report.Format.INTERNAL)
                destinations += "Sending ${batchReport.itemCount} items to $receiverDescription at $time"
                val event = ReceiverEvent(Event.EventAction.BATCH, receiver.fullName, time)
                workflowEngine.dispatchReport(event, batchReport, txn)
                actionHistory.trackCreatedReport(event, batchReport, receiver)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.format == Report.Format.HL7 -> {
                destinations += "Sending ${report.itemCount} reports to $receiverDescription immediately"
                report
                    .split()
                    .forEach {
                        val event = ReportEvent(Event.EventAction.SEND, it.id)
                        workflowEngine.dispatchReport(event, it, txn)
                        actionHistory.trackCreatedReport(event, it, receiver)
                    }
                loggerMsg = "Queue: ${report.itemCount} reports"
            }
            else -> {
                destinations += "Sending ${report.itemCount} items to $receiverDescription immediately"
                val event = ReportEvent(Event.EventAction.SEND, report.id)
                workflowEngine.dispatchReport(event, report, txn)
                actionHistory.trackCreatedReport(event, report, receiver)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
        }
        context.logger.info(loggerMsg)
    }

    private fun createResponseBody(result: ValidatedRequest, destinations: List<String> = emptyList()): String {
        val factory = JsonFactory()
        val outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.useDefaultPrettyPrinter()
            it.writeStartObject()
            if (result.report != null) {
                it.writeStringField("id", result.report.id.toString())
                it.writeNumberField("reportItemCount", result.report.itemCount)
            } else
                it.writeNullField("id")
            it.writeArrayFieldStart("destinations")
            destinations.forEach { destination -> it.writeString(destination) }
            it.writeEndArray()

            it.writeNumberField("warningCount", result.warnings.size)
            it.writeNumberField("errorCount", result.errors.size)

            fun writeDetailsArray(field: String, array: List<ResultDetail>) {
                it.writeArrayFieldStart(field)
                array.forEach { error ->
                    it.writeStartObject()
                    it.writeStringField("scope", error.scope.toString())
                    it.writeStringField("id", error.id)
                    it.writeStringField("details", error.details)
                    it.writeEndObject()
                }
                it.writeEndArray()
            }

            writeDetailsArray("errors", result.errors)
            writeDetailsArray("warnings", result.warnings)
            it.writeEndObject()
        }
        return outStream.toString()
    }

    private fun okResponse(
        request: HttpRequestMessage<String?>,
        validatedRequest: ValidatedRequest
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.OK)
            .body(createResponseBody(validatedRequest))
            .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
            .build()
    }

    private fun badRequestResponse(
        request: HttpRequestMessage<String?>,
        validatedRequest: ValidatedRequest,
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body(createResponseBody(validatedRequest))
            .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
            .build()
    }

    private fun createdResponse(
        request: HttpRequestMessage<String?>,
        validatedRequest: ValidatedRequest,
        responseBody: String,
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.CREATED)
            .body(responseBody)
            .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
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