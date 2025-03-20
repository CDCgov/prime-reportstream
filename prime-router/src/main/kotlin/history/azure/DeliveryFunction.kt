package gov.cdc.prime.router.history.azure

import com.fasterxml.jackson.annotation.JsonProperty
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.ApiResponse
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.history.db.DeliveryApiSearch
import gov.cdc.prime.router.history.db.DeliveryDatabaseAccess
import gov.cdc.prime.router.history.db.DeliveryHistoryApiSearch
import gov.cdc.prime.router.history.db.DeliveryHistoryDatabaseAccess
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.history.db.SubmitterApiSearch
import gov.cdc.prime.router.history.db.SubmitterDatabaseAccess
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.InvalidParameterException
import java.util.UUID

/**
 * Deliveries API
 * Returns a list of Actions from `public.action`. combined with `public.report_file`.
 *
 * @property reportFileFacade Facade class containing business logic to handle the data.
 * @property workflowEngine Container for helpers and accessors used when dealing with the workflow.
 * @property reportService Service for querying graphs of reports or items
 */
class DeliveryFunction(
    val deliveryFacade: DeliveryFacade = DeliveryFacade.instance,
    workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val reportService: ReportService = ReportService(),
) : ReportFileFunction(
    deliveryFacade,
    workflowEngine
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    private val submitterDatabaseAccess = SubmitterDatabaseAccess()
    private val deliveryDatabaseAccess = DeliveryDatabaseAccess()
    private val deliveryHistoryDatabaseAccess = DeliveryHistoryDatabaseAccess(workflowEngine = workflowEngine)

    /**
     * Authorization and shared logic uses the organization name without the service
     * We store the service name here to pass to the facade
     */
    var receivingOrgSvc: String? = null

    /**
     * Container for extracted Delivery History API parameters.
     *
     * @property reportId is the reportId to get results for.
     * @property fileName is the fileName to get results for.
     * @property receivingOrgSvcStatus is the customer status of the receiver to get results for.
     */
    data class DeliveryHistoryApiParameters(
        val reportId: String?,
        val fileName: String?,
        val receivingOrgSvcStatus: List<CustomerStatus>?,
    ) {
        constructor(query: Map<String, String>) : this(
            reportId = query["reportId"],
            fileName = extractFileName(query),
            receivingOrgSvcStatus = extractReceivingOrgSvcStatus(query),
        )

        companion object {
            /**
             * Convert fileName from query into param used for the DB
             * @param query Incoming query params
             * @return encoded param
             */
            fun extractFileName(query: Map<String, String>): String? = if (query["fileName"] != null) {
                    URLEncoder.encode(query["fileName"], Charset.defaultCharset())
                } else {
                    null
                }

            fun extractReceivingOrgSvcStatus(query: Map<String, String>): List<CustomerStatus>? = try {
                    query["receivingOrgSvcStatus"]?.split(",")?.map { CustomerStatus.valueOf(it) }
                } catch (e: IllegalArgumentException) {
                    throw InvalidParameterException("Invalid value for receivingOrgSvcStatus.")
                }
        }
    }

    /**
     * Verify the correct name for an organization based on the name
     *
     * @param organization Name of organization and optionally a receiver channel in the format {orgName}.{receiver}
     * @return Name for the organization
     */
    override fun validateOrgSvcName(
        organization: String,
    ): String? = if (organization.contains(Sender.fullNameSeparator)) {
            workflowEngine.settings.findReceiver(organization).also { receivingOrgSvc = it?.name }?.organizationName
        } else {
            workflowEngine.settings.findOrganization(organization)?.name
        }

    /**
     * Verify that the action being checked has the correct data/parameters
     * for the type of report being viewed.
     *
     * @param action DB Action that we are reviewing
     * @return true if action is valid, else false
     */
    override fun actionIsValid(
        action: Action,
    ): Boolean = action.actionName == TaskAction.batch || action.actionName == TaskAction.send

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
            params.reportId,
            params.fileName,
            params.receivingOrgSvcStatus
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
    override fun singleDetailedHistory(
        id: String,
        txn: DataAccessTransaction,
        action: Action,
    ): DeliveryHistory? = deliveryFacade.findDetailedDeliveryHistory(id, action.actionId)

    @FunctionName("getDeliveriesV1")
    fun getDeliveriesV1(
        @HttpTrigger(
            name = "getDeliveriesV1",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/receivers/{receiverName}/deliveries"
        ) request: HttpRequestMessage<String?>,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        val receiver =
            BaseEngine.settingsProviderSingleton.findReceiver(receiverName) ?: return HttpUtilities.notFoundResponse(
                request,
                "No such receiver $receiverName"
            )
        if (claims == null ||
            !claims.authorizedForSendOrReceive(
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

    @FunctionName("getDeliveriesHistory")
    fun getDeliveriesHistory(
        @HttpTrigger(
            name = "getDeliveriesHistory",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/waters/org/{organization}/deliveries"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organization") organization: String,
    ): HttpResponseMessage {
        try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            val userOrganization = this.validateOrgSvcName(organization)
                ?: return HttpUtilities.notFoundResponse(
                    request,
                    "$organization: invalid organization or service identifier"
                )

            if (!reportFileFacade.checkAccessAuthorizationForOrg(claims, userOrganization, null, request)) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claims scopes are ${claims.scopes} but client id is $userOrganization"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.scopes} to getListByOrg on organization $userOrganization."
            )

            if (DeliveryHistoryApiParameters(request.queryParameters).reportId != null &&
                DeliveryHistoryApiParameters(request.queryParameters).fileName != null
            ) {
                return HttpUtilities.badRequestResponse(request, "Either reportId or fileName can be provided")
            }

            request.body ?: HttpUtilities.badRequestResponse(request, "Search body must be included")
            val search = DeliveryHistoryApiSearch.parse(request)
            val params = DeliveryHistoryApiParameters(request.queryParameters)
            val results = deliveryHistoryDatabaseAccess.getDeliveries(
                search,
                userOrganization,
                receivingOrgSvc,
                params.receivingOrgSvcStatus,
                params.reportId,
                params.fileName
            )
            val response = ApiResponse.buildFromApiSearch("delivery_history", search, results)
            return HttpUtilities.okJSONResponse(request, response)
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, HttpUtilities.errorJson(e.message ?: "Invalid Request"))
        }
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
    ): HttpResponseMessage = this.getListByOrg(request, organization)

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
        @BindingName("id") id: String,
    ): HttpResponseMessage = this.getDetailedView(request, id)

    /**
     * Endpoint for intermediary receivers to verify status of messages. It passes
     * a null engine to the retrieveMetadata function because Azure gets upset if there
     * are any non-annotated parameters in the method signature other than ExecutionContext
     * and we needed the engine to be a parameter so it can be mocked for tests
     *
     */
    @FunctionName("getEtorMetadataForDelivery")
    fun getEtorMetadata(
        @HttpTrigger(
            name = "getEtorMetadataForDelivery",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/report/{reportId}/delivery/etorMetadata"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: UUID,
        context: ExecutionContext,
    ): HttpResponseMessage = this.retrieveETORIntermediaryMetadata(request, reportId, context, null)

    /**
     * Function for finding the associated report ID that the intermediary knows about given the report ID that the receiver is
     * given from report stream
     */
    override fun getLookupId(reportId: UUID): UUID? {
        // the delivery endpoint is called by the final receiver with a sent report ID, where TI
        // knows about the related submission report ID
        try {
            val root = reportService.getRootReport(reportId)
            return root.reportId
        } catch (ex: IllegalStateException) {
            logger.error("Unable to locate root report for report $reportId")
            return null
        }
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
        @BindingName("id") id: String,
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
        @BindingName("reportId") reportId: UUID,
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
    data class FacilityListApiParameters(val sortColumn: DatabaseDeliveryAccess.FacilitySortColumn) {
        constructor(query: Map<String, String>) : this(
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
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        val receiver =
            BaseEngine.settingsProviderSingleton.findReceiver(receiverName) ?: return HttpUtilities.notFoundResponse(
                request,
                "No such receiver $receiverName"
            )
        if (claims == null ||
            !claims.authorizedForSendOrReceive(
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
        val total: Long?,
    )
}