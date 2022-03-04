package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */

class SubmissionFunction(
    submissionsFacade: SubmissionsFacade = SubmissionsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) : Logging {
    private val facade = submissionsFacade
    private val oktaAuthentication = oktaAuthentication

    data class Parameters(
        val sort: String,
        val sortColumn: String,
        val cursor: OffsetDateTime?,
        val endCursor: OffsetDateTime?,
        val pageSize: Int,
    ) {
        constructor(query: Map<String, String>) : this (
            extractSortOrder(query),
            extractSortCol(query),
            extractCursor(query, "cursor"),
            extractCursor(query, "endCursor"),
            extractPageSize(query),
        )

        companion object {
            fun extractSortOrder(query: Map<String, String>): String {
                return query.getOrDefault("sort", "DESC")
            }

            fun extractSortCol(query: Map<String, String>): String {
                return query.getOrDefault("sortCol", "default")
            }

            fun extractCursor(query: Map<String, String>, name: String): OffsetDateTime? {
                val cursor = query.get(name)
                return if (cursor != null) {
                    try {
                        OffsetDateTime.parse(cursor)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("\"$name\" must be a valid datetime")
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
    @FunctionName("getOrgSubmissions")
    fun getOrgSubmissions(
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
                val (qSortOrder, qSortColumn, resultsAfterDate, resultsBeforeDate, pageSize) =
                    Parameters(request.queryParameters)
                val submissions = facade.findSubmissionsAsJson(
                    organization,
                    qSortOrder,
                    qSortColumn,
                    resultsAfterDate,
                    resultsBeforeDate,
                    pageSize
                )
                HttpUtilities.okResponse(request, submissions)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            }
        }
    }

    @FunctionName("getSubmissionHistory")
    fun getSubmissionHistory(
        @HttpTrigger(
            name = "getSubmissionHistory",
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
                if (submission != null) HttpUtilities.okJSONResponse(request, submission)
                else HttpUtilities.notFoundResponse(request, "Submission $submissionId was not found.")
            } catch (e: DataAccessException) {
                logger.error("Unable to fetch history for submission ID $submissionId", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Get the history for a given report ID.
     */
    @FunctionName("getReportHistory")
    fun getReportHistory(
        @HttpTrigger(
            name = "getReportHistory",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/{organization}/report/{reportId}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
        @BindingName("reportId") reportId: String, // Use string to be able to detect a format error
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organization, true) {
            val reportUuid = try {
                UUID.fromString(reportId)
            } catch (e: IllegalArgumentException) {
                // Need to isolate this catch as this type of exception can happen for other reasons
                logger.debug("Invalid format for report ID.", e)
                null
            }

            if (reportUuid != null)
                try {
                    val submission = facade.findReport(organization, reportUuid)
                    if (submission != null) HttpUtilities.okJSONResponse(request, submission)
                    else HttpUtilities.notFoundResponse(request, "Report $reportId was not found.")
                } catch (e: DataAccessException) {
                    logger.error("Unable to fetch history for report ID $reportId", e)
                    HttpUtilities.internalErrorResponse(request)
                }
            else HttpUtilities.badRequestResponse(request, "Invalid format for report ID parameter.")
        }
    }
}