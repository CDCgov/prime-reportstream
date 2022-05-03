package gov.cdc.prime.router.azure

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
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.DEFAULT_SEPARATOR
import gov.cdc.prime.router.DuplicateItemMessage
import gov.cdc.prime.router.DuplicateSubmissionMessage
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ROUTE_TO_SEPARATOR
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Sender.ProcessingType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.azure.fhirProcessQueueName
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging

private const val CLIENT_PARAMETER = "client"
private const val PAYLOAD_NAME_PARAMETER = "payloadname"
private const val OPTION_PARAMETER = "option"
private const val DEFAULT_PARAMETER = "default"
private const val ROUTE_TO_PARAMETER = "routeTo"
private const val ALLOW_DUPLICATES_PARAMETER = "allowDuplicate"
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
     * @see ../../../docs/api/reports.yml
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
        if (senderName.isBlank())
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
    fun submitToWaters(
        @HttpTrigger(
            name = "waters",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val senderName = extractClient(request)
        if (senderName.isBlank())
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")

        actionHistory.trackActionParams(request)
        try {
            val claims = AuthenticationStrategy.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            val sender = workflowEngine.settings.findSender(senderName)
                ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown client")

            // Do authorization based on org name in claim matching org name in client header
            if ((claims.organizationNameClaim != sender.organizationName) && !claims.isPrimeAdmin) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claim org is ${claims.organizationNameClaim} but client id is ${sender.organizationName}"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.organizationNameClaim}" +
                    " to submit data via client id ${sender.organizationName}.  Beginning to ingest report"
            )
            return processRequest(request, sender)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
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
        // allow duplicates 'override' param
        val allowDuplicatesParam = request.queryParameters.getOrDefault(ALLOW_DUPLICATES_PARAMETER, null)
        val optionsText = request.queryParameters.getOrDefault(OPTION_PARAMETER, "None")
        val httpStatus: HttpStatus =
            try {
                val options = Options.valueOfOrNone(optionsText)
                val payloadName = extractPayloadName(request)
                // track the sending organization and client based on the header
                actionHistory.trackActionSenderInfo(sender.fullName, payloadName)
                val validatedRequest = validateRequest(request)
                val rawBody = validatedRequest.content.toByteArray()

                // if the override parameter is populated, use that, otherwise use the sender value
                val allowDuplicates = if
                (!allowDuplicatesParam.isNullOrEmpty()) allowDuplicatesParam == "true"
                else
                    sender.allowDuplicates

                // Only process the report if we are not checking for connection or validation.
                if (options != Options.CheckConnections && options != Options.ValidatePayload) {
                    // TODO: Decision point - we will need to parse non-covid reports and only call parseCovidReport
                    //  once we have verified the sender is a covid-specific sender
                    // TODO: full ELR, See #5050
                    val (report, actionLogs) = workflowEngine.parseCovidReport(
                        // TODO: when the full ELR stuff is refactored out, ideally we would already know we
                        //  have a CovidSender before reaching the point of parseCovidReport
                        sender as CovidSender,
                        validatedRequest.content,
                        validatedRequest.defaults,
                    )

                    // prevent duplicates if configured to not allow them
                    if (!allowDuplicates) {
                        doDuplicateDetection(
                            report,
                            actionLogs
                        )
                    }

                    val receivedBlobInfo = workflowEngine.recordReceivedReport(
                        report, rawBody, sender, actionHistory, payloadName
                    )

                    // if there are any errors, kick this out.
                    if (actionLogs.hasErrors()) {
                        throw actionLogs.exception
                    }

                    // Places a message on a queue for async testing of the fhir engine
                    // Only triggers if a feature flag is enabled
                    routeToFHIREngine(receivedBlobInfo, sender, workflowEngine.queue)

                    actionHistory.trackLogs(actionLogs.logs)

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

                    HttpStatus.CREATED
                } else HttpStatus.OK
            } catch (e: ActionError) {
                actionHistory.trackLogs(e.details)
                HttpStatus.BAD_REQUEST
            } catch (e: IllegalArgumentException) {
                actionHistory.trackLogs(
                    ActionLog(InvalidReportMessage(e.message ?: "Invalid request."), type = ActionLogLevel.error)
                )
                HttpStatus.BAD_REQUEST
            } catch (e: IllegalStateException) {
                actionHistory.trackLogs(
                    ActionLog(InvalidReportMessage(e.message ?: "Invalid request."), type = ActionLogLevel.error)
                )
                HttpStatus.BAD_REQUEST
            }

        actionHistory.trackActionResult(httpStatus)
        workflowEngine.recordAction(actionHistory)

        check(actionHistory.action.actionId > 0)
        val submission = SubmissionsFacade.instance.findDetailedSubmissionHistory(
            actionHistory.action.sendingOrg,
            actionHistory.action.actionId
        )
        val response = request.createResponseBuilder(httpStatus)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                JacksonMapperUtilities.allowUnknownsMapper
                    .writeValueAsString(submission)
            )
            .header(
                HttpHeaders.LOCATION,
                request.uri.resolve(
                    "/api/history/${sender.organizationName}/submissions/${actionHistory.action.actionId}"
                ).toString()
            )
            .build()

        // queue messages here after all task / action records are in
        actionHistory.queueMessages(workflowEngine)

        return response
    }

    /**
     * Checks the [report] rows looking for duplicates, and adds errors as needed
     */
    internal fun doDuplicateDetection(
        report: Report,
        actionLogs: ActionLogger
    ) {
        // keep list of hashes generated by this ingest to do same-file comparison
        val generatedHashes = mutableListOf<String>()
        val duplicateIndexes = mutableListOf<Int>()
        for (rowNum in 0 until report.itemCount) {
            val itemHash = report.getItemHashForRow(rowNum)
            // check for duplicate item
            val isDuplicate = generatedHashes.contains(itemHash) ||
                workflowEngine.isDuplicateItem(itemHash)
            if (isDuplicate) {
                duplicateIndexes.add(rowNum + 1)
            } else {
                generatedHashes.add(itemHash)
            }
        }
        // TODO Tech Debt: We need a more consistent way to handle 'trackingId'. Once we have one, we can
        //  pass it in here to ensure a trackingId is returned in the response message
        // sanity check - if all items are duplicate
        if (duplicateIndexes.size == report.itemCount) {
            addDuplicateLogs(actionLogs, null, null, null)
        }
        // add logs as needed
        else {
            duplicateIndexes.forEach {
                addDuplicateLogs(actionLogs, null, it, null)
            }
        }
    }

    /**
     * If a [rowNum] is passed, a DuplicateItemMessage with [message] is added to [actionLogs] as an error.
     * If no [rowNum] is passed, a DuplicateFileMessage with [message] is added to [actionLogs] as an error.
     * If a [trackingId] is passed, it will be used in the response message
     */
    internal fun addDuplicateLogs(
        actionLogs: ActionLogger,
        payloadName: String?,
        rowNum: Int?,
        trackingId: String?
    ) {
        if (rowNum != null) {
            actionLogs.getItemLogger(rowNum, trackingId).error(DuplicateItemMessage())
        } else {
            // duplicate files are always an error, never a warning
            actionLogs.error(DuplicateSubmissionMessage(payloadName))
        }
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
                            fhirProcessQueueName,
                            RawSubmission(
                                blobInfo.blobUrl,
                                BlobAccess.digestToString(blobInfo.digest),
                                sender.fullName
                            ).serialize()
                        )
                    else -> {}
                }
        } catch (t: Throwable) {
            logger.error("Failed to queue message for FHIR Engine: No action required during testing phase\n$t")
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
        val processingTypeString = request.queryParameters[PROCESSING_TYPE_PARAMETER]
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
        val actionLogs = ActionLogger()
        HttpUtilities.payloadSizeCheck(request)

        val receiverNamesText = request.queryParameters.getOrDefault(ROUTE_TO_PARAMETER, "")
        val routeTo = if (receiverNamesText.isNotBlank()) receiverNamesText.split(ROUTE_TO_SEPARATOR) else emptyList()
        routeTo.filter { workflowEngine.settings.findReceiver(it) == null }
            .forEach { actionLogs.error(InvalidParamMessage("Invalid receiver name: $it")) }

        val clientName = extractClient(request)
        if (clientName.isBlank())
            actionLogs.error(InvalidParamMessage("Expected a '$CLIENT_PARAMETER' query parameter"))

        val sender = workflowEngine.settings.findSender(clientName)
        if (sender == null)
            actionLogs.error(InvalidParamMessage("'$CLIENT_PARAMETER:$clientName': unknown sender"))

        // verify schema if the sender is a covidSender
        var schema: Schema? = null
        if (sender != null && sender is CovidSender) {
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
            actionLogs.error(InvalidParamMessage("Expecting a post message with content"))
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

                // only CovidSenders will have a schema
                if (sender is CovidSender && schema != null) {
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
        )
    }
}