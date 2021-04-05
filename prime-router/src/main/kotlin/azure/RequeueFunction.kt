package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.apache.logging.log4j.kotlin.Logging
import java.util.UUID

/*
 * Requeue API
 */

class RequeueFunction : Logging {
    @FunctionName("requeue") // devnote:  putting slashes in this (/) breaks it.
    fun run(
        @HttpTrigger(
            name = "requeue",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "requeue/send"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        logger.info("Entering requeue/send api")
        val workflowEngine = WorkflowEngine()
        val actionHistory = ActionHistory(TaskAction.resend, context)
        actionHistory.trackActionParams(request)
        val response = try {
            doResend(request, workflowEngine, actionHistory)
        } catch (t: Throwable) {
            val msg: String = t.cause?.let { "${t.cause!!.localizedMessage}\n" } + t.localizedMessage
            bad(request, msg + "\n")
        }
        actionHistory.trackActionResult(response)
        workflowEngine.recordAction(actionHistory)
        return response
    }

    fun doResend(
        request: HttpRequestMessage<String?>,
        workflowEngine: WorkflowEngine,
        actionHistory: ActionHistory
    ): HttpResponseMessage {
        if (request.queryParameters.size != 2) return bad(request, "Expecting 2 parameters\n")
        val reportIdStr = request.queryParameters["reportId"]
            ?: return bad(request, "Missing option reportId\n")
        val reportId = UUID.fromString(reportIdStr)
        val fullName = request.queryParameters["receiver"]
            ?: return bad(request, "Missing option receiver\n")
        val receiver = workflowEngine.settings.findReceiver(fullName)
            ?: return bad(request, "No such receiver fullname $fullName\n")
        workflowEngine.resendEvent(reportId, receiver) // sanity checks throw exceptions
        return HttpUtilities.httpResponse(
            request, "Report $reportId queued to resend immediately to ${receiver.fullName}\n",
            HttpStatus.OK
        )
    }

    fun bad(request: HttpRequestMessage<String?>, msg: String): HttpResponseMessage {
        logger.error(msg)
        return HttpUtilities.badRequestResponse(request, msg)
    }
}