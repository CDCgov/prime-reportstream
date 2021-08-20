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
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TokenAuthentication
import org.apache.logging.log4j.kotlin.Logging
import org.postgresql.util.PSQLException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.logging.Level

private const val CLIENT_PARAMETER = "client"
private const val OPTION_PARAMETER = "option"
private const val DEFAULT_PARAMETER = "default"
private const val DEFAULT_SEPARATOR = ":"
private const val ROUTE_TO_PARAMETER = "routeTo"
private const val ROUTE_TO_SEPARATOR = ","
private const val VERBOSE_PARAMETER = "verbose"
private const val VERBOSE_TRUE = "true"
/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction : Logging {
    enum class Options {
        None,
        ValidatePayload,
        CheckConnections,
        SkipSend,
        SkipInvalidItems,
        SendImmediately,
    }

    data class ValidatedRequest(
        val httpStatus: HttpStatus,
        val errors: MutableList<ResultDetail> = mutableListOf(),
        val warnings: MutableList<ResultDetail> = mutableListOf(),
        val options: Options = Options.None,
        val defaults: Map<String, String> = emptyMap(),
        val routeTo: List<String> = emptyList(),
        val report: Report? = null,
        val sender: Sender? = null,
        val verbose: Boolean = false,
    )

    data class ItemRouting(
        val reportIndex: Int,
        val trackingId: String?,
        val destinations: MutableList<String> = mutableListOf(),
    )

    /**
     * POST a report to the router
     *
     * @see ../../../docs/openapi.yml
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
        return ingestReport(request, context)
    }

    /**
     * The Waters API, in memory of Dr. Michael Waters
     * (The older version of this API is "/api/reports")
     * POST a report to the router, using FHIR auth security
     */
    @FunctionName("waters")
    @StorageAccount("AzureWebJobsStorage")
    fun report(
        @HttpTrigger(
            name = "waters",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {

        logger.debug(" request headers: ${request.headers}")
        val workflowEngine = WorkflowEngine()
        val authenticationStrategy = AuthenticationStrategy.authStrategy(
            request.headers["authentication-type"],
            PrincipalLevel.USER,
            workflowEngine
        )
        val senderName = request.headers[CLIENT_PARAMETER]
            ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
        // todo This code is redundant w/validateRequest. Remove from validateRequest once old endpoint is removed
        if (senderName.isBlank())
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")
        val sender = workflowEngine.settings.findSender(senderName)
            ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")

        if (authenticationStrategy is OktaAuthentication) {
            // Okta Auth
            return authenticationStrategy.checkAccess(request, senderName) {
                return@checkAccess ingestReport(request, context)
            }
        }

        if (authenticationStrategy is TokenAuthentication) {
            val claims = authenticationStrategy.checkAccessToken(request, "${sender.fullName}.report")
                ?: return HttpUtilities.unauthorizedResponse(request)
            logger.info("Claims for ${claims["sub"]} validated.  Beginning ingestReport.")
            return ingestReport(request, context)
        }
        return HttpUtilities.bad(request, "Failed authorization") // unreachable code.
    }

    private fun ingestReport(request: HttpRequestMessage<String?>, context: ExecutionContext): HttpResponseMessage {
        val workflowEngine = WorkflowEngine()
        val actionHistory = ActionHistory(TaskAction.receive, context)
        var report: Report? = null
        val verboseResponse = StringBuilder()
        actionHistory.trackActionParams(request)
        val httpResponseMessage = try {
            val validatedRequest = validateRequest(workflowEngine, request)
            // track the sending organization and client based on the header
            actionHistory.trackActionSender(extractClientHeader(request))
            when {
                validatedRequest.options == Options.CheckConnections -> {
                    workflowEngine.checkConnections()
                    verboseResponse.append(createResponseBody(validatedRequest, false))
                    HttpUtilities.okResponse(request, verboseResponse.toString())
                }
                validatedRequest.report == null -> {
                    verboseResponse.append(createResponseBody(validatedRequest, false))
                    HttpUtilities.httpResponse(
                        request,
                        verboseResponse.toString(),
                        validatedRequest.httpStatus
                    )
                }
                validatedRequest.options == Options.ValidatePayload -> {
                    verboseResponse.append(createResponseBody(validatedRequest, false))
                    HttpUtilities.okResponse(request, verboseResponse.toString())
                }
                else -> {
                    // Regular happy path workflow is here
                    context.logger.info("Successfully reported: ${validatedRequest.report.id}.")
                    report = validatedRequest.report
                    routeReport(context, workflowEngine, validatedRequest, actionHistory)
                    if (request.body != null && validatedRequest.sender != null) {
                        workflowEngine.recordReceivedReport(
                            report, request.body!!.toByteArray(), validatedRequest.sender,
                            actionHistory, workflowEngine
                        )
                    } else error(
                        // This should never happen after the validation, but we do not want a mystery exception here
                        "Unable to save original report ${report.name} due to null " +
                            "request body or sender"
                    )
                    val responseBody = createResponseBody(validatedRequest, validatedRequest.verbose, actionHistory)
                    // if a verbose response was not requested, then generate one for the actionResponse
                    verboseResponse.append(
                        if (validatedRequest.verbose)
                            responseBody
                        else
                            createResponseBody(validatedRequest, true, actionHistory)
                    )
                    HttpUtilities.createdResponse(request, responseBody)
                }
            }
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            actionHistory.trackActionResult("Exception: ${e.message ?: e}")
            HttpUtilities.internalErrorResponse(request)
        }
        actionHistory.trackActionResult(httpResponseMessage)
        // add the response to the action table as JSONB and record the httpsStatus
        actionHistory.trackActionResponse(httpResponseMessage, verboseResponse.toString())
        workflowEngine.recordAction(actionHistory)
        actionHistory.queueMessages(workflowEngine) // Must be done after creating TASK record.
        // write the data to the table if we're dealing with covid-19. this has to happen
        // here AFTER we've written the report to the DB
        writeCovidResultMetadataForReport(report, context, workflowEngine)
        return httpResponseMessage
    }

    /**
     * Given a report object, it collects the non-PII, non-PHI out of it and then saves it to the
     * database in the covid_results_metadata table.
     */
    private fun writeCovidResultMetadataForReport(
        report: Report?,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine
    ) {
        if (report != null && report.schema.topic.lowercase() == "covid-19") {
            // next check that we're dealing with an external file
            val clientSource = report.sources.firstOrNull { it is ClientSource }
            if (clientSource != null) {
                context.logger.info("Writing deidentified report data to the DB")
                // wrap the insert into an exception handler
                try {
                    workflowEngine.db.transact { txn ->
                        // verify the file exists in report_file before continuing
                        if (workflowEngine.db.checkReportExists(report.id, txn)) {
                            val deidentifiedData = report.getDeidentifiedResultMetaData()
                            workflowEngine.db.saveTestData(deidentifiedData, txn)
                            context.logger.info("Wrote ${deidentifiedData.count()} rows to test data table")
                        } else {
                            // warn if it does not exist
                            context.logger.warning(
                                "Skipping write to metadata table because " +
                                    "reportId ${report.id} does not exist in report_file."
                            )
                        }
                    }
                } catch (pse: PSQLException) {
                    // report this but move on
                    context.logger.severe(
                        "Exception writing COVID test metadata " +
                            "for ${report.id}: ${pse.localizedMessage}.\n" +
                            pse.stackTraceToString()
                    )
                } catch (e: Exception) {
                    // catch all as we have seen jooq Exceptions thrown
                    context.logger.severe(
                        "Exception writing COVID test metadata " +
                            "for ${report.id}: ${e.localizedMessage}.\n" +
                            e.stackTraceToString()
                    )
                }
            }
        }
    }

    /**
     * Extract client header from request headers or query string parameters
     * @param request the http request message from the client
     */
    private fun extractClientHeader(request: HttpRequestMessage<String?>): String {
        return request.headers[CLIENT_PARAMETER] ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
    }

    private fun validateRequest(engine: WorkflowEngine, request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()
        val (sizeStatus, errMsg) = HttpUtilities.payloadSizeCheck(request)
        if (sizeStatus != HttpStatus.OK) {
            errors.add(ResultDetail.report(errMsg))
            // If size is too big, we ignore the option.
            return ValidatedRequest(sizeStatus, errors, warnings)
        }

        val optionsText = request.queryParameters.getOrDefault(OPTION_PARAMETER, "")
        val options = if (optionsText.isNotBlank()) {
            try {
                Options.valueOf(optionsText)
            } catch (e: IllegalArgumentException) {
                errors.add(ResultDetail.param(OPTION_PARAMETER, "'$optionsText' is not valid"))
                Options.None
            }
        } else {
            Options.None
        }

        if (options == Options.CheckConnections) {
            return ValidatedRequest(HttpStatus.OK, errors, warnings, options = options)
        }

        val receiverNamesText = request.queryParameters.getOrDefault(ROUTE_TO_PARAMETER, "")
        val routeTo = if (receiverNamesText.isNotBlank()) receiverNamesText.split(ROUTE_TO_SEPARATOR) else emptyList()
        val receiverNameErrors = routeTo
            .filter { engine.settings.findReceiver(it) == null }
            .map { ResultDetail.param(ROUTE_TO_PARAMETER, "Invalid receiver name: $it") }
        errors.addAll(receiverNameErrors)

        val clientName = extractClientHeader(request)
        if (clientName.isBlank())
            errors.add(ResultDetail.param(CLIENT_PARAMETER, "Expected a '$CLIENT_PARAMETER' query parameter"))

        val sender = engine.settings.findSender(clientName)
        if (sender == null)
            errors.add(ResultDetail.param(CLIENT_PARAMETER, "'$CLIENT_PARAMETER:$clientName': unknown sender"))

        val schema = engine.metadata.findSchema(sender?.schemaName ?: "")
        if (sender != null && schema == null)
            errors.add(
                ResultDetail.param(
                    CLIENT_PARAMETER, "'$CLIENT_PARAMETER:$clientName': unknown schema '${sender.schemaName}'"
                )
            )

        // extract the verbose param and default to empty if not present
        val verboseParam = request.queryParameters.getOrDefault(VERBOSE_PARAMETER, "")
        val verbose = verboseParam.equals(VERBOSE_TRUE, true)

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.lowercase(), "")
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
            return ValidatedRequest(HttpStatus.BAD_REQUEST, errors, warnings)
        }

        val defaultValues = if (request.queryParameters.containsKey(DEFAULT_PARAMETER)) {
            val values = request.queryParameters.getOrDefault(DEFAULT_PARAMETER, "").split(",")
            values.mapNotNull {
                val parts = it.split(DEFAULT_SEPARATOR)
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
                    errors.add(ResultDetail.param(DEFAULT_PARAMETER, error))
                    return@mapNotNull null
                }
                Pair(parts[0], parts[1])
            }.toMap()
        } else {
            emptyMap()
        }

        if (content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(HttpStatus.BAD_REQUEST, errors, warnings)
        }

        var report = createReport(engine, sender, content, defaultValues, errors, warnings)

        var status = HttpStatus.OK
        if (options != Options.SkipInvalidItems && errors.isNotEmpty()) {
            report = null
            status = HttpStatus.BAD_REQUEST
        }
        return ValidatedRequest(status, errors, warnings, options, defaultValues, routeTo, report, sender, verbose)
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
                    val readResult = engine.csvSerializer.readExternal(
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
            Sender.Format.HL7 -> {
                try {
                    val readResult = engine.hl7Serializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        ClientSource(organization = sender.organizationName, client = sender.name)
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
        actionHistory: ActionHistory,
    ) {
        if (validatedRequest.options == Options.ValidatePayload ||
            validatedRequest.options == Options.CheckConnections
        ) return
        workflowEngine.db.transact { txn ->
            workflowEngine
                .translator
                .filterAndTranslateByReceiver(
                    validatedRequest.report!!,
                    validatedRequest.defaults,
                    validatedRequest.routeTo,
                    validatedRequest.warnings,
                )
                .forEach { (report, receiver) ->
                    sendToDestination(
                        report,
                        receiver,
                        context,
                        workflowEngine,
                        validatedRequest,
                        actionHistory,
                        txn
                    )
                }
        }
    }

    private fun sendToDestination(
        report: Report,
        receiver: Receiver,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        validatedRequest: ValidatedRequest,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction
    ) {
        val loggerMsg: String
        when {
            validatedRequest.options == Options.SkipSend -> {
                // Note that SkipSend should really be called SkipBothTimingAndSend  ;)
                val event = ReportEvent(Event.EventAction.NONE, report.id)
                workflowEngine.dispatchReport(event, report, actionHistory, receiver, txn, context)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.timing != null && validatedRequest.options != Options.SendImmediately -> {
                val time = receiver.timing.nextTime()
                // Always force a batched report to be saved in our INTERNAL format
                val batchReport = report.copy(bodyFormat = Report.Format.INTERNAL)
                val event = ReceiverEvent(Event.EventAction.BATCH, receiver.fullName, time)
                workflowEngine.dispatchReport(event, batchReport, actionHistory, receiver, txn, context)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.format == Report.Format.HL7 -> {
                report
                    .split()
                    .forEach {
                        val event = ReportEvent(Event.EventAction.SEND, it.id)
                        workflowEngine.dispatchReport(event, it, actionHistory, receiver, txn, context)
                    }
                loggerMsg = "Queued to send immediately: HL7 split into ${report.itemCount} individual reports"
            }
            else -> {
                val event = ReportEvent(Event.EventAction.SEND, report.id)
                workflowEngine.dispatchReport(event, report, actionHistory, receiver, txn, context)
                loggerMsg = "Queued to send immediately: ${event.toQueueMessage()}"
            }
        }
        context.logger.info(loggerMsg)
    }

    // todo I think all of this info is now in ActionHistory.  Move to there.   Already did destinations.
    private fun createResponseBody(
        result: ValidatedRequest,
        verbose: Boolean,
        actionHistory: ActionHistory? = null,
    ): String {
        val factory = JsonFactory()
        val outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.useDefaultPrettyPrinter()
            it.writeStartObject()
            if (result.report != null) {
                it.writeStringField("id", result.report.id.toString())
                it.writeStringField("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                it.writeStringField("topic", result.report.schema.topic)
                it.writeNumberField("reportItemCount", result.report.itemCount)
            } else
                it.writeNullField("id")

            actionHistory?.prettyPrintDestinationsJson(it, WorkflowEngine.settings, result.options)
            // print the report routing when in verbose mode
            if (verbose) {
                it.writeArrayFieldStart("routing")
                createItemRouting(result, actionHistory).forEach { ij ->
                    it.writeStartObject()
                    it.writeNumberField("reportIndex", ij.reportIndex)
                    it.writeStringField("trackingId", ij.trackingId)
                    it.writeArrayFieldStart("destinations")
                    ij.destinations.sorted().forEach { d -> it.writeString(d) }
                    it.writeEndArray()
                    it.writeEndObject()
                }
                it.writeEndArray()
            }

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

    /**
     * Creates a list of [ItemRouting] instances with the report index
     * and trackingId along with the list of the receiver organizations where
     * the report was routed.
     * @param validatedRequest the instance generated while processing the report
     * @param actionHistory the instance generated while processing the report
     * @return the report routing for each item
     */
    private fun createItemRouting(
        validatedRequest: ValidatedRequest,
        actionHistory: ActionHistory? = null,
    ): List<ItemRouting> {
        // create the item routing from the item lineage
        val routingMap = mutableMapOf<Int, ItemRouting>()
        actionHistory?.let { ah ->
            ah.itemLineages.forEach { il ->
                val item = routingMap.getOrPut(il.parentIndex) { ItemRouting(il.parentIndex, il.trackingId) }
                ah.reportsOut[il.childReportId]?.let { rf ->
                    item.destinations.add("${rf.receivingOrg}.${rf.receivingOrgSvc}")
                }
            }
        }
        // account for any items that routed no where and were not in the item lineage
        return validatedRequest.report?.let { report ->
            val items = mutableListOf<ItemRouting>()
            // the report has all the submitted items
            report.itemIndices.forEach { i ->
                // if an item was not present, create the routing with empty destinations
                items.add(
                    routingMap.getOrDefault(
                        i,
                        ItemRouting(i, report.getString(i, report.schema.trackingElement ?: ""))
                    )
                )
            }
            items
        } ?: run {
            // unlikely, but in case the report is null...
            routingMap.toSortedMap().values.map { it }
        }
    }
}