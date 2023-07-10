package gov.cdc.prime.router.history.azure

import com.fasterxml.jackson.annotation.JsonProperty
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.ApiResponse
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.history.db.DeliveryApiSearch
import gov.cdc.prime.router.history.db.DeliveryDatabaseAccess
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.history.db.SubmitterApiSearch
import gov.cdc.prime.router.history.db.SubmitterDatabaseAccess
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import java.util.UUID

/**
 * Deliveries API
 * Returns a list of Actions from `public.action`. combined with `public.report_file`.
 *
 * @property reportFileFacade Facade class containing business logic to handle the data.
 * @property workflowEngine Container for helpers and accessors used when dealing with the workflow.
 */
class DeliveryFunction(
    val deliveryFacade: DeliveryFacade = DeliveryFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine()
) : ReportFileFunction(
    deliveryFacade,
    workflowEngine
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    private val submitterDatabaseAccess = SubmitterDatabaseAccess()
    private val deliveryDatabaseAccess = DeliveryDatabaseAccess()

    /**
     * Authorization and shared logic uses the organization name without the service
     * We store the service name here to pass to the facade
     */
    var receivingOrgSvc: String? = null

    /**
     * Verify the correct name for an organization based on the name
     *
     * @param organization Name of organization and optionally a receiver channel in the format {orgName}.{receiver}
     * @return Name for the organization
     */
    override fun validateOrgSvcName(organization: String): String? {
        return if (organization.contains(Sender.fullNameSeparator)) {
            workflowEngine.settings.findReceiver(organization).also { receivingOrgSvc = it?.name }?.organizationName
        } else {
            workflowEngine.settings.findOrganization(organization)?.name
        }
    }

    /**
     * Verify that the action being checked has the correct data/parameters
     * for the type of report being viewed.
     *
     * @param action DB Action that we are reviewing
     * @return true if action is valid, else false
     */
    override fun actionIsValid(action: Action): Boolean {
        return action.actionName == TaskAction.batch || action.actionName == TaskAction.send
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
            params.pageSize
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
        return deliveryFacade.findDetailedDeliveryHistory(action.actionId)
    }

    @FunctionName("getDeliveriesV1")
    fun getDeliveriesV1(
        @HttpTrigger(
            name = "getDeliveriesV1",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/receivers/{receiverName}/deliveries"
        ) request: HttpRequestMessage<String?>,
        @BindingName("receiverName") receiverName: String
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        val receiver =
            BaseEngine.settingsProviderSingleton.findReceiver(receiverName) ?: return HttpUtilities.notFoundResponse(
                request,
                "No such receiver $receiverName"
            )
        if (claims == null || !claims.authorizedForSendOrReceive(
                requiredOrganization = receiver.organizationName,
                request = request
            )
        ) {
            logger.warn("User '${claims?.userName}' FAILED authorization for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        request.body ?: HttpUtilities.badRequestResponse(request, "Search body must be included")
        val search = DeliveryApiSearch.parse(request)
        val results = deliveryDatabaseAccess.getDeliveries(search, receiver)
        val response = ApiResponse.buildFromApiSearch("delivery", search, results)
        return HttpUtilities.okJSONResponse(request, response)
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
        @BindingName("organization") organization: String
    ): HttpResponseMessage {
        return this.getListByOrg(request, organization)
    }

    /**
     * Get expanded details for a single report
     *
     * @param request HTML request body.
     * @param id Report or Delivery id
     * @return json formatted delivery
     */
    @FunctionName("getDeliveryDetails")
    fun getDeliveryDetails(
        @HttpTrigger(
            name = "getDeliveryDetails",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{id}/delivery"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: String
    ): HttpResponseMessage {
        return this.getDetailedView(request, id)
    }

    /**
     * Get a sortable list of delivery facilities
     *
     * @param request HTML request body.
     * @param id Report or Delivery id for the report to get facilities from
     * @return JSON of the facility list or errors.
     */
    @FunctionName("getDeliveryFacilities")
    fun getDeliveryFacilities(
        @HttpTrigger(
            name = "getDeliveryFacilities",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{id}/facilities"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: String
    ): HttpResponseMessage {
        try {
            // Do authentication
            val authResult = this.authSingleBlocks(request, id)

            return if (authResult != null) {
                authResult
            } else {
                val actionId = id.toLongOrNull()

                val reportId = if (actionId == null) {
                    this.toUuidOrNull(id)
                } else {
                    deliveryFacade.fetchReportForActionId(actionId)?.reportId
                }

                val facilities = deliveryFacade.findDeliveryFacilities(
                    reportId!!,
                    HistoryApiParameters(request.queryParameters).sortDir,
                    FacilityListApiParameters(request.queryParameters).sortColumn
                )

                HttpUtilities.okResponse(
                    request,
                    mapper.writeValueAsString(
                        facilities.map {
                            Facility(
                                it.testingLabName,
                                it.location,
                                it.testingLabClia,
                                it.positive,
                                it.countRecords
                            )
                        }
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, HttpUtilities.errorJson(e.message ?: "Invalid Request"))
        } catch (ex: IllegalStateException) {
            logger.error(ex)
            // Errors above are actionId or UUID not found errors.
            return HttpUtilities.notFoundResponse(request, ex.message)
        }
    }

    /**
     * Fetches the items that were contained in the passed in report ID walking up the report lineage if necessary
     */
    @FunctionName("getReportItemsV1")
    fun getReportItems(
        @HttpTrigger(
            name = "getReportItemsV1",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/report/{reportId}/items"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: UUID
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf("*.*.primeadmin"))) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }

        val reportGraph = ReportGraph(workflowEngine.db)
        val metadata = reportGraph.getMetadataForReports(listOf(reportId))

        return HttpUtilities.okJSONResponse(request, metadata)
    }

    /**
     * Container for extracted History API parameters exclusively related to Deliveries.
     *
     * @property sortColumn sort the table by specific column; default created_at.
     */
    data class FacilityListApiParameters(
        val sortColumn: DatabaseDeliveryAccess.FacilitySortColumn
    ) {
        constructor(query: Map<String, String>) : this (
            sortColumn = extractSortCol(query)
        )

        companion object {
            /**
             * Convert sorting column from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractSortCol(query: Map<String, String>): DatabaseDeliveryAccess.FacilitySortColumn {
                val col = query["sortcol"]
                return if (col == null) {
                    DatabaseDeliveryAccess.FacilitySortColumn.NAME
                } else {
                    DatabaseDeliveryAccess.FacilitySortColumn.valueOf(col)
                }
            }
        }
    }

    /**
     * API for searching for submitters for a specific receiver
     */
    @FunctionName("getSubmittersV1")
    fun getSubmitters(
        @HttpTrigger(
            name = "getSubmittersV1",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/receivers/{receiverName}/deliveries/submitters/search"
        ) request: HttpRequestMessage<String?>,
        @BindingName("receiverName") receiverName: String
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        val receiver =
            BaseEngine.settingsProviderSingleton.findReceiver(receiverName) ?: return HttpUtilities.notFoundResponse(
                request,
                "No such receiver $receiverName"
            )
        if (claims == null || !claims.authorizedForSendOrReceive(
                requiredOrganization = receiver.organizationName,
                request = request
            )
        ) {
            logger.warn("User '${claims?.userName}' FAILED authorization for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        request.body ?: HttpUtilities.badRequestResponse(request, "Search body must be included")
        val search = SubmitterApiSearch.parse(request)
        val results = submitterDatabaseAccess.getSubmitters(search, receiver)
        val response = ApiResponse.buildFromApiSearch("submitter", search, results)
        return HttpUtilities.okJSONResponse(request, response)
    }

    /**
     * Container for the output data of a facility
     *
     * @property facility the full name of the facility
     * @property location the city and state of the facility
     * @property clia The CLIA number (10-digit alphanumeric) of the facility
     * @property positive the result (conclusion) of the test. 0 = negative (good usually)
     * @property total number of facilities included in the object
     */
    data class Facility(
        val facility: String?,
        val location: String?,
        @JsonProperty("CLIA")
        val clia: String?,
        val positive: Long?,
        val total: Long?
    )
}