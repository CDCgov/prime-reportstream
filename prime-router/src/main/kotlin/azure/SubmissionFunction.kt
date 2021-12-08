package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
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

    /**
     * An Azure Function that is triggered at the `/api/submissions/` endpoint
     *
     * @param qSortOrder a user supplied sort order overwriting the default value.
     * @param qResultsAfterDate a user supplied `OffsetDateTime` overwriting the default value.
     * @param qLimit a user supplied page size limit overwriting the default value.
     * @return a list of submission history results.
     */
    @FunctionName("getSubmissions")
    fun run(
        @HttpTrigger(
            name = "getSubmissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "submissions"
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

                // URL Query Parameters
                val qSortOrder = request.queryParameters.getOrDefault("sort", "DESC")

                val qResultsAfterDate = request.queryParameters.get("cursor")
                val resultsAfterDate = if (qResultsAfterDate != null) {
                    try {
                        OffsetDateTime.parse(qResultsAfterDate)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("cursor must be a valid datetime")
                    }
                } else null

                val pageSize = request.queryParameters.getOrDefault("pagesize", "10").toIntOrNull()
                require(pageSize != null) { "pageSize must be a positive integer" }

                val submissions = facade.findSubmissionsAsJson(org, qSortOrder, resultsAfterDate, pageSize)
                HttpUtilities.okResponse(request, submissions)
            } catch (e: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
            }
        }
    }
}