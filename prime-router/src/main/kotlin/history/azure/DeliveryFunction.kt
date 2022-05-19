package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure

/**
 * Deliveries API
 * Returns a list of Actions from `public.action`.
 */
class DeliveryFunction(
    private val deliveryFacade: DeliveryFacade = DeliveryFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine(),
) : ReportFileFunction(
    workflowEngine,
) {
    /**
     * This endpoint is meant for use by either an Admin or a User.
     * It does not assume the user belongs to a single Organization.  Rather, it uses
     * the organization in the URL path, after first confirming authorization to access that organization.
     */
    @FunctionName("getDeliveries")
    fun getDeliveries(
        @HttpTrigger(
            name = "getDeliveries",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/{organization}/deliveries"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val claims = AuthenticationStrategy.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // Confirm the org name in the path is a sender in the system.
            val sender = workflowEngine.settings.findSender(organization) // err if no default sender in settings in org
                ?: return HttpUtilities.notFoundResponse(request, "$organization: unknown ReportStream sender")

            // Do authorization based on: org name in the path == org name in claim.  Or be a prime admin.
            if ((claims.organizationNameClaim != sender.organizationName) && !claims.isPrimeAdmin) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claim org is ${claims.organizationNameClaim} but client id is ${sender.organizationName}"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.organizationNameClaim}" +
                    " to $sender/deliveries endpoint via client id ${sender.organizationName}. "
            )

            val (qSortOrder, qSortColumn, resultsAfterDate, resultsBeforeDate, pageSize) =
                Parameters(request.queryParameters)

            val sortOrder = try {
                ReportFileAccess.SortOrder.valueOf(qSortOrder)
            } catch (e: IllegalArgumentException) {
                ReportFileAccess.SortOrder.DESC
            }

            val sortColumn = try {
                ReportFileAccess.SortColumn.valueOf(qSortColumn)
            } catch (e: IllegalArgumentException) {
                ReportFileAccess.SortColumn.CREATED_AT
            }

            val deliveries = deliveryFacade.findDeliveriesAsJson(
                sender.organizationName,
                sortOrder,
                sortColumn,
                resultsAfterDate,
                resultsBeforeDate,
                pageSize
            )

            return HttpUtilities.okResponse(request, deliveries)
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, e.message ?: "Invalid Request")
        }
    }
}