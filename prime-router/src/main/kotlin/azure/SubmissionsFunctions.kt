package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.Logging

/*
 * Submissions API
 */

class GetSubmissions(
    submissionsFacade: SubmissionsFacade = SubmissionsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) :
    BaseFunction(submissionsFacade, oktaAuthentication) {
    @FunctionName("getSubmissions")
    fun run(
        @HttpTrigger(
            name = "getSubmissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "submissions"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        return when (request.httpMethod) {
            HttpMethod.GET -> getList(request, SubmissionAPI::class.java)
            else -> error("Unsupported method")
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
        clazz: Class<T>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            val submissions = facade.findSubmissionsAsJson(clazz)
            val lastModified = facade.getLastModified()
            HttpUtilities.okResponse(request, submissions, lastModified)
        }
    }

    private fun errorJson(message: String): String = HttpUtilities.errorJson(message)
}