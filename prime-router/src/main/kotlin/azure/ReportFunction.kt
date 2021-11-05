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
import org.postgresql.util.PSQLException

private const val CLIENT_PARAMETER = "client"
private const val OPTION_PARAMETER = "option"
private const val DEFAULT_PARAMETER = "default"
private const val ROUTE_TO_PARAMETER = "routeTo"
private const val VERBOSE_PARAMETER = "verbose"
private const val VERBOSE_TRUE = "true"
private const val PROCESSING_TYPE = "processing"

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction : Logging {

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

        // if we are processing a message asynchronously, the report's next action will be 'process'
        if (isAsync && report != null) {
            report.nextAction = TaskAction.process
        }

        // checks for errors from createReport
        if (validatedRequest.options != Options.SkipInvalidItems && errors.isNotEmpty()) {
            val responseBody = actionHistory.createResponseBody(
                validatedRequest.options,
                warnings,
                errors,
                false
            )
            val response = HttpUtilities.httpResponse(
                request,
                responseBody,
                HttpStatus.BAD_REQUEST
            )
            actionHistory.trackActionResult(response)
            actionHistory.trackActionResponse(response, responseBody)
            workflowEngine.recordAction(actionHistory)
            return response
        }

        workflowEngine.recordReceivedReport(
            // should make createReport always return a report or error
            report!!, validatedRequest.content.toByteArray(), sender,
            actionHistory, workflowEngine
        )

        // this function call checks internally to verify that this is a covid-19 topic
        // Write the data to the table if we're dealing with covid-19. this has to happen
        // here AFTER we've written the report to the DB.
        writeCovidResultMetadataForReport(report, context, workflowEngine)

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
        val responseBody = actionHistory.createResponseBody(
            validatedRequest.options,
            warnings,
            errors,
            validatedRequest.verbose,
            actionHistory,
            report
        )
        val response = HttpUtilities.createdResponse(request, responseBody)
        actionHistory.trackActionResult(response)
        actionHistory.trackActionResponse(response, responseBody)
        workflowEngine.recordAction(actionHistory)

        // queue messages here after all task / action records are in
        actionHistory.queueMessages(workflowEngine)

        return response
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
        // client can be in the header or in the url parameters:
        return request.headers[CLIENT_PARAMETER]
            ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
    }

    // TODO: Make this so that we check sender's configuration if there is no param type, this is blocked by
    //  adding the 'processingType' attribute to a sender
    private fun processingType(request: HttpRequestMessage<String?>): ProcessingType {
        // uppercase first char, so it matches enum when 'async' is passed in
        val processingTypeString = request.queryParameters.getOrDefault(PROCESSING_TYPE, "Sync")
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

        val defaultValues = if (request.queryParameters.containsKey(DEFAULT_PARAMETER)) {
            val values = request.queryParameters.getOrDefault(DEFAULT_PARAMETER, "").split(",")
            values.mapNotNull {
                val parts = it.split(DEFAULT_SEPARATOR)
                if (parts.size != 2) {
                    errors.add(ResultDetail.report(InvalidReportMessage.new("'$it' is not a valid default")))
                    return@mapNotNull null
                }
                val element = schema!!.findElement(parts[0])
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

        if (sender == null || schema == null || content.isEmpty() || errors.isNotEmpty()) {
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
}