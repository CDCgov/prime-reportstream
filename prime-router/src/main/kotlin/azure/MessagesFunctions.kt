package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.messageTracker.Message
import gov.cdc.prime.router.messageTracker.MessageReceiver
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging

const val MESSAGE_ID_PARAMETER = "messageId"

/**
 * Azure Functions with HTTP Trigger.
 * Search and retrieve messages
 */
class MessagesFunctions(
    private val dbAccess: DatabaseAccess = DatabaseAccess()
) : Logging {
    /**
     * entry point for the /messages/search endpoint,
     * which searches for a given messageId in the database
     */
    @FunctionName("messageSearch")
    @StorageAccount("AzureWebJobsStorage")
    fun messageSearch(
        @HttpTrigger(
            name = "messageSearch",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "messages/search"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            processSearchRequest(request)
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
     * Handles an incoming message search request.
     * @param request The incoming request
     * @return Returns an HttpResponseMessage indicating the result of the operation and any resulting information
     */
    internal fun processSearchRequest(
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val messageId = request.queryParameters
            .getOrDefault(
                MESSAGE_ID_PARAMETER,
                null
            )

        // return a list of messages or an error
        var response: Any = emptyList<Message>()
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                if (messageId.isNullOrEmpty() || messageId.isBlank()) {
                    errorMessage = InvalidParamMessage("Missing the messageId request param").message
                    HttpStatus.BAD_REQUEST
                } else {
                    val results = dbAccess.fetchCovidResultMetadatasByMessageId(messageId)
                    response = results
                        .map {
                            Message(
                                it.covidResultsMetadataId,
                                it.messageId,
                                it.senderId,
                                it.createdAt,
                                it.reportId.toString(),
                                null,
                                null
                            )
                        }

                    HttpStatus.OK
                }
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { e.message }
                HttpStatus.BAD_REQUEST
            }

        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    /**
     * entry point for the /message/{id} endpoint,
     * which searches for a given id of a metadata result in the database
     */
    @FunctionName("messageDetails")
    @StorageAccount("AzureWebJobsStorage")
    fun messageDetails(
        @HttpTrigger(
            name = "messageDetails",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "message/{id}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: Long
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            processMessageDetailRequest(request, id)
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
     * Handles an incoming id to retrieve message details request.
     * @param id to search by
     * @return Returns an HttpResponseMessage indicating the result of the operation and any resulting information
     */
    internal fun processMessageDetailRequest(
        request: HttpRequestMessage<String?>,
        id: Long
    ): HttpResponseMessage {
        // return a message or an error
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                val result = dbAccess.fetchSingleMetadataById(id)
                if (result == null) {
                    errorMessage = "No message found."
                    HttpStatus.BAD_REQUEST
                } else {
                    // incoming report from the sender
                    val reportResult = dbAccess.fetchReportFile(result.reportId)
                    val actionLogWarnings = dbAccess.fetchActionLogsByReportIdAndTrackingIdAndType(
                        result.reportId,
                        result.messageId,
                        ActionLogType.warning
                    )
                    val actionLogErrors = dbAccess.fetchActionLogsByReportIdAndTrackingIdAndType(
                        result.reportId,
                        result.messageId,
                        ActionLogType.error
                    )
                    val receiverData = getReceiverData(result.reportId, result.messageId)
                    response = Message(
                        result.covidResultsMetadataId,
                        result.messageId,
                        result.senderId,
                        result.createdAt,
                        result.reportId.toString(),
                        reportResult.externalName,
                        reportResult.bodyUrl,
                        actionLogWarnings,
                        actionLogErrors,
                        receiverData
                    )
                    HttpStatus.OK
                }
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { e.message }
                HttpStatus.BAD_REQUEST
            }

        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    private fun responseBuilder(
        httpStatus: HttpStatus,
        errorMessage: String?,
        response: Any,
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val responseMessage = if (httpStatus == HttpStatus.BAD_REQUEST) {
            mapOf(
                "error" to true,
                "status" to httpStatus.value(),
                "message" to errorMessage
            )
        } else {
            response
        }

        return request.createResponseBuilder(httpStatus)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                JacksonMapperUtilities.allowUnknownsMapper
                    .writeValueAsString(responseMessage)
            )
            .build()
    }

    private fun getReceiverData(reportId: ReportId, trackingId: String): List<MessageReceiver> {
        val reportDescendants = dbAccess
            .fetchReportDescendantsFromReportId(reportId)
            .filter { it.receivingOrg != null && it.reportId != reportId }
        val childReportIds = reportDescendants.map { it.reportId }
        val reportFiles = dbAccess.fetchReportFileByIds(childReportIds)

        return reportFiles.map {
            // Should we filter by trackingId???
            val qualityFilters = dbAccess.fetchActionLogsByReportIdAndFilterType(
                it.reportId,
                trackingId,
                "QUALITY_FILTER"
            )

            MessageReceiver(
                it.reportId.toString(),
                it.receivingOrg,
                it.receivingOrgSvc,
                it.transportResult,
                it.externalName,
                it.bodyUrl,
                it.createdAt.toLocalDateTime(),
                qualityFilters
            )
        }
    }
}