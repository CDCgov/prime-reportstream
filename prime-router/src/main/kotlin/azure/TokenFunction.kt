package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.FindReportStreamSecretInVault
import gov.cdc.prime.router.tokens.Server2ServerAuthentication
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.logging.log4j.kotlin.Logging
import tokens.Server2ServerAuthenticationException
import tokens.Server2ServerError

/**
 * names of URL parameters required by TokenFunction
 */
const val client_assertion = "client_assertion"
const val scope = "scope"

const val OAUTH_ERROR_BASE_LOCATION =
    "https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/examples/generate-jwt-python/jwt-errors.md"

/**
 * HTTP response body that is returned when an error is
 *
 * @param error the OAuth error code see: https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
 * @param errorDescription the description of the error that occurred
 * @param errorUriLocation the URI location for the explanation of the error
 */
data class OAuthError(
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String,
    @JsonIgnore val errorUriLocation: String
) {
    @JsonProperty("error_uri")
    fun getErrorUri(): String {
        return "$OAUTH_ERROR_BASE_LOCATION#$errorUriLocation"
    }
}

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
    fun token(
        @HttpTrigger(
            name = "token",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        // Exampling incoming URL
        // http://localhost:7071/api/token
        //
        // Body/payload:
        //
        // scope=strac.default.reports
        // &grant_type=client_credentials
        // &client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
        // &client_assertion=verylong.signed.jwtstring
        //
        // Note that best practice is to send the parameters in the body/payload, not in the URL,
        // and to set the header "Content-Type: application/x-www-form-urlencoded"
        // (This is how form data is typically sent)

        // Grab and parse out any parameters sent in the body/payload of the request.
        val bodyParamList = URLEncodedUtils.parse(request.body, Charsets.UTF_8)
        // URLEncodedUtils.parse is nifty, but oddly returns a list, not a map.  Turn it into a Map:
        val bodyParams = bodyParamList.associate {
            Pair(it.name, it.value)
        }
        val workflowEngine = WorkflowEngine.Builder().metadata(metadata).build()
        val actionHistory = ActionHistory(TaskAction.token_auth)
        actionHistory.trackActionParams(request)
        val response = try {
            // Note that we allow the required parameters to come from the request body, or from URL parameters.
            // TODO Work with our senders to deprecate parameters sent in the URL.
            val scope = bodyParams[scope] ?: request.queryParameters[scope]
                ?: throw Server2ServerAuthenticationException(Server2ServerError.MISSING_SCOPE, "")
            val clientAssertion = bodyParams[client_assertion] ?: request.queryParameters[client_assertion]
                ?: throw Server2ServerAuthenticationException(Server2ServerError.MISSING_CLIENT_ASSERTION, scope)

            val server2ServerAuthentication = Server2ServerAuthentication(workflowEngine)
            val jti = DatabaseJtiCache(workflowEngine.db)

            server2ServerAuthentication.checkSenderToken(clientAssertion, scope, jti, actionHistory)
            val token = server2ServerAuthentication.createAccessToken(
                scope, FindReportStreamSecretInVault(), actionHistory
            )

            // Per https://hl7.org/fhir/uv/bulkdata/authorization/index.html#issuing-access-tokens
            return HttpUtilities.okJSONResponse(request, token)
        } catch (ex: Server2ServerAuthenticationException) {
            actionHistory.trackActionResult("Token request denied.")
            actionHistory.setActionType(TaskAction.token_error)

            // The SMART on FHIR spec specifies this error when auth fails
            // http://hl7.org/fhir/uv/bulkdata/authorization/index.html#protocol-details:~:text=the%20server%20SHALL%20respond%20with%20an%20invalid_client%20error
            HttpUtilities.unauthorizedResponse(
                request,
                OAuthError(
                    ex.server2ServerError.oAuthErrorType.name.lowercase(),
                    ex.server2ServerError.name.lowercase(),
                    ex.server2ServerError.errorUri
                )
            )
        }
        workflowEngine.recordAction(actionHistory)
        return response
    }
}