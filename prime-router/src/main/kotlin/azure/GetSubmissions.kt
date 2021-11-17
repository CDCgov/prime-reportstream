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
     * @param qLimit a user supplied page size limit overwriting the default value.
     * @param limit a default value for the number of results to display per page.
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
        val qLimit = request.queryParameters.getOrDefault("limit", "10")

        if (isPositiveInteger(qLimit)) {
            val limit = qLimit.toInt()
            return getList(request, limit)
        }

        return HttpUtilities.bad(request, "Limit must be a positive integer.")
    }

    /**
     * Return true if [s] is a positive integer.
     * TODO: move to a utility class
     */
    private fun isPositiveInteger(s: String): Boolean {
        return s.isNullOrEmpty() && s.all { Character.isDigit(it) } && s.toInt() > 0
    }

    /**
     * @param request the body content from an HTTP Request to pass into Okta for authentication.
     * @param limit is an Integer used for setting the number of results per page.
     * @return data after Okta Authentication
     */
    private fun getList(
        request: HttpRequestMessage<String?>,
        limit: Int
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            try {
                val organizationName = it.jwtClaims["organization"] as String
                val submissions = facade.findSubmissionsAsJson(organizationName, limit)
                HttpUtilities.okResponse(request, submissions)
            } catch (e: Exception) {
                logger().error("Unauthorized.", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }
}