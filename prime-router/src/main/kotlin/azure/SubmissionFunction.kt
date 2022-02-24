package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */

class SubmissionFunction(
    submissionsFacade: SubmissionsFacade = SubmissionsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) {
    private val facade = submissionsFacade
    private val oktaAuthentication = oktaAuthentication

    data class Parameters(
        val sort: String,
        val sortColumn: String,
        val cursor: OffsetDateTime?,
        val pageSize: Int,
    ) {
        constructor(query: Map<String, String>) : this (
            extractSortOrder(query),
            extractSortCol(query),
            extractCursor(query),
            extractPageSize(query),
        )

        companion object {
            fun extractSortOrder(query: Map<String, String>): String {
                val qSortOrder = query.getOrDefault("sort", "DESC")
                return qSortOrder
            }

            fun extractSortCol(query: Map<String, String>): String {
                return query.getOrDefault("sortCol", "default")
            }

            fun extractCursor(query: Map<String, String>): OffsetDateTime? {
                val qResultsAfterDate = query.get("cursor")
                return if (qResultsAfterDate != null) {
                    try {
                        OffsetDateTime.parse(qResultsAfterDate)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("cursor must be a valid datetime")
                    }
                } else null
            }

            fun extractPageSize(query: Map<String, String>): Int {
                val size = query.getOrDefault("pagesize", "10").toIntOrNull()
                require(size != null) { "pageSize must be a positive integer" }
                return size
            }
        }
    }

    /**
     * This endpoint is meant for use by either an Admin or a User.
     * It does not assume the user belongs to a single Organization.  Rather, it uses
     * the organization in the URL path, after first confirming authorization to access that organization.
     */
    @FunctionName("getSubmissions")
    fun submissions(
        @HttpTrigger(
            name = "getOrgSubmissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/{organization}/submissions"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organization, true) {
            try {
                val (qSortOrder, qSortColumn, resultsAfterDate, pageSize) = Parameters(request.queryParameters)
                val submissions = facade.findSubmissionsAsJson(
                    organization,
                    qSortOrder,
                    qSortColumn,
                    resultsAfterDate,
                    pageSize
                )
                HttpUtilities.okResponse(request, submissions)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            }
        }
    }

    @FunctionName("getSubmission")
    fun submission(
        @HttpTrigger(
            name = "getSubmission",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/{organization}/submissions/{submissionId}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
        @BindingName("submissionId") submissionId: Long,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organization, true) {

            try {
                val submission = facade.findSubmission(organization, submissionId)
                    ?: throw HttpNotFoundException("No submission found")
                HttpUtilities.okJSONResponse(request, submission)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            } catch (e: HttpNotFoundException) {
                HttpUtilities.notFoundResponse(request, e.message)
            }
        }
    }
}