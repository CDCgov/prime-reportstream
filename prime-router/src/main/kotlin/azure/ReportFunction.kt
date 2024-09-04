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
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Sender.ProcessingType
import gov.cdc.prime.router.SubmissionReceiver
import gov.cdc.prime.router.UniversalPipelineReceiver
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.cli.PIIRemovalCommands
import gov.cdc.prime.router.common.AzureHttpUtils.getSenderIP
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.history.azure.SubmissionsFacade
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging
import java.util.UUID

private const val PROCESSING_TYPE_PARAMETER = "processing"

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.receive),
    private val reportEventService: IReportStreamEventService = ReportStreamEventService(
        workflowEngine.db,
        workflowEngine.azureEventService,
        workflowEngine.reportService
    ),
) : RequestFunction(workflowEngine),
    Logging {

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
        if (senderName.isBlank()) {
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")
        }

        // Sender should eventually be obtained directly from who is authenticated
        val sender = workflowEngine.settings.findSender(senderName)
            ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")
        actionHistory.trackActionParams(request)

        return try {
            processRequest(request, sender)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    /**
     * GET report to download
     *
     * @see ../../../docs/api/reports.yml
     */
    @FunctionName("getMessagesFromTestBank")
    fun downloadReport(
        @HttpTrigger(
            name = "downloadReport",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "reports/download"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val reportId = request.queryParameters[REPORT_ID_PARAMETER]
        val removePIIRaw = request.queryParameters[REMOVE_PII]
        var removePII = false
        if (removePIIRaw.isNullOrBlank() || removePIIRaw.toBoolean()) {
            removePII = true
        }
        if (reportId.isNullOrBlank()) {
            return HttpUtilities.badRequestResponse(request, "Must provide a reportId.")
        }
        return processDownloadReport(
            request,
            ReportId.fromString(reportId),
            removePII,
            Environment.get().envName
        )
    }

    fun processDownloadReport(
        request: HttpRequestMessage<String?>,
        reportId: UUID,
        removePII: Boolean?,
        envName: String,
        databaseAccess: DatabaseAccess = DatabaseAccess(),
    ): HttpResponseMessage {
        val requestedReport = databaseAccess.fetchReportFile(reportId)

        return if (requestedReport.bodyUrl != null && requestedReport.bodyUrl.toString().lowercase().endsWith("fhir")) {
            val contents = BlobAccess.downloadBlobAsByteArray(requestedReport.bodyUrl)

            val content = if (removePII == null || removePII) {
                PIIRemovalCommands().removePii(FhirTranscoder.decode(contents.toString(Charsets.UTF_8)))
            } else {
                if (envName == "prod") {
                    return HttpUtilities.badRequestResponse(request, "Must remove PII for messages from prod.")
                }

                val jsonObject = JacksonMapperUtilities.defaultMapper
                    .readValue(contents.toString(Charsets.UTF_8), Any::class.java)
                JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)
            }

            HttpUtilities.okJSONResponse(request, content)
        } else if (requestedReport.bodyUrl == null) {
            HttpUtilities.badRequestResponse(request, "The requested report does not exist.")
        } else {
            HttpUtilities.badRequestResponse(request, "The requested report is not fhir.")
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
        if (senderName.isBlank()) {
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")
        }

        actionHistory.trackActionParams(request)
        try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            val sender = workflowEngine.settings.findSender(senderName)
                ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown client")

            if (!claims.authorizedForSendOrReceive(sender, request)) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            return processRequest(request, sender)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
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
                val option = Options.valueOfOrNone(optionsText)
                if (option.isDeprecated) {
                    actionHistory.trackLogs(
                        ActionLog(
                            InvalidParamMessage(
                                "Url Options Parameter, $optionsText has been deprecated. " +
                                    "Valid options: ${Options.activeValues.joinToString()}"
                            ),
                            type = ActionLogLevel.warning
                        )
                    )
                }
                val payloadName = extractPayloadName(request)
                // track the sending organization and client based on the header
                actionHistory.trackActionSenderInfo(sender.fullName, payloadName)
                val validatedRequest = validateRequest(request)

                // if the override parameter is populated, use that, otherwise use the sender value
                val allowDuplicates =
                    if (!allowDuplicatesParam.isNullOrEmpty()) {
                        allowDuplicatesParam == "true"
                    } else {
                        sender.allowDuplicates
                    }

                // Only process the report if we are not checking for connection or validation.
                if (option != Options.CheckConnections && option != Options.ValidatePayload) {
                    val receiver = SubmissionReceiver.getSubmissionReceiver(sender, workflowEngine, actionHistory)
                    val content =
                        if (receiver is UniversalPipelineReceiver) {
                            validatedRequest.content
                        } // removes incoming '#' if included in separation characters
                        else {
                            validatedRequest.content.replace("|^~\\&#", "|^~\\&")
                        }
                    val rawBody = content.toByteArray()
                    // send report on its way, either via the COVID pipeline or the full ELR pipeline
                    val report = receiver.validateAndMoveToProcessing(
                        sender,
                        content,
                        validatedRequest.defaults,
                        option,
                        validatedRequest.routeTo,
                        isAsync,
                        allowDuplicates,
                        rawBody,
                        payloadName
                    )

                    reportEventService.sendReportEvent(
                        eventName = ReportStreamEventName.REPORT_RECEIVED,
                        childReport = report,
                        pipelineStepName = TaskAction.receive
                    ) {
                        params(
                            listOfNotNull(
                                ReportStreamEventProperties.REQUEST_PARAMETERS
                                    to actionHistory.filterParameters(request),
                                ReportStreamEventProperties.SENDER_NAME to sender.fullName,
                                ReportStreamEventProperties.FILE_LENGTH to request.headers["content-length"].toString(),
                                getSenderIP(request)?.let { ReportStreamEventProperties.SENDER_IP to it }
                            ).toMap()
                        )
                    }

                    // return CREATED status, report submission was successful
                    HttpStatus.CREATED
                } else {
                    HttpStatus.OK
                }
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
            } catch (e: Options.InvalidOptionException) {
                actionHistory.trackLogs(
                    ActionLog(InvalidParamMessage(e.message ?: "Invalid request."), type = ActionLogLevel.error)
                )
                HttpStatus.BAD_REQUEST
            }

        actionHistory.trackActionResult(httpStatus)
        workflowEngine.recordAction(actionHistory)

        check(actionHistory.action.actionId > 0)
        val submission = workflowEngine.db.transactReturning { txn ->
            SubmissionsFacade.instance.findDetailedSubmissionHistory(txn, null, actionHistory.action)
        }

        val response = request.createResponseBuilder(httpStatus)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                JacksonMapperUtilities.allowUnknownsMapper
                    .writeValueAsString(submission)
            )
            .header(
                HttpHeaders.LOCATION,
                request.uri.resolve(
                    "/api/waters/report/${submission?.reportId}/history"
                ).toString()
            )
            .build()

        // queue messages here after all task / action records are in
        actionHistory.queueMessages(workflowEngine)

        return response
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
}