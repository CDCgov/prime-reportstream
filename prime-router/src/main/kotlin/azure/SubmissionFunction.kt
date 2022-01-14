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
        val cursor: OffsetDateTime?,
        val pageSize: Int,
    ) {
        constructor(query: Map<String, String>) : this(
            extractSort(query),
            extractCursor(query),
            extractPageSize(query),
        )

        companion object {
            fun extractSort(query: Map<String, String>): String {
                val qSortOrder = query.getOrDefault("sort", "DESC")
                return qSortOrder
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
    @FunctionName("getOrgSubmissions")
    fun organizationSubmissions(
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
                val (qSortOrder, resultsAfterDate, pageSize) = Parameters(request.queryParameters)

                val submissions = facade.findSubmissionsAsJson(organization, qSortOrder, resultsAfterDate, pageSize)
                HttpUtilities.okResponse(request, submissions)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            }
        }
    }

    /**
     * An Azure Function that is triggered at the `/api/submissions/` endpoint
     * This endpoint is meant for use by a User.  It assumes the user belongs to a single Organization.
     * DO NOT USE - DEPRECATED - DELETE THIS
     *
     * @param qSortOrder a user supplied sort order overwriting the default value.
     * @param qResultsAfterDate a user supplied `OffsetDateTime` overwriting the default value.
     * @param qLimit a user supplied page size limit overwriting the default value.
     * @return a list of submission history results.
     */
    @FunctionName("getSubmissions")
    fun submissions(
        @HttpTrigger(
            name = "getSubmissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/submissions"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {

            try {
                @Suppress("UNCHECKED_CAST")
                val orgs = it.jwtClaims["organization"] as? List<String>
                require(orgs != null) {
                    "Invalid organization"
                }

                // a user can now be part of a sender group as well, so find the first "sender" group in their claims
                var org = (orgs).find {
                    org ->
                    org.startsWith(oktaSenderGroupPrefix)
                }
                require(org != null) {
                    "Invalid organization"
                }

                org = org.removePrefix(oktaSenderGroupPrefix)

                val (qSortOrder, resultsAfterDate, pageSize) = Parameters(request.queryParameters)

                val submissions = facade.findSubmissionsAsJson(org, qSortOrder, resultsAfterDate, pageSize)
                HttpUtilities.okResponse(request, submissions)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            }
        }
    }
}