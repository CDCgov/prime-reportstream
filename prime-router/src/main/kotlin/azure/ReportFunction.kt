package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.ajalt.clikt.core.CliktError
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.BlobTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Sender.ProcessingType
import gov.cdc.prime.router.SubmissionReceiver
import gov.cdc.prime.router.UniversalPipelineReceiver
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.BlobAccess.Companion.getBlobContainer
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.azure.service.SubmissionResponseBuilder
import gov.cdc.prime.router.cli.PIIRemovalCommands
import gov.cdc.prime.router.cli.ProcessFhirCommands
import gov.cdc.prime.router.common.AzureHttpUtils.getSenderIP
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.history.azure.SubmissionsFacade
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.kotlin.Logging
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.io.path.Path

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
    private val submissionResponseBuilder: SubmissionResponseBuilder = SubmissionResponseBuilder(),
) : RequestFunction(workflowEngine),
    Logging {

    enum class IngestionMethod {
        SFTP,
        REST,
    }

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
     * GET list of senders
     */
    @FunctionName("getSendersForTesting")
    fun getSenders(
        @HttpTrigger(
            name = "getSendersForTesting",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "reports/testing/senders"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims != null && claims.authorized(setOf(Scope.primeAdminScope))) {
            var sendersResponse = listOf(SenderResponse("None", null, null))
            try {
                val senders = workflowEngine.settings.senders.filterIsInstance<UniversalPipelineSender>()
                sendersResponse = sendersResponse.plus(
                    senders.map {
                    SenderResponse("${it.organizationName}.${it.name}", it.format.name, it.schemaName)
                }
                )
                val jsonb = JacksonMapperUtilities.allowUnknownsMapper.writeValueAsString(sendersResponse)
                return HttpUtilities.okResponse(request, jsonb ?: "[]")
            } catch (e: Exception) {
                logger.error(e)
                val jsonb = JacksonMapperUtilities.allowUnknownsMapper.writeValueAsString(sendersResponse)
                return HttpUtilities.okResponse(request, jsonb ?: "[]")
            }
        }
        return HttpUtilities.unauthorizedResponse(request)
    }

    class SenderResponse(var id: String? = null, var format: String? = null, var schemaName: String? = null)

    /**
     * GET messages from test bank
     *
     * @see ../../../docs/api/reports.yml
     */
    @FunctionName("getMessagesFromTestBank")
    fun getMessagesFromTestBank(
        @HttpTrigger(
            name = "getMessagesFromTestBank",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "reports/testing"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims != null && claims.authorized(setOf(Scope.primeAdminScope))) {
            return processGetMessageFromTestBankRequest(request)
        }
        return HttpUtilities.unauthorizedResponse(request)
    }

    /**
     * Run a message through the fhirdata cli
     *
     * @see ../../../docs/api/reports.yml
     */
    @FunctionName("processFhirDataRequest")
    fun processFhirDataRequest(
        @HttpTrigger(
            name = "processFhirDataRequest",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "reports/testing/test"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        val caseInsensitiveHeaders = request.headers.mapKeys { it.key.lowercase() }
        val accessToken = caseInsensitiveHeaders[HttpHeaders.AUTHORIZATION.lowercase()]
         if (claims != null && claims.authorized(setOf(Scope.primeAdminScope))) {
            val receiverName = request.queryParameters["receiverName"]
            val organizationName = request.queryParameters["organizationName"]
            val senderId = request.queryParameters["senderId"]
             if (receiverName.isNullOrBlank()) {
                return HttpUtilities.badRequestResponse(
                    request,
                    "The receiver name is required"
                )
            }
            if (organizationName.isNullOrBlank()) {
                return HttpUtilities.badRequestResponse(
                    request,
                    "The organization name is required"
                )
            }
            if (request.body.isNullOrBlank()) {
                return HttpUtilities.badRequestResponse(
                    request,
                    "Input message is blank."
                )
            }

            val requestString = request.body.toString()
            val inputMessageFormat = if (requestString.contains("\"resourceType\"") &&
                requestString.contains("\"Bundle\"")
            ) {
                "fhir"
            } else if (requestString.contains("PID|1|")) {
                "hl7"
            } else {
                return HttpUtilities.badRequestResponse(
                    request,
                    "Input not recognized as FHIR or HL7."
                )
            }

            var senderSchema: String? = null
            if (!senderId.isNullOrBlank() && senderId != "None") {
                val sender = workflowEngine.settings.findSender(senderId)
                    ?: run {
                        return HttpUtilities.badRequestResponse(
                            request,
                            "No sender found for $senderId."
                        )
                    }
                if (inputMessageFormat != sender.format.ext) {
                    return HttpUtilities.badRequestResponse(
                        request,
                        "Expected ${sender.format.ext.uppercase()} input for selected sender."
                    )
                }
                senderSchema = sender.schemaName
            }
             val file = File("testing.$inputMessageFormat")
             file.createNewFile()
             file.bufferedWriter().use { out ->
                 out.write(request.body)
             }

            try {
                val result = ProcessFhirCommands().processFhirDataRequest(
                    file,
                    Environment.get(),
                    receiverName,
                    organizationName,
                    senderSchema,
                    false,
                    accessToken!!
                )
                file.delete()

                return HttpUtilities.okResponse(
                    request,
                    result.toString()
                )
            } catch (exception: CliktError) {
                file.delete()
                return HttpUtilities.badRequestResponse(request, "${exception.message}")
            }
        }
        return HttpUtilities.unauthorizedResponse(request)
    }

    class MessageOrBundleStringified(
        var message: String? = null,
        var bundle: String? = null,
        override var senderTransformErrors: MutableList<String> = mutableListOf(),
        override var senderTransformWarnings: MutableList<String> = mutableListOf(),
        override var enrichmentSchemaErrors: MutableList<String> = mutableListOf(),
        override var enrichmentSchemaWarnings: MutableList<String> = mutableListOf(),
        override var receiverTransformErrors: MutableList<String> = mutableListOf(),
        override var receiverTransformWarnings: MutableList<String> = mutableListOf(),
        override var filterErrors: MutableList<ProcessFhirCommands.FilterError> = mutableListOf(),
    ) : ProcessFhirCommands.MessageOrBundleParent()

    /**
     * Moved the logic to a separate function for testing purposes
     */
    fun processGetMessageFromTestBankRequest(
        request: HttpRequestMessage<String?>,
        blobAccess: BlobAccess.Companion = BlobAccess,
        defaultBlobMetadata: BlobAccess.BlobContainerMetadata = BlobAccess.defaultBlobMetadata,
    ): HttpResponseMessage = try {
            val updatedBlobMetadata = defaultBlobMetadata.copy(containerName = "test-bank")
            val results = blobAccess.listBlobs("", updatedBlobMetadata)
            val reports = mutableListOf<TestReportInfo>()
            val sourceContainer = getBlobContainer(updatedBlobMetadata)
            results.forEach { currentResult ->
                if (currentResult.currentBlobItem.name.endsWith(".fhir") ||
                    currentResult.currentBlobItem.name.endsWith(".hl7")
                ) {
                    val sourceBlobClient = sourceContainer.getBlobClient(currentResult.currentBlobItem.name)
                    val data = sourceBlobClient.downloadContent()

                    val currentTestReportInfo = TestReportInfo(
                        currentResult.currentBlobItem.properties.creationTime.toString(),
                        currentResult.currentBlobItem.name,
                        data.toString(),
                        currentResult.currentBlobItem.name.substringBefore("/")
                    )
                    reports.add(currentTestReportInfo)
                }
            }

            val mapper: ObjectMapper = JsonMapper.builder()
                .addModule(JavaTimeModule())
                .build()
            HttpUtilities.okResponse(request, mapper.writeValueAsString(reports) ?: "[]")
        } catch (e: Exception) {
            logger.error("Unable to fetch messages from test bank", e)
            HttpUtilities.internalErrorResponse(request)
        }

    class TestReportInfo(var dateCreated: String, var fileName: String, var reportBody: String, var senderId: String)

    /**
     * GET report to download
     *
     * @see ../../../docs/api/reports.yml
     */
    @FunctionName("downloadReport")
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
        piiRemovalCommands: PIIRemovalCommands = PIIRemovalCommands(),
    ): HttpResponseMessage {
        var requestedReport = ReportFile()
        try {
            requestedReport = databaseAccess.fetchReportFile(reportId)
        } catch (e: Exception) {
            HttpUtilities.badRequestResponse(request, "The requested report does not exist.")
        }

        return if (requestedReport.bodyUrl != null && requestedReport.bodyUrl.toString().lowercase().endsWith("fhir")) {
            val contents = BlobAccess.downloadBlobAsByteArray(requestedReport.bodyUrl)

            val content = if (removePII == null || removePII) {
                piiRemovalCommands.removePii(FhirTranscoder.decode(contents.toString(Charsets.UTF_8)))
            } else {
                if (envName == "prod") {
                    return HttpUtilities.badRequestResponse(request, "Must remove PII for messages from prod.")
                }
                String(contents, StandardCharsets.UTF_8)
            }

            HttpUtilities.okJSONResponse(request, Json.parseToJsonElement(content))
        } else if (requestedReport.bodyUrl == null) {
            HttpUtilities.badRequestResponse(request, "The requested report does not exist.")
        } else {
            HttpUtilities.badRequestResponse(request, "The requested report is not fhir.")
        }
    }

    class SftpSubmissionException(override val message: String) : RuntimeException(message)

    @FunctionName("submitSFTP")
    fun submitViaSftp(
        @BlobTrigger(
            name = "report",
            dataType = "string",
            path = "sftp-submissions/{name}.{extension}",
            connection = "SftpStorage"
        ) content: String,
        @BindingName("name") name: String,
        @BindingName("extension") extension: String,
    ) {
        val format = try {
            MimeFormat.valueOfFromExt(extension)
        } catch (ex: IllegalArgumentException) {
            throw SftpSubmissionException("$extension is not valid.")
        }

        val filename = "$name.$extension"

        val senderId = getSenderIdFromFilePath(filename)
        val sender = workflowEngine.settings.findSender(senderId)
            ?: throw SftpSubmissionException("No sender found for $senderId, parsed from $filename")

        if (sender.customerStatus == CustomerStatus.INACTIVE) {
            logger.info("Sender is disabled, not processing the report")
            // TODO https://github.com/CDCgov/prime-reportstream/issues/16260
            return
        }

        val payloadName = extractPayloadNameFromFilePath(filename)
        actionHistory.trackActionSenderInfo(sender.fullName, payloadName)

        try {
            val receiver = SubmissionReceiver.getSubmissionReceiver(sender, workflowEngine, actionHistory)
            val rawBody = content.toByteArray()
            val report = receiver.validateAndMoveToProcessing(
                sender,
                content,
                emptyMap(),
                Options.None,
                emptyList(),
                isAsync = true,
                allowDuplicates = true,
                rawBody = rawBody,
                payloadName = payloadName
            )

            reportEventService.sendReportEvent(
                eventName = ReportStreamEventName.REPORT_RECEIVED,
                childReport = report,
                pipelineStepName = TaskAction.receive
            ) {
                params(
                    listOfNotNull(
                        ReportStreamEventProperties.SENDER_NAME to sender.fullName,
                        ReportStreamEventProperties.INGESTION_TYPE to IngestionMethod.SFTP,
                        ReportStreamEventProperties.ITEM_FORMAT to format
                    ).toMap()
                )
            }
        } catch (e: ActionError) {
            actionHistory.trackLogs(e.details)
            throw e
        } catch (e: IllegalArgumentException) {
            actionHistory.trackLogs(
                ActionLog(InvalidReportMessage(e.message ?: "Invalid request."), type = ActionLogLevel.error)
            )
            throw e
        } catch (e: IllegalStateException) {
            actionHistory.trackLogs(
                ActionLog(InvalidReportMessage(e.message ?: "Invalid request."), type = ActionLogLevel.error)
            )
            throw e
        }

        actionHistory.trackActionResult(HttpStatus.OK)
        workflowEngine.recordAction(actionHistory)
        // queue messages here after all task / action records are in
        actionHistory.queueMessages(workflowEngine)
    }

    private fun extractPayloadNameFromFilePath(filename: String): String = Path(filename).fileName.toString()

    private fun getSenderIdFromFilePath(filename: String): String {
        val path = Path(filename)
        return path.parent.toString().split("/").joinToString(".")
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
        // Allow only active sender (i.e sender with customerStatus="active") to pass through.
        if (sender.customerStatus == CustomerStatus.INACTIVE) {
            return HttpUtilities.gone(
                request,
                "ReportStream sunsetted on December 31, 2025. For more detail, please refer to " +
                        "https://reportstream.cdc.gov."
            )
        }

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
                                ReportStreamEventProperties.INGESTION_TYPE to IngestionMethod.REST,
                                ReportStreamEventProperties.REQUEST_PARAMETERS
                                    to actionHistory.filterParameters(request),
                                ReportStreamEventProperties.SENDER_NAME to sender.fullName,
                                ReportStreamEventProperties.FILE_LENGTH to request.headers["content-length"].toString(),
                                ReportStreamEventProperties.ITEM_COUNT to report.itemCount,
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

        val response = submissionResponseBuilder.buildResponse(
            sender,
            httpStatus,
            request,
            submission
        )

        // queue events here after all task / action records are in
        actionHistory.queueMessages(workflowEngine)

        // queue fhir messages after all tasks / action records are in
        actionHistory.queueFhirMessages(workflowEngine)

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