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
import gov.cdc.prime.router.DEFAULT_SEPARATOR
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ROUTE_TO_SEPARATOR
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TokenAuthentication
import org.apache.logging.log4j.kotlin.Logging

private const val CLIENT_PARAMETER = "client"
private const val OPTION_PARAMETER = "option"
private const val DEFAULT_PARAMETER = "default"
private const val ROUTE_TO_PARAMETER = "routeTo"
private const val VERBOSE_PARAMETER = "verbose"
private const val VERBOSE_TRUE = "true"
private const val PROCESSING_TYPE_PARAMETER = "processing"

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction : Logging {

    /**
     * Enumeration representing whether a submission will be processed follow the synchronous or asynchronous
     * message pipeline. Within the code this defaults to Sync unless the PROCESSING_TYPE_PARAMETER query
     * string value is 'async'
     */
    enum class ProcessingType {
        Sync,
        Async,
    }

    data class ValidatedRequest(
        val valid: Boolean,
        val httpStatus: HttpStatus,
        val errors: MutableList<ResultDetail> = mutableListOf(),
        val warnings: MutableList<ResultDetail> = mutableListOf(),
        val content: String = "",
        val options: Options = Options.None,
        val defaults: Map<String, String> = emptyMap(),
        val routeTo: List<String> = emptyList(),
        val sender: Sender? = null,
        val verbose: Boolean = false,
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
        val workflowEngine = WorkflowEngine()

        val senderName = extractClientHeader(request)
        if (senderName.isNullOrBlank())
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")

        // Sender should eventually be obtained directly from who is authenticated
        val sender = workflowEngine.settings.findSender(senderName)
            ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")
        val actionHistory = ActionHistory(TaskAction.receive, context)
        actionHistory.trackActionParams(request)

        try {
            return processRequest(request, sender, context, workflowEngine, actionHistory)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
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
        val workflowEngine = WorkflowEngine()

        val senderName = extractClientHeader(request)
        if (senderName.isNullOrBlank())
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")

        // Sender should eventually be obtained directly from who is authenticated
        val sender = workflowEngine.settings.findSender(senderName)
            ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")

        val authenticationStrategy = AuthenticationStrategy.authStrategy(
            request.headers["authentication-type"],
            PrincipalLevel.USER,
            workflowEngine
        )

        try {
            val actionHistory = ActionHistory(TaskAction.receive, context)
            actionHistory.trackActionParams(request)

            if (authenticationStrategy is OktaAuthentication) {
                // The report is coming from a sender that is using Okta, so set "oktaSender" to true
                return authenticationStrategy.checkAccess(request, senderName, true) {
                    return@checkAccess processRequest(request, sender, context, workflowEngine, actionHistory)
                }
            }

            if (authenticationStrategy is TokenAuthentication) {
                val claims = authenticationStrategy.checkAccessToken(request, "${sender.fullName}.report")
                    ?: return HttpUtilities.unauthorizedResponse(request)
                logger.info("Claims for ${claims["sub"]} validated.  Beginning ingestReport.")
                return processRequest(request, sender, context, workflowEngine, actionHistory)
            }
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
        return HttpUtilities.bad(request, "Failed authorization")
    }

    /**
     * Handles an incoming request after it has been authenticated by either /reports or /waters endpoint.
     * Does basic validation and either pushes it into the sync or async pipeline, based on the value
     * of the incoming PROCESSING_TYPE_PARAMETER query string value
     * @param request The incoming request
     * @param sender The sender record, pulled from the database based on sender name on the request
     * @param context Execution context
     * @param workflowEngine WorkflowEngine instance used through the entire
     * @param actionHistory ActionHistory instance to track messages and lineages\
     * @return Returns an HttpResponseMessage indicating the result of the operation and any resulting information
     */
    private fun processRequest(
        request: HttpRequestMessage<String?>,
        sender: Sender,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        actionHistory: ActionHistory
    ): HttpResponseMessage {
        // determine if we should be following the sync or async workflow
        val isAsync = processingType(request) == ProcessingType.Async
        val errors: MutableList<ResultDetail> = mutableListOf()
        val warnings: MutableList<ResultDetail> = mutableListOf()
        // The following is identical to waters (for arch reasons)
        val validatedRequest = validateRequest(workflowEngine, request)
        warnings += validatedRequest.warnings
        handleValidation(validatedRequest, request, actionHistory, workflowEngine)?.let {
            return it
        }

        var report = workflowEngine.createReport(
            sender,
            validatedRequest.content,
            validatedRequest.defaults,
            errors,
            warnings
        )

        var response: HttpResponseMessage

        // default response body, will be overwritten if message is successfully processed
        var responseBody = actionHistory.createResponseBody(
            validatedRequest.options,
            warnings,
            errors,
            false
        )

        // checks for errors from createReport
        if (validatedRequest.options != Options.SkipInvalidItems && errors.isNotEmpty()) {
            response = HttpUtilities.httpResponse(
                request,
                responseBody,
                HttpStatus.BAD_REQUEST
            )
            actionHistory.trackActionResult(response)
            actionHistory.trackActionResponse(response, responseBody)
            workflowEngine.recordAction(actionHistory)
        }
        // if no errors resulting in a bad request, move forward with processing
        else {
            if (report != null) {
                // if we are processing a message asynchronously, the report's next action will be 'process'
                // this is used in the 'recordReceivedReport' function when entering the Task record
                if (isAsync) {
                    report.nextAction = TaskAction.process
                }

                report.bodyURL = workflowEngine.recordReceivedReport(
                    // should make createReport always return a report or error
                    report, validatedRequest.content.toByteArray(), sender,
                    actionHistory, workflowEngine
                )

                // call the correct processing function based on processing type
                if (isAsync) {
                    processAsync(
                        report,
                        workflowEngine,
                        validatedRequest.options,
                        validatedRequest.defaults,
                        validatedRequest.routeTo,
                        actionHistory
                    )
                } else {
                    workflowEngine.routeReport(
                        context,
                        report,
                        validatedRequest.options,
                        validatedRequest.defaults,
                        validatedRequest.routeTo,
                        warnings,
                        actionHistory
                    )
                }
                responseBody = actionHistory.createResponseBody(
                    validatedRequest.options,
                    warnings,
                    errors,
                    validatedRequest.verbose,
                    report
                )
            }
            response = HttpUtilities.createdResponse(request, responseBody)
            actionHistory.trackActionResult(response)
            actionHistory.trackActionResponse(response, responseBody)
            workflowEngine.recordAction(actionHistory)

            // queue messages here after all task / action records are in
            actionHistory.queueMessages(workflowEngine)
        }
        return response
    }

    /**
     * Extract client header from request headers or query string parameters
     * @param request the http request message from the client
     */
    private fun extractClientHeader(request: HttpRequestMessage<String?>): String {
        // client can be in the header or in the url parameters:
        return request.headers[CLIENT_PARAMETER]
            ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
    }

    // TODO: Make this so that we check sender's configuration if there is no param type, this is blocked by
    //  adding the 'processingType' attribute to a sender
    private fun processingType(request: HttpRequestMessage<String?>): ProcessingType {
        // uppercase first char, so it matches enum when 'async' is passed in
        val processingTypeString = request.queryParameters.getOrDefault(PROCESSING_TYPE_PARAMETER, "Sync")
            .replaceFirstChar { it.uppercase() }
        val processingType = try {
            ProcessingType.valueOf(processingTypeString)
        } catch (e: IllegalArgumentException) {
            ProcessingType.Sync
        }
        return processingType
    }

    private fun processAsync(
        report: Report,
        workflowEngine: WorkflowEngine,
        options: Options,
        defaults: Map<String, String>,
        routeTo: List<String>,
        actionHistory: ActionHistory
    ) {
        // add 'Process' queue event to the actionHistory
        val processEvent = ProcessEvent(Event.EventAction.PROCESS, report.id, options, defaults, routeTo)
        actionHistory.trackEvent(processEvent)

        // add task to task table
        workflowEngine.insertProcessTask(report, report.bodyFormat.toString(), report.bodyURL, processEvent)
    }

    private fun handleValidation(
        validatedRequest: ValidatedRequest,
        request: HttpRequestMessage<String?>,
        actionHistory: ActionHistory,
        workflowEngine: WorkflowEngine
    ): HttpResponseMessage? {
        var response: HttpResponseMessage? = when {
            !validatedRequest.valid -> {
                HttpUtilities.httpResponse(
                    request,
                    actionHistory.createResponseBody(
                        validatedRequest.options,
                        validatedRequest.warnings,
                        validatedRequest.errors,
                        false
                    ),
                    validatedRequest.httpStatus
                )
            }
            validatedRequest.options == Options.CheckConnections -> {
                workflowEngine.checkConnections()
                HttpUtilities.okResponse(
                    request,
                    actionHistory.createResponseBody(
                        validatedRequest.options,
                        validatedRequest.warnings,
                        validatedRequest.errors,
                        false
                    )
                )
            }
            // is this meant to happen after the creation of a report?
            validatedRequest.options == Options.ValidatePayload -> {
                HttpUtilities.okResponse(
                    request,
                    actionHistory.createResponseBody(
                        validatedRequest.options,
                        validatedRequest.warnings,
                        validatedRequest.errors,
                        false
                    )
                )
            }
            else -> null
        }

        if (response != null) {
            actionHistory.trackActionResult(response)
            actionHistory.trackActionResponse(
                response,
                actionHistory.createResponseBody(
                    validatedRequest.options,
                    validatedRequest.warnings,
                    validatedRequest.errors,
                    false
                )
            )
            workflowEngine.recordAction(actionHistory)
        }

        return response
    }

    private fun validateRequest(engine: WorkflowEngine, request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()
        val (sizeStatus, errMsg) = HttpUtilities.payloadSizeCheck(request)
        if (sizeStatus != HttpStatus.OK) {
            errors.add(ResultDetail.report(InvalidReportMessage.new(errMsg)))
            // If size is too big, we ignore the option.
            return ValidatedRequest(false, sizeStatus, errors, warnings)
        }

        val optionsText = request.queryParameters.getOrDefault(OPTION_PARAMETER, "")
        val options = if (optionsText.isNotBlank()) {
            try {
                Options.valueOf(optionsText)
            } catch (e: IllegalArgumentException) {
                errors.add(ResultDetail.param(OPTION_PARAMETER, InvalidParamMessage.new("'$optionsText' is not valid")))
                Options.None
            }
        } else {
            Options.None
        }

        if (options == Options.CheckConnections) {
            return ValidatedRequest(false, HttpStatus.OK, errors, warnings, options = options)
        }

        val receiverNamesText = request.queryParameters.getOrDefault(ROUTE_TO_PARAMETER, "")
        val routeTo = if (receiverNamesText.isNotBlank()) receiverNamesText.split(ROUTE_TO_SEPARATOR) else emptyList()
        val receiverNameErrors = routeTo
            .filter { engine.settings.findReceiver(it) == null }
            .map { ResultDetail.param(ROUTE_TO_PARAMETER, InvalidParamMessage.new("Invalid receiver name: $it")) }
        errors.addAll(receiverNameErrors)

        val clientName = extractClientHeader(request)
        if (clientName.isBlank())
            errors.add(
                ResultDetail.param(
                    CLIENT_PARAMETER, InvalidParamMessage.new("Expected a '$CLIENT_PARAMETER' query parameter")
                )
            )

        val sender = engine.settings.findSender(clientName)
        if (sender == null)
            errors.add(
                ResultDetail.param(
                    CLIENT_PARAMETER, InvalidParamMessage.new("'$CLIENT_PARAMETER:$clientName': unknown sender")
                )
            )

        val schema = engine.metadata.findSchema(sender?.schemaName ?: "")
        if (sender != null && schema == null)
            errors.add(
                ResultDetail.param(
                    CLIENT_PARAMETER,
                    InvalidParamMessage.new(
                        "'$CLIENT_PARAMETER:$clientName': unknown schema '${sender.schemaName}'"
                    )
                )
            )

        // extract the verbose param and default to empty if not present
        val verboseParam = request.queryParameters.getOrDefault(VERBOSE_PARAMETER, "")
        val verbose = verboseParam.equals(VERBOSE_TRUE, true)

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.lowercase(), "")
        if (contentType.isBlank()) {
            errors.add(ResultDetail.param(HttpHeaders.CONTENT_TYPE, InvalidParamMessage.new("missing")))
        } else if (sender != null && sender.format.mimeType != contentType) {
            errors.add(
                ResultDetail.param(
                    HttpHeaders.CONTENT_TYPE, InvalidParamMessage.new("expecting '${sender.format.mimeType}'")
                )
            )
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            errors.add(ResultDetail.param("Content", InvalidParamMessage.new("expecting a post message with content")))
        }

        if (sender == null || schema == null || content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(false, HttpStatus.BAD_REQUEST, errors, warnings)
        }

        val defaultValues = if (request.queryParameters.containsKey(DEFAULT_PARAMETER)) {
            val values = request.queryParameters.getOrDefault(DEFAULT_PARAMETER, "").split(",")
            values.mapNotNull {
                val parts = it.split(DEFAULT_SEPARATOR)
                if (parts.size != 2) {
                    errors.add(ResultDetail.report(InvalidReportMessage.new("'$it' is not a valid default")))
                    return@mapNotNull null
                }
                val element = schema.findElement(parts[0])
                if (element == null) {
                    errors.add(
                        ResultDetail.report(InvalidReportMessage.new("'${parts[0]}' is not a valid element name"))
                    )
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
            return ValidatedRequest(false, HttpStatus.BAD_REQUEST, errors, warnings)
        }

        return ValidatedRequest(
            true,
            HttpStatus.OK,
            errors,
            warnings,
            content,
            options,
            defaultValues,
            routeTo,
            sender,
            verbose
        )
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
            receiver.format.isSingleItemFormat -> {
                report
                    .split()
                    .forEach {
                        val event = ReportEvent(Event.EventAction.SEND, it.id)
                        workflowEngine.dispatchReport(event, it, actionHistory, receiver, txn, context)
                    }
                loggerMsg = "Queued to send immediately: Split into ${report.itemCount} individual reports"
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
                    it.writeStringField("details", error.responseMessage.detailMsg())
                    it.writeEndObject()
                }
                it.writeEndArray()
            }
            writeDetailsArray("errors", result.errors)
            writeDetailsArray("warnings", result.warnings)

            fun createRowsDescription(rows: MutableList<Int>?): String {
                // Consolidate row ranges, e.g. 1,2,3,5,7,8,9 -> 1-3,5,7-9
                if (rows == null || rows.isEmpty()) return ""
                rows.sort() // should already be sorted, just in case
                val sb = StringBuilder().append("Rows: ")
                var isListing = false
                rows.forEachIndexed { i, row ->
                    if (i == 0) {
                        sb.append(row.toString())
                    } else if (row == rows[i - 1] || row == rows[i - 1] + 1) {
                        isListing = true
                    } else if (isListing) {
                        sb.append(" to " + rows[i - 1].toString() + ", " + row.toString())
                        isListing = false
                    } else {
                        sb.append(", " + row.toString())
                        isListing = false
                    }
                    if (i == rows.lastIndex && isListing) {
                        sb.append(" to " + rows[rows.lastIndex].toString())
                    }
                }
                return sb.toString()
            }

            fun writeConsolidatedArray(field: String, array: List<ResultDetail>) {
                val rowsByGroupingId = hashMapOf<String, MutableList<Int>>()
                val messageByGroupingId = hashMapOf<String, String>()
                array.forEach { resultDetail ->
                    val groupingId = resultDetail.responseMessage.groupingId()
                    if (!rowsByGroupingId.containsKey(groupingId)) {
                        rowsByGroupingId[groupingId] = mutableListOf()
                        messageByGroupingId[groupingId] = resultDetail.responseMessage.detailMsg()
                    }
                    if (resultDetail.row != -1) {
                        // Add 2 to account for array offset and csv header
                        rowsByGroupingId[groupingId]?.add(resultDetail.row + 1)
                    }
                }
                it.writeArrayFieldStart(field)
                rowsByGroupingId.keys.forEach { groupingId ->
                    it.writeStartObject()
                    it.writeStringField("message", messageByGroupingId[groupingId])
                    it.writeStringField("rows", createRowsDescription(rowsByGroupingId[groupingId]))
                    it.writeEndObject()
                }
                it.writeEndArray()
            }
            writeConsolidatedArray("consolidatedErrors", result.errors)
            writeConsolidatedArray("consolidatedWarnings", result.warnings)
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