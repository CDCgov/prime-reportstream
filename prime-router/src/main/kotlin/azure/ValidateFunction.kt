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
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.ValidationReceiver
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.Destination
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedReport
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.ReportStreamFilterResultForResponse
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging
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
    @FunctionName("validate")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "validate",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val senderName = extractClient(request)
        if (senderName.isBlank()) {
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")
        }
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // Sender should eventually be obtained directly from who is authenticated
            val sender = workflowEngine.settings.findSender(senderName)
                ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")

            if (!claims.authorizedForSendOrReceive(sender, request)) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
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
     * Handles an incoming validation request after it has been authenticated via the /validate endpoint.
     * @param request The incoming request
     * @param sender The sender record, pulled from the database based on sender name on the request
     * @return Returns an HttpResponseMessage indicating the result of the operation and any resulting information
     */
    internal fun processRequest(
        request: HttpRequestMessage<String?>,
        sender: Sender
    ): HttpResponseMessage {
        // allow duplicates 'override' param
        val allowDuplicatesParam = request.queryParameters.getOrDefault(ALLOW_DUPLICATES_PARAMETER, null)
        var routedTo = emptyList<Translator.RoutedReport>()
        val httpStatus: HttpStatus =
            try {
                val validatedRequest = validateRequest(request)

                // if the override parameter is populated, use that, otherwise use the sender value
                val allowDuplicates = if
                (!allowDuplicatesParam.isNullOrEmpty()) allowDuplicatesParam == "true"
                else {
                    sender.allowDuplicates
                }

                val receiver = ValidationReceiver(workflowEngine, actionHistory)
                routedTo = receiver.validateAndRoute(
                    sender,
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