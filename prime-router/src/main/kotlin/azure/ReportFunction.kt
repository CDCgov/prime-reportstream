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
import gov.cdc.prime.router.ResultError
import gov.cdc.prime.router.ResultErrors
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
        val content: String = "",
        val defaults: Map<String, String> = emptyMap(),
        val routeTo: List<String> = emptyList(),
        val sender: Sender,
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
        val responseBuilder = request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
        try {
            val optionsText = request.queryParameters.getOrDefault(OPTION_PARAMETER, "None")
            val options = Options.valueOf(optionsText)

            // extract the verbose param and default to empty if not present
            val verboseParam = request.queryParameters.getOrDefault(VERBOSE_PARAMETER, "")
            val verbose = verboseParam.equals(VERBOSE_TRUE, true)
            // track the sending organization and client based on the header
            try {
                val validatedRequest = validateRequest(workflowEngine, request)
                actionHistory.trackActionSender(validatedRequest.sender.fullName)
                when (options) {
                    Options.CheckConnections, Options.ValidatePayload -> {
                        responseBuilder.body(
                            actionHistory.createResponseBody(
                                options,
                                emptyList(),
                                emptyList(),
                                false,
                            )
                        ).status(HttpStatus.OK)
                    }
                    else -> {
                        val (report, readErrors, readWarnings) = workflowEngine.createReport(
                            sender,
                            validatedRequest.content,
                            validatedRequest.defaults,
                        )

                        // checks for errors from createReport
                        if (options != Options.SkipInvalidItems && readErrors.isNotEmpty()) {
                            throw ResultErrors(readErrors)
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
                                options,
                                validatedRequest.defaults,
                                validatedRequest.routeTo,
                                actionHistory
                            )
                        } else {
                            workflowEngine.routeReport(
                                context,
                                report,
                                options,
                                validatedRequest.defaults,
                                validatedRequest.routeTo,
                                warnings,
                                actionHistory
                            )
                        }

                        // TODO DG: continue reducing this passing of mutable lists
                        errors += readErrors
                        warnings += readWarnings
                        responseBuilder.body(
                            actionHistory.createResponseBody(
                                // TODO DG: get rid of the need for options, only used to handle "skip send" case
                                // to tell them that it's never sent.
                                options,
                                warnings,
                                errors,
                                verbose,
                                report
                            )
                        )
                        responseBuilder.status(HttpStatus.CREATED)
                    }
                }
            } catch (e: ResultError) {
                responseBuilder.body(
                    actionHistory.createResponseBody(
                        options,
                        warnings,
                        listOf(e.detail),
                        verbose,
                    )
                )
            } catch (e: ResultErrors) {
                responseBuilder.body(
                    actionHistory.createResponseBody(
                        options,
                        warnings,
                        e.details,
                        verbose,
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            responseBuilder.body(e.message ?: "Invalid request.")
        } catch (e: IllegalStateException) {
            responseBuilder.body(e.message ?: "Invalid request.")
        }

        val response = responseBuilder.build()
        actionHistory.trackActionResult(response)
        actionHistory.trackActionResponse(response, response.body.toString())
        workflowEngine.recordAction(actionHistory)

        // queue messages here after all task / action records are in
        actionHistory.queueMessages(workflowEngine)
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
        parsedReport: Report,
        workflowEngine: WorkflowEngine,
        options: Options,
        defaults: Map<String, String>,
        routeTo: List<String>,
        actionHistory: ActionHistory
    ) {

        val report = parsedReport.copy()

        if (report.bodyFormat != Report.Format.INTERNAL) {
            error("Processing a non internal report async.")
        }

        val processEvent = ProcessEvent(Event.EventAction.PROCESS, report.id, options, defaults, routeTo)

        val blobInfo = workflowEngine.blob.uploadBody(report, action = processEvent.eventAction)

        actionHistory.trackCreatedReport(processEvent, report, blobInfo)
        // add task to task table
        workflowEngine.insertProcessTask(report, report.bodyFormat.toString(), blobInfo.blobUrl, processEvent)
    }

    private fun validateRequest(engine: WorkflowEngine, request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<ResultDetail>()
        HttpUtilities.payloadSizeCheck(request)

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
            throw ResultErrors(errors)
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
            throw ResultErrors(errors)
        }

        return ValidatedRequest(
            content,
            defaultValues,
            routeTo,
            sender,
        )
    }
}