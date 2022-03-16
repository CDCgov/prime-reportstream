package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.DEFAULT_SEPARATOR
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ROUTE_TO_SEPARATOR
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Sender.ProcessingType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.engine.RawSubmission
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TokenAuthentication
import org.apache.logging.log4j.kotlin.Logging

private const val CLIENT_PARAMETER = "client"
private const val PAYLOAD_NAME_PARAMETER = "payloadname"
private const val OPTION_PARAMETER = "option"
private const val DEFAULT_PARAMETER = "default"
private const val ROUTE_TO_PARAMETER = "routeTo"
private const val VERBOSE_PARAMETER = "verbose"
private const val ALLOW_DUPLICATES_PARAMETER = "allowDuplicate"
private const val VERBOSE_TRUE = "true"
private const val PROCESSING_TYPE_PARAMETER = "processing"

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.receive)
) : Logging {

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
    ): HttpResponseMessage {
        val senderName = extractClient(request)
        if (senderName.isNullOrBlank())
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")

        // Sender should eventually be obtained directly from who is authenticated
        val sender = workflowEngine.settings.findSender(senderName)
            ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")
        actionHistory.trackActionParams(request)

        return try {
            processRequest(request, sender)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            HttpUtilities.internalErrorResponse(request)
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
    ): HttpResponseMessage {
        val senderName = extractClient(request)
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
            actionHistory.trackActionParams(request)

            if (authenticationStrategy is OktaAuthentication) {
                // The report is coming from a sender that is using Okta, so set "oktaSender" to true
                return authenticationStrategy.checkAccess(request, senderName, true, actionHistory) {
                    return@checkAccess processRequest(request, sender)
                }
            }

            if (authenticationStrategy is TokenAuthentication) {
                val claims = authenticationStrategy.checkAccessToken(request, "${sender.fullName}.report")
                    ?: return HttpUtilities.unauthorizedResponse(request)
                logger.info("Claims for ${claims["sub"]} validated.  Beginning ingestReport.")
                return processRequest(request, sender)
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
     * @return Returns an HttpResponseMessage indicating the result of the operation and any resulting information
     */
    internal fun processRequest(
        request: HttpRequestMessage<String?>,
        sender: Sender,
    ): HttpResponseMessage {
        // determine if we should be following the sync or async workflow
        val isAsync = processingType(request, sender) == ProcessingType.async
        val responseBuilder = request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
        // extract the verbose param and default to empty if not present
        val verboseParam = request.queryParameters.getOrDefault(VERBOSE_PARAMETER, "")
        // allow duplicates 'override' param
        val allowDuplicatesParam = request.queryParameters.getOrDefault(ALLOW_DUPLICATES_PARAMETER, null)
        val verbose = verboseParam.equals(VERBOSE_TRUE, true)

        val optionsText = request.queryParameters.getOrDefault(OPTION_PARAMETER, "None")
        val options = Options.valueOf(optionsText)

        val report = try {
            // track the sending organization and client based on the header
            val validatedRequest = validateRequest(request)
            val rawBody = validatedRequest.content.toByteArray()
            val payloadName = extractPayloadName(request)

            // if the override parameter is populated, use that, otherwise use the sender value
            val allowDuplicates = if
            (!allowDuplicatesParam.isNullOrEmpty()) allowDuplicatesParam == "true"
            else
                sender.allowDuplicates

            // check if we are preventing duplicate files from the sender
            if (!allowDuplicates) {
                // TODO this should be only calculated once and passed, but the underlying functions are called by
                //  receive (this), process, batch, send and those do *not* need to calculate it outside of the function
                //  so leaving it 2x calculating on receive for the time being.
                val digest = BlobAccess.sha256Digest(rawBody)

                // throws ActionError if there is a duplicate detected
                workflowEngine.verifyNoDuplicateFile(sender, digest, payloadName)
            }

            actionHistory.trackActionSenderInfo(validatedRequest.sender.fullName, payloadName)
            when (options) {
                Options.CheckConnections, Options.ValidatePayload -> {
                    responseBuilder.status(HttpStatus.OK)
                    null
                }
                else -> {
                    val (report, errors, warnings) = workflowEngine.parseReport(
                        sender,
                        validatedRequest.content,
                        validatedRequest.defaults,
                    )

                    val receivedBlobInfo = workflowEngine.recordReceivedReport(
                        report, rawBody, sender, actionHistory, payloadName
                    )

                    // Places a message on a queue for async testing of the fhir engine
                    // Only triggers if a feature flag is enabled
                    routeToFHIREngine(receivedBlobInfo, sender, workflowEngine.queue)

                    // checks for errors from parseReport
                    if (options != Options.SkipInvalidItems && errors.isNotEmpty()) {
                        throw ActionError(errors)
                    }

                    actionHistory.trackLogs(errors)
                    actionHistory.trackLogs(warnings)

                    // call the correct processing function based on processing type
                    if (isAsync) {
                        processAsync(
                            report,
                            options,
                            validatedRequest.defaults,
                            validatedRequest.routeTo
                        )
                    } else {
                        val routingWarnings = workflowEngine.routeReport(
                            report,
                            options,
                            validatedRequest.defaults,
                            validatedRequest.routeTo,
                            actionHistory
                        )
                        actionHistory.trackLogs(routingWarnings)
                    }

                    responseBuilder.status(HttpStatus.CREATED)
                    report
                }
            }
        } catch (e: ActionError) {
            actionHistory.trackLogs(e.details)
            null
        } catch (e: IllegalArgumentException) {
            actionHistory.trackLogs(
                ActionLog.report(
                    e.message ?: "Invalid request.", ActionLog.ActionLogType.error
                )
            )
            null
        } catch (e: IllegalStateException) {
            actionHistory.trackLogs(
                ActionLog.report(
                    e.message ?: "Invalid request.", ActionLog.ActionLogType.error
                )
            )
            null
        }

        responseBuilder.body(
            actionHistory.createResponseBody(
                verbose,
                report,
                workflowEngine.settings
            )
        )
        val response = responseBuilder.build()
        actionHistory.trackActionResult(response)
        actionHistory.trackActionResponse(response, report, workflowEngine.settings)
        workflowEngine.recordAction(actionHistory)

        var bypassMessageQueue = false
        if (options == Options.BypassQueueForQualityFilters) {
            var hasQualityFilters = false
            val tree = jacksonObjectMapper().readTree(response.body.toString())
            val destinations = tree.path("destinations")
            destinations.apply {
                forEach { it ->
                    val filteredReportItems = it.path("filteredReportItems")
                    filteredReportItems.forEach {
                        if (it.path("filterType").textValue() == "QUALITY_FILTER") {
                            hasQualityFilters = true
                            return@apply
                        }
                    }
                }
            }

            if (hasQualityFilters) bypassMessageQueue = true
        }

        // queue messages here after all task / action records are in
        if (!bypassMessageQueue)
            actionHistory.queueMessages(workflowEngine)

        val uri = request.getUri()
        responseBuilder.header(
            HttpHeaders.LOCATION,
            uri.resolve(
                "/api/history/${sender.organizationName}/submissions/${actionHistory.action.actionId}"
            ).toString()
        )
        // TODO: having to build response twice in order to save it and then include a response with the resulting actionID
        return responseBuilder.build()
    }

    /**
     * Put a message on a queue to trigger the FHIR Engine pipeline for testing
     */
    private fun routeToFHIREngine(blobInfo: BlobAccess.BlobInfo, sender: Sender, queue: QueueAccess) {
        try {
            if (Environment.FeatureFlags.FHIR_ENGINE_TEST_PIPELINE.enabled())
                when (blobInfo.format) {
                    // limit to hl7
                    Report.Format.HL7 ->
                        queue.sendMessage(
                            fhirQueueName,
                            RawSubmission(
                                blobInfo.blobUrl,
                                BlobAccess.digestToString(blobInfo.digest),
                                sender.fullName
                            ).serialize()
                        )
                    else -> {}
                }
        } catch (t: Throwable) {
            logger.error("Failed to queue message for FHIR Engine: No action required durring testing phase\n$t")
        }
    }

    /**
     * Extract client header from request headers or query string parameters
     * @param request the http request message from the client
     */
    private fun extractClient(request: HttpRequestMessage<String?>): String {
        // client can be in the header or in the url parameters:
        return request.headers[CLIENT_PARAMETER]
            ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
    }

    /**
     * Extract the optional payloadName (aka sender-supplied filename) from request headers or query string parameters
     * @param request the http request message from the client
     */
    private fun extractPayloadName(request: HttpRequestMessage<String?>): String? {
        // payloadName can be in the header or in the url parameters.  Return null if not found.
        return request.headers[PAYLOAD_NAME_PARAMETER]
            ?: request.queryParameters[PAYLOAD_NAME_PARAMETER]
    }

    private fun processingType(request: HttpRequestMessage<String?>, sender: Sender): ProcessingType {
        val processingTypeString = request.queryParameters.get(PROCESSING_TYPE_PARAMETER)
        return if (processingTypeString == null) {
            sender.processingType
        } else {
            try {
                ProcessingType.valueOfIgnoreCase(processingTypeString)
            } catch (e: IllegalArgumentException) {
                sender.processingType
            }
        }
    }

    private fun processAsync(
        parsedReport: Report,
        options: Options,
        defaults: Map<String, String>,
        routeTo: List<String>
    ) {

        val report = parsedReport.copy()
        val sender = parsedReport.sources.firstOrNull()
            ?: error("Unable to process report ${report.id} because sender sources collection is empty.")
        val senderName = (sender as ClientSource).name

        if (report.bodyFormat != Report.Format.INTERNAL) {
            error("Processing a non internal report async.")
        }

        val processEvent = ProcessEvent(Event.EventAction.PROCESS, report.id, options, defaults, routeTo)

        val blobInfo = workflowEngine.blob.generateBodyAndUploadReport(
            report,
            senderName,
            action = processEvent.eventAction
        )
        actionHistory.trackCreatedReport(processEvent, report, blobInfo)
        // add task to task table
        workflowEngine.insertProcessTask(report, report.bodyFormat.toString(), blobInfo.blobUrl, processEvent)
    }

    internal fun validateRequest(request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<ActionLog>()
        HttpUtilities.payloadSizeCheck(request)

        val receiverNamesText = request.queryParameters.getOrDefault(ROUTE_TO_PARAMETER, "")
        val routeTo = if (receiverNamesText.isNotBlank()) receiverNamesText.split(ROUTE_TO_SEPARATOR) else emptyList()
        val receiverNameErrors = routeTo
            .filter { workflowEngine.settings.findReceiver(it) == null }
            .map { ActionLog.param(ROUTE_TO_PARAMETER, "Invalid receiver name: $it") }
        errors.addAll(receiverNameErrors)

        val clientName = extractClient(request)
        if (clientName.isBlank())
            errors.add(
                ActionLog.param(
                    CLIENT_PARAMETER, "Expected a '$CLIENT_PARAMETER' query parameter"
                )
            )

        val sender = workflowEngine.settings.findSender(clientName)
        if (sender == null)
            errors.add(
                ActionLog.param(
                    CLIENT_PARAMETER, "'$CLIENT_PARAMETER:$clientName': unknown sender"
                )
            )

        val schema = workflowEngine.metadata.findSchema(sender?.schemaName ?: "")
        if (sender != null && schema == null)
            errors.add(
                ActionLog.param(
                    CLIENT_PARAMETER,
                    "'$CLIENT_PARAMETER:$clientName': unknown schema '${sender.schemaName}'"
                )
            )

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.lowercase(), "")
        if (contentType.isBlank()) {
            errors.add(ActionLog.param(HttpHeaders.CONTENT_TYPE, "missing"))
        } else if (sender != null && sender.format.mimeType != contentType) {
            errors.add(
                ActionLog.param(
                    HttpHeaders.CONTENT_TYPE, "expecting '${sender.format.mimeType}'"
                )
            )
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            errors.add(ActionLog.param("Content", "expecting a post message with content"))
        }

        if (sender == null || schema == null || content.isEmpty() || errors.isNotEmpty()) {
            throw ActionError(errors)
        }

        val defaultValues = if (request.queryParameters.containsKey(DEFAULT_PARAMETER)) {
            val values = request.queryParameters.getOrDefault(DEFAULT_PARAMETER, "").split(",")
            values.mapNotNull {
                val parts = it.split(DEFAULT_SEPARATOR)
                if (parts.size != 2) {
                    errors.add(
                        ActionLog.report(
                            "'$it' is not a valid default",
                        )
                    )
                    return@mapNotNull null
                }
                val element = schema.findElement(parts[0])
                if (element == null) {
                    errors.add(
                        ActionLog.report(
                            "'${parts[0]}' is not a valid element name",
                        )
                    )
                    return@mapNotNull null
                }
                val error = element.checkForError(parts[1])
                if (error != null) {
                    errors.add(ActionLog.param(DEFAULT_PARAMETER, error))
                    return@mapNotNull null
                }
                Pair(parts[0], parts[1])
            }.toMap()
        } else {
            emptyMap()
        }

        if (content.isEmpty() || errors.isNotEmpty()) {
            throw ActionError(errors)
        }

        return ValidatedRequest(
            content,
            defaultValues,
            routeTo,
            sender,
        )
    }
}