package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.logger

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */

class GetSubmissions(
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

            val organizationName = it.jwtClaims["organization"] as String

            // URL Query Parameters
            val qSortOrder = request.queryParameters.getOrDefault("sort", "DESC")
            var qResultsAfterDate = request.queryParameters.getOrDefault("after", "")
            val qPageSize = request.queryParameters.getOrDefault("pagesize", "10")

            if (isPositiveInteger(qPageSize)) {
                val pageSize = qPageSize.toInt()
                getList(organizationName, request, qSortOrder, qResultsAfterDate, pageSize)
            } else {
                HttpUtilities.bad(request, "Limit must be a positive integer.")
            }
        }
    }

    /**
     * Return true if [s] is a positive integer.
     * TODO: move to a utility class
     */
    private fun isPositiveInteger(s: String): Boolean {
        return !s.isNullOrEmpty() && s.all { Character.isDigit(it) } && s.toInt() > 0
    }

    /**
     * @param organizationName name of Sending Org set in the JWT Claim.
     * @param request the body content from an authenticated HTTP Request.
     * @param sortOrder sort the results in ASCending or DESCending order.
     * @param resultsAfterDate the `createdAt` value is used to skip DB rows.
     * @param pageSize is an Integer used for setting the number of results per page.
     * @return serialized JSON if okay HTTP response.
     */
    private fun getList(
        organizationName: String,
        request: HttpRequestMessage<String?>,
        sortOrder: String,
        resultsAfterDate: String,
        pageSize: Int
    ): HttpResponseMessage {
        try {
            val submissions = facade.findSubmissionsAsJson(organizationName, sortOrder, resultsAfterDate, pageSize)
            return HttpUtilities.okResponse(request, submissions)
        } catch (e: Exception) {
            logger().error("Unauthorized.", e)
            return HttpUtilities.internalErrorResponse(request)
        }
    }
}