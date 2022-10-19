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
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.Message
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging

const val MESSAGE_SEARCH_ID_PARAMETER = "messageId"

/**
 * Azure Functions with HTTP Trigger.
 * Search and retrieve messages
 */
class MessagesFunction(
    private val dbAccess: DatabaseAccess = DatabaseAccess(),
) : Logging {

    /**
     * entry point for the /messages/search endpoint,
     * which searches for a given messageId in the database
     */
    @FunctionName("messageSearch")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
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
                MESSAGE_SEARCH_ID_PARAMETER,
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
                                it.messageId,
                                it.senderId,
                                it.createdAt,
                                it.reportId.toString()
                            )
                        }

                    HttpStatus.OK
                }
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { e.message }
                HttpStatus.BAD_REQUEST
            }

        if (httpStatus == HttpStatus.BAD_REQUEST) {
            response = mapOf(
                "error" to true,
                "status" to httpStatus.value(),
                "message" to errorMessage
            )
        }

        return request.createResponseBuilder(httpStatus)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                JacksonMapperUtilities.allowUnknownsMapper
                    .writeValueAsString(response)
            )
            .build()
    }
}