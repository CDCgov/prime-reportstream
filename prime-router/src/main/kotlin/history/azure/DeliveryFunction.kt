package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure

/**
 * Deliveries API
 * Returns a list of Actions from `public.action`. combined with `public.report_file`.
 *
 * @property reportFileFacade Facade class containing business logic to handle the data.
 * @property workflowEngine Container for helpers and accessors used when dealing with the workflow.
 */
class DeliveryFunction(
    val deliveryFacade: DeliveryFacade = DeliveryFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine(),
) : ReportFileFunction(
    deliveryFacade,
    workflowEngine,
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Authorization and shared logic uses the organization name without the service
     * We store the service name here to pass to the facade
     */
    var receivingOrgSvc: String? = null

    /**
     * Get the correct name for an organization receiver based on the name.
     *
     * @param organization Name of organization and client in the format {orgName}.{client}
     * @return Name for the organization
     */
    override fun userOrgName(organization: String): String? {
        val receiver = workflowEngine.settings.findReceiver(organization)
        receivingOrgSvc = receiver?.name
        return receiver?.organizationName
    }

    /**
     * Get a list of delivery history
     *
     * @param queryParams Parameters extracted from the HTTP Request
     * @param userOrgName Name of the organization
     * @return json list of deliveries
     */
    override fun historyAsJson(queryParams: MutableMap<String, String>, userOrgName: String): String {
        val params = HistoryApiParameters(queryParams)

        val deliveries = deliveryFacade.findDeliveries(
            userOrgName,
            receivingOrgSvc,
            params.sortDir,
            params.sortColumn,
            params.cursor,
            params.since,
            params.until,
            params.pageSize,
        )

        return mapper.writeValueAsString(deliveries)
    }

    /**
     * Get expanded details for a single report
     *
     * @param queryParams Parameters extracted from the HTTP Request
     * @param action Action from which the data for the delivery is loaded
     * @return
     */
    override fun singleDetailedHistory(queryParams: MutableMap<String, String>, action: Action): DeliveryHistory? {
        return deliveryFacade.findDetailedDeliveryHistory(action.sendingOrg, action.actionId)
    }

    /**
     * This endpoint is meant for use by either an Admin or a User.
     * It does not assume the user belongs to a single Organization.  Rather, it uses
     * the organization in the URL path, after first confirming authorization to access that organization.
     *
     * @param request HTML request body.
     * @param organization Name of organization and service
     * @return json list of deliveries
     */
    @FunctionName("getDeliveries")
    fun getDeliveries(
        @HttpTrigger(
            name = "getDeliveries",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/org/{organization}/deliveries"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
    ): HttpResponseMessage {
        return this.getListByOrg(request, organization)
    }

    /**
     * Get expanded details for a single report
     *
     * @param request HTML request body.
     * @param deliveryId Report or Delivery id
     * @return json formatted delivery
     */
    @FunctionName("getDeliveryDetails")
    fun getDeliveryDetails(
        @HttpTrigger(
            name = "getDeliveryDetails",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{deliveryId}/delivery"
        ) request: HttpRequestMessage<String?>,
        @BindingName("deliveryId") deliveryId: String,
    ): HttpResponseMessage {
        return this.getDetailedView(request, deliveryId)
    }

    /**
     * Get a sortable list of delivery facilities
     *
     * @param request HTML request body.
     * @param reportId UUID for the report to get facilities from
     * @return JSON of the facility list or errors.
     */
    @FunctionName("getDeliveryFacilities")
    fun getDeliveryFacilities(
        @HttpTrigger(
            name = "getDeliveryFacilities",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{deliveryId}/facilities"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val claims = AuthenticationStrategy.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            logger.info("Authenticated request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")

            val convertedReportId = toUuidOrNull(reportId) ?: error("Bad format: $reportId must be a num or a UUID")
            val action = deliveryFacade.fetchActionForReportId(convertedReportId)
                ?: error("No such reportId: $reportId")

            // Do Authorization.  Confirm these claims allow access to this Action
            if (!deliveryFacade.checkSenderAccessAuthorization(action, claims)) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            logger.info(
                "Authorized request by ${claims.organizationNameClaim} to read ${action.sendingOrg}/submissions"
            )

            val params = HistoryApiParameters(request.queryParameters)
            val facilityParams = FacilityListApiParameters(request.queryParameters)

            return HttpUtilities.okResponse(
                request,
                mapper.writeValueAsString(
                    deliveryFacade.findDeliveryFacilities(
                        ReportId.fromString(reportId),
                        params.sortDir,
                        facilityParams.sortColumn,
                        params.cursor,
                        params.pageSize,
                    )
                )
            )
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, HttpUtilities.errorJson(e.message ?: "Invalid Request"))
        }
    }

    data class FacilityListApiParameters(
        val sortColumn: DatabaseDeliveryAccess.FacilitySortColumn,
    ) {
        constructor(query: Map<String, String>) : this (
            sortColumn = extractSortCol(query),
        )

        companion object {
            /**
             * Convert sorting column from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractSortCol(query: Map<String, String>): DatabaseDeliveryAccess.FacilitySortColumn {
                val col = query["sortcol"]
                return if (col == null)
                    DatabaseDeliveryAccess.FacilitySortColumn.NAME
                else
                    DatabaseDeliveryAccess.FacilitySortColumn.valueOf(col)
            }
        }
    }
}