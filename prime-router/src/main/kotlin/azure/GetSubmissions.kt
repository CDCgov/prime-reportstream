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

    @FunctionName("getSubmissions")
    fun run(
        @HttpTrigger(
            name = "getSubmissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "submissions"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val qLimit = request.queryParameters["limit"]

        var limit = 10
        if (!qLimit.isNullOrEmpty()) {
            if (isNumber(qLimit))
                limit = qLimit.toInt()
            else
                return HttpUtilities.bad(request, "Limit must be an integer.")
        }

        return when (request.httpMethod) {
            HttpMethod.GET -> getList(request, limit)
            else -> error("Unsupported method")
        }
    }

    // Move to Utility Class at some point
    private fun isNumber(s: String): Boolean {
        return if (s.isNullOrEmpty()) false else s.all { Character.isDigit(it) }
    }

    /**
     * Request data after Okta Authentication
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