package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.Logging
import org.apache.logging.log4j.kotlin.logger

/*
 * Submissions API
 */

class GetSubmissions(
    submissionsFacade: SubmissionsFacade = SubmissionsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) {
    // TODO: for supporting multiple functions for Submissions, probably want to create BaseSubmissionsFunction
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
        val limit = request.getQueryParameters().getOrDefault("limit", "10")

        return when (request.httpMethod) {
            HttpMethod.GET -> getList(request, limit)
            else -> error("Unsupported method")
        }
    }

    fun getList(
        request: HttpRequestMessage<String?>,
        limit: String
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

/**
 * Common Submission API
 */

open class BaseSubmissionsFunction(
    private val facade: SubmissionsFacade,
    private val oktaAuthentication: OktaAuthentication
) : Logging {
    private val missingAuthorizationHeader = HttpUtilities.errorJson("Missing Authorization Header")
    private val invalidClaim = HttpUtilities.errorJson("Invalid Authorization Header")

    fun <T : SubmissionAPI> getList(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        limit: String,
        clazz: Class<T>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            val submissions = facade.findSubmissionsAsJson(organizationName, limit)
            HttpUtilities.okResponse(request, submissions)
        }
    }

    private fun errorJson(message: String): String = HttpUtilities.errorJson(message)
}