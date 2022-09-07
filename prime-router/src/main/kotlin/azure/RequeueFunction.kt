package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
import org.apache.logging.log4j.kotlin.Logging
import java.util.UUID

/*
 * Requeue API
 */

class RequeueFunction : Logging {
    @FunctionName("requeue") // devnote:  putting slashes in this (/) breaks it.
    @Deprecated(
        "This uses Azure auth x-functions-key. " +
            "Left in until new method 'adm/resend' verified in production"
    )
    fun requeue(
        @HttpTrigger(
            name = "requeue",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION, // Azure function auth `x-functions-key`
            route = "requeue/send"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        logger.info("Entering requeue/send api")
        return handleRequest(request, null)
    }

    @FunctionName("doResendFunction")
    fun doResendFunction(
        @HttpTrigger(
            name = "doResendFunction",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS, // NO Azure function auth aka `x-functions-key`
            route = "adm/resend"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        logger.info("adm/resend api")
        return OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) {
            handleRequest(request, it)
        }
    }

    fun handleRequest(request: HttpRequestMessage<String?>, claim: AuthenticatedClaims?): HttpResponseMessage {
        val workflowEngine = WorkflowEngine()
        val actionHistory = ActionHistory(TaskAction.resend)
        actionHistory.trackActionParams(request)
        val msgs = mutableListOf<String>()
        val response = try {
            doResend(request, workflowEngine, msgs)
        } catch (t: Throwable) {
            msgs.add(t.cause?.let { "${t.cause!!.localizedMessage}\n" } ?: ("" + t.localizedMessage))
            HttpUtilities.bad(request, msgs.joinToString("\n") + "\n")
        }
        actionHistory.trackActionResult(response.status, response.body.toString())
        if (claim !== null && claim.userName.trimToNull() !== null) {
            actionHistory.trackUsername(claim.userName)
        }
        workflowEngine.recordAction(actionHistory)
        return response
    }

    fun doResend(
        request: HttpRequestMessage<String?>,
        workflowEngine: WorkflowEngine,
        msgs: MutableList<String>,
    ): HttpResponseMessage {
        val isTest = ! request.queryParameters["test"].isNullOrEmpty()
        if (isTest) msgs.add("Here is what would happen if this were NOT a test:")
        if (request.queryParameters.size < 2 || request.queryParameters.size > 4)
            return HttpUtilities.bad(request, "Expecting 2 to 4 parameters\n")
        val reportIdStr = request.queryParameters["reportId"]
            ?: return HttpUtilities.bad(request, "Missing option reportId\n")
        val reportId = UUID.fromString(reportIdStr)
        val fullName = request.queryParameters["receiver"]
            ?: return HttpUtilities.bad(request, "Missing option receiver\n")
        val receiver = workflowEngine.settings.findReceiver(fullName)
            ?: return HttpUtilities.bad(request, "No such receiver fullname $fullName\n")
        // sanity checks throw exceptions inside here:
        workflowEngine.resendEvent(reportId, receiver, isTest, msgs)
        return HttpUtilities.httpResponse(request, msgs.joinToString("\n") + "\n", HttpStatus.OK)
    }
}