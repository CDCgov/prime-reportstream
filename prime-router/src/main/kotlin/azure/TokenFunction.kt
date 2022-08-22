package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.FindReportStreamSecretInVault
import gov.cdc.prime.router.tokens.FindSenderKeyInSettings
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.Server2ServerAuthentication
import org.apache.logging.log4j.kotlin.Logging

/**
 * Token functions.
 * @param metadata metadata instance
 */
class TokenFunction(val metadata: Metadata = Metadata.getInstance()) : Logging {
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
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        // Exampling incoming URL
        // http://localhost:7071/api/token?
        // scope=strac.default.reports
        // &grant_type=client_credentials
        // &client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
        // &client_assertion=verylong.signed.jwtstring
        val clientAssertion = request.queryParameters["client_assertion"]
            ?: return HttpUtilities.bad(request, "Missing client_assertion parameter", HttpStatus.UNAUTHORIZED)
        val scope = request.queryParameters["scope"]
            ?: return HttpUtilities.bad(request, "Missing scope parameter", HttpStatus.UNAUTHORIZED)
        if (!Scope.isWellFormedScope(scope))
            return HttpUtilities.bad(request, "Incorrect scope format: $scope", HttpStatus.UNAUTHORIZED)
        val workflowEngine = WorkflowEngine.Builder().metadata(metadata).build()
        val actionHistory = ActionHistory(TaskAction.token_auth)
        actionHistory.trackActionParams(request)
        val senderKeyFinder = FindSenderKeyInSettings(scope, metadata)
        val server2ServerAuthentication = Server2ServerAuthentication()
        val jti = DatabaseJtiCache(workflowEngine.db)
        val response = if (
            server2ServerAuthentication.checkSenderToken(clientAssertion, senderKeyFinder, jti, actionHistory)
        ) {
            val token = server2ServerAuthentication.createAccessToken(
                scope, FindReportStreamSecretInVault(), actionHistory
            )

            // Per https://hl7.org/fhir/uv/bulkdata/authorization/index.html#issuing-access-tokens
            HttpUtilities.httpResponse(
                request, jacksonObjectMapper().writeValueAsString(token), HttpStatus.OK
            )
        } else {
            actionHistory.trackActionResult("Token request denied.")
            actionHistory.setActionType(TaskAction.token_error)
            if (senderKeyFinder.errorMsg != null) {
                logger.error("${senderKeyFinder.errorMsg}")
                actionHistory.trackActionResult("${senderKeyFinder.errorMsg}")
            }

            HttpUtilities.unauthorizedResponse(request)
        }
        workflowEngine.recordAction(actionHistory)
        return response
    }
}