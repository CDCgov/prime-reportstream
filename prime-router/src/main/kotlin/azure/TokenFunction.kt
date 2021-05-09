package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.FindSenderKeyInSettings
import gov.cdc.prime.router.tokens.GetStaticSecret
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
        // &client_assertion=verylong.signed.jwtstring
        val clientAssertion = request.queryParameters["client_assertion"]
            ?: return HttpUtilities.bad(request, "Missing client_assertion parameter", HttpStatus.UNAUTHORIZED)
        val scope = request.queryParameters["scope"]
            ?: return HttpUtilities.bad(request, "Missing scope parameter", HttpStatus.UNAUTHORIZED)
        if (!TokenAuthentication.isWellFormedScope(scope))
            return HttpUtilities.bad(request, "Incorrect scope format: $scope", HttpStatus.UNAUTHORIZED)
        val actionHistory = ActionHistory(TaskAction.token_auth, context)
        actionHistory.trackActionParams(request)
        val senderKeyFinder = FindSenderKeyInSettings(scope)
        if (tokenAuthentication.checkSenderToken(clientAssertion, senderKeyFinder, actionHistory)) {
            val token = tokenAuthentication.createAccessToken(scope, GetStaticSecret(), actionHistory)
            workflowEngine.recordAction(actionHistory)
            // Per https://hl7.org/fhir/uv/bulkdata/authorization/index.html#issuing-access-tokens
            return HttpUtilities.httpResponse(request, jacksonObjectMapper().writeValueAsString(token), HttpStatus.OK)
        } else {
            actionHistory.trackActionResult("Token request denied.")
            actionHistory.setActionType(TaskAction.token_error)
            if (senderKeyFinder.errorMsg != null) {
                logger.error("${senderKeyFinder.errorMsg}")
                actionHistory.trackActionResult("${senderKeyFinder.errorMsg}")
            }
            workflowEngine.recordAction(actionHistory)
            return HttpUtilities.unauthorizedResponse(request)
        }
    }
}