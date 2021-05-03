package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.FindReportStreamSecretInVault
import gov.cdc.prime.router.tokens.FindSenderKeyInSettings
import gov.cdc.prime.router.tokens.TokenAuthentication
import org.apache.logging.log4j.kotlin.Logging

class TokenFunction: Logging {


    /**
     * Handle requests for server-to-server auth tokens.
     */
    @FunctionName("token")
    @StorageAccount("AzureWebJobsStorage")
    fun report(
        @HttpTrigger(
            name = "token",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val workflowEngine = WorkflowEngine()
        val tokenAuthentication = TokenAuthentication(DatabaseJtiCache(workflowEngine.db))
        // Exampling incoming URL
        // http://localhost:7071/api/token?
        // scope=reports
        // &grant_type=client_credentials
        // &client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
        // &client_assertion=a.b.c
        val clientAssertion = request.queryParameters["client_assertion"]
            ?: return HttpUtilities.unauthorizedResponse(request)
        val scope = request.queryParameters["scope"]
            ?: return HttpUtilities.unauthorizedResponse(request)
        if (tokenAuthentication.checkSenderToken(clientAssertion, FindSenderKeyInSettings(scope))) {
            val token = tokenAuthentication.createAccessToken(scope, FindReportStreamSecretInVault())
            // return the token as the payload??????
            return HttpUtilities.httpResponse(request, token.accessToken, HttpStatus.OK)
        } else {
            return HttpUtilities.unauthorizedResponse(request)
        }
    }
}