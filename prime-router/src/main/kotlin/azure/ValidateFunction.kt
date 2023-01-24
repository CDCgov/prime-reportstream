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
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.TopicSender
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.ValidationReceiver
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.Destination
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedReport
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.ReportStreamFilterResultForResponse
import org.apache.logging.log4j.kotlin.Logging
import java.security.InvalidParameterException
import java.time.OffsetDateTime

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ValidateFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.receive)
) : Logging, RequestFunction(workflowEngine) {

    /**
     * entry point for the /validate endpoint, which validates a potential submission without writing
     * to the database or storing/sending a file
     */
    @FunctionName("validateWithClient")
    @StorageAccount("AzureWebJobsStorage")
    fun validateWithClient(
        @HttpTrigger(
            name = "validateWithClient",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return try {
            val senderName = extractClient(request)
            if (senderName.isBlank()) {
                return HttpUtilities.bad(
                    request, "Expected a '$CLIENT_PARAMETER' query parameter"
                )
            }
            val sender = workflowEngine.settings.findSender(senderName)
                ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")
            actionHistory.trackActionParams(request)
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
     * entry point for the /validate endpoint, which validates a potential submission without writing
     * to the database or storing/sending a file
     */
    @FunctionName("validateWithSchema")
    @StorageAccount("AzureWebJobsStorage")
    fun validateWithSchema(
        @HttpTrigger(
            name = "validateWithSchema",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return try {
            val schemaName = request.queryParameters.getOrDefault(SCHEMA_PARAMETER, null)
            val format = request.queryParameters.getOrDefault(FORMAT_PARAMETER, null)
            if (schemaName != null && format != null) {
                actionHistory.trackActionParams(request)
                processRequest(request, schemaName = schemaName, format = format)
            } else {
                return HttpUtilities.bad(
                    request, "Expected '$SCHEMA_PARAMETER' and '$FORMAT_PARAMETER' query parameters"
                )
            }
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
     * Handles an incoming validation request after it has been authenticated via the /validate endpoint.
     * @param request The incoming request
     * @param sender The sender record, pulled from the database based on sender name on the request
     * @return Returns an HttpResponseMessage indicating the result of the operation and any resulting information
     */
    internal fun processRequest(
        request: HttpRequestMessage<String?>,
        sender: Sender? = null,
        schemaName: String? = null,
        format: String? = null
    ): HttpResponseMessage {
        if (sender == null && (schemaName == null || format == null)) {
            throw InvalidParameterException(
                "Invalid request. Must provide at least one of: sender or schemaName with format"
            )
        }
        // allow duplicates 'override' param
        val allowDuplicatesParam = request.queryParameters.getOrDefault(ALLOW_DUPLICATES_PARAMETER, null)
        var routedTo = emptyList<Translator.RoutedReport>()
        val httpStatus: HttpStatus =
            try {
                val validatedRequest = validateRequest(request)

                // if the override parameter is populated, use that, otherwise use the sender value. Default to false.
                var allowDuplicates = false
                if (!allowDuplicatesParam.isNullOrEmpty()) {
                    allowDuplicates = allowDuplicatesParam == "true"
                } else if (sender != null) {
                    allowDuplicates = sender.allowDuplicates
                }

                // setup dummy sender if needed
                var validationSender = sender
                if (validationSender == null) {
                    if (schemaName != null && format != null) {
                        val schema = workflowEngine.metadata.findSchema(schemaName)
                        if (schema != null) {
                            validationSender = TopicSender(
                                "ValidationSender",
                                "Internal",
                                Sender.Format.valueOf(format),
                                CustomerStatus.TESTING,
                                schemaName,
                                schema.topic
                            )
                        } else {
                            // error schema not found
                            throw InvalidParameterException("Schema of name '$schemaName' could not be found")
                        }
                    } else {
                        throw InvalidParameterException(
                            "Parameters 'schemaName' and 'format' not found when sender also not provided"
                        )
                    }
                }
                val receiver = ValidationReceiver(workflowEngine, actionHistory)
                routedTo = receiver.validateAndRoute(
                    validationSender,
                    validatedRequest.content,
                    validatedRequest.defaults,
                    validatedRequest.routeTo,
                    allowDuplicates,
                )
                // return OK status, report validation was successful
                HttpStatus.OK
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
            } catch (e: InvalidParameterException) {
                actionHistory.trackLogs(
                    ActionLog(InvalidReportMessage(e.message ?: "Invalid request."), type = ActionLogLevel.error)
                )
                HttpStatus.BAD_REQUEST
            }

        val submission = DetailedSubmissionHistory(
            1,
            TaskAction.none,
            OffsetDateTime.now(),
            httpStatus.value(),
            mutableListOf<DetailedReport>(),
            actionHistory.actionLogs.map {
                DetailedActionLog(
                    it.scope,
                    it.reportId,
                    it.index,
                    it.trackingId,
                    it.type,
                    it.detail
                )
            }
        )

        // add quality filter results to the submission before returning
        routedTo.forEach { it ->
            submission.destinations.add(
                Destination(
                    it.receiver.organizationName,
                    it.receiver.name,
                    itemCount = it.report.itemCount,
                    sentReports = mutableListOf(),
                    downloadedReports = mutableListOf(),
                    filteredReportItems = it.report.filteringResults.map { ReportStreamFilterResultForResponse(it) },
                    filteredReportRows = it.report.filteringResults.map { it.message },
                    itemCountBeforeQualFilter = it.report.itemCountBeforeQualFilter,
                    sendingAt = null
                )
            )
        }

        // set status for validation response
        submission.overallStatus = if (httpStatus == HttpStatus.BAD_REQUEST)
            DetailedSubmissionHistory.Status.ERROR
        else
            DetailedSubmissionHistory.Status.VALID

        return request.createResponseBuilder(httpStatus)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                JacksonMapperUtilities.allowUnknownsMapper
                    .writeValueAsString(submission)
            )
            .build()
    }
}