package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.history.ReportHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import gov.cdc.prime.router.transport.RESTTransport
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID
import java.util.logging.Logger

/**
 * History API
 * Returns a list of Actions from `public.action`. combined with `public.report_file`.
 *
 * @property reportFileFacade Facade class containing business logic to handle the data.
 * @property workflowEngine Container for helpers and accessors used when dealing with the workflow.
 */
abstract class ReportFileFunction(
    val reportFileFacade: ReportFileFacade,
    internal val workflowEngine: WorkflowEngine = WorkflowEngine(),
) : Logging {

    internal val intermediaryReceiverName = "flexion.etor-service-receiver-orders"

    /**
     * Helper to store currently loaded action to prevent extra DB calls
     */
    private var currentAction: Action? = null

    /**
     * Verify the correct name for an organization based on the name
     *
     * @param organization Name of organization and optionally a service in the format {orgName}.{service}
     * @return Name for the organization
     */
    abstract fun validateOrgSvcName(organization: String): String?

    /**
     * Get history entries as a list
     *
     * @param queryParams Parameters extracted from the HTTP Request
     * @param userOrgName Name of the organization
     * @return json list of history
     */
    abstract fun historyAsJson(queryParams: MutableMap<String, String>, userOrgName: String): String

    /**
     * Get expanded details for a single report
     *
     * @param queryParams Parameters extracted from the HTTP Request
     * @param action Action from which the data for the report is loaded
     * @return
     */
    abstract fun singleDetailedHistory(id: String, txn: DataAccessTransaction, action: Action): ReportHistory?

    /**
     * Verify that the action being checked has the correct data/parameters
     * for the type of report being viewed.
     *
     * @param action DB Action that we are reviewing
     * @return true if action is valid, else false
     */
    abstract fun actionIsValid(action: Action): Boolean

    /**
     * Get a list of reports for a given organization.
     *
     * @param request HTML request body.
     * @param organization Name of the organization sending or receiving this report.
     * @return JSON of the report list or errors.
     */
    fun getListByOrg(
        request: HttpRequestMessage<String?>,
        organization: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            val userOrgName = this.validateOrgSvcName(organization)
                ?: return HttpUtilities.notFoundResponse(
                    request,
                    "$organization: invalid organization or service identifier"
                )

            // Authorize based on: org name in the path == org name in claim.  Or be a prime admin.
            if (!reportFileFacade.checkAccessAuthorizationForOrg(claims, userOrgName, null, request)) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claims scopes are ${claims.scopes} but client id is $userOrgName"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.scopes} to getListByOrg on organization $userOrgName."
            )

            if (HistoryApiParameters(request.queryParameters).reportId != null &&
                HistoryApiParameters(request.queryParameters).fileName != null
            ) {
                return HttpUtilities.badRequestResponse(request, "Either reportId or fileName can be provided")
            }

            return HttpUtilities.okResponse(request, this.historyAsJson(request.queryParameters, userOrgName))
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, HttpUtilities.errorJson(e.message ?: "Invalid Request"))
        }
    }

    /**
     * Get a single element that matches the given id.
     *
     * @param request HTML request body.
     * @param id Either a reportId or actionId to look for matches on.
     * @return JSON of the found match or an error explaining what happened.
     */
    fun getDetailedView(
        request: HttpRequestMessage<String?>,
        id: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val authResult = this.authSingleBlocks(request, id)

            return if (authResult != null) {
                authResult
            } else {
                val action = this.actionFromId(id)
                val history = workflowEngine.db.transactReturning { txn ->
                    this.singleDetailedHistory(id, txn, action)
                }
                HttpUtilities.okJSONResponse(request, history)
            }
        } catch (e: DataAccessException) {
            logger.error("Unable to fetch history for ID $id", e)
            return HttpUtilities.internalErrorResponse(request)
        } catch (ex: IllegalStateException) { // actionId or UUID not found
            val submission = SubmissionTableService.getInstance().getSubmission(id, "Received")

            return if (submission == null) {
                logger.error(ex)
                HttpUtilities.notFoundResponse(request, ex.message)
            } else {
                HttpUtilities.okJSONResponse(request, submission)
            }
        }
    }

    /**
     * Check for auth issues when fetching a single result.
     *
     * @param request HTML request body.
     * @param id Either a reportId or actionId to look for matches on.
     * @return The error response if found, or null if all is well.
     */
    fun authSingleBlocks(
        request: HttpRequestMessage<String?>,
        id: String,
    ): HttpResponseMessage? {
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

        logger.debug("Authenticated request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")

        val action: Action = this.actionFromId(id)

        return if (!this.actionIsValid(action)) {
            HttpUtilities.notFoundResponse(request, "$id is not a valid report")
        }
        // todo bug:  we need to find the report_file id's receiving_org, and send it here.
        else if (!reportFileFacade.checkAccessAuthorizationForAction(claims, action, request)) {
            logger.warn(
                "Invalid Authorization for user ${claims.userName}:" +
                    " ${request.httpMethod}:${request.uri.path}"
            )
            HttpUtilities.unauthorizedResponse(request, authorizationFailure)
        } else {
            currentAction = action
            logger.debug(
                "Authorized request with scope ${claims.scopes} to read ${action.sendingOrg}/submissions"
            )
            null
        }
    }

    /**
     * Function to return metadata of a single report from the CDC Intermediary.
     * The [reportId] is a valid report UUID. This function is for the Intermediary only, please don't update
     * without contacting that engineering team
     */
    fun retrieveETORIntermediaryMetadata(
        request: HttpRequestMessage<String?>,
        reportId: UUID,
        context: ExecutionContext,
        engine: HttpClientEngine?,
        etorTiBaseUrl: String? = System.getenv("ETOR_TI_baseurl"),
    ): HttpResponseMessage {
        if (etorTiBaseUrl == null) {
            return HttpUtilities.httpResponse(
                request,
                "Environment variable ETOR_TI_baseurl is not set. Set this variable and run the TI service.",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }

        val authResult = this.authSingleBlocks(request, reportId.toString())

        if (authResult != null) {
            return authResult
        }

        var response: HttpResponse?
        val receiver = workflowEngine.settings.findReceiver(this.intermediaryReceiverName)
        val client = if (engine == null) HttpClient() else HttpClient(engine)
        val restTransportInfo = receiver?.transport as RESTTransportType
        val (credential, jksCredential) = RESTTransport().getCredential(restTransportInfo, receiver)
        val logger: Logger = context.logger

        val httpHeaders = RESTTransport().getHeaders(restTransportInfo, reportId.toString())
        val accessToken = runBlocking {
            async {
                RESTTransport().getOAuthToken(
                    restTransportInfo,
                    jksCredential,
                    credential,
                    logger
                )
            }.await()
        }

        val lookupId = this.getLookupId(reportId)
            ?: return HttpUtilities.notFoundResponse(request, "lookup Id not found")

        val (status, responseBody) = runBlocking {
            async {
                response = client.get("$etorTiBaseUrl/v1/etor/metadata/" + lookupId) {
                    httpHeaders.forEach { entry ->
                        headers.append(entry.key, entry.value)
                    }

                    headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
                }

                Pair<HttpStatusCode, String>(response!!.status, response!!.body())
            }.await()
        }

        if (status == HttpStatusCode.NotFound) {
            return HttpUtilities.notFoundResponse(request, "metadata not found")
        }

        if (status.value >= 300) {
            return HttpUtilities.internalErrorResponse(request)
        }

        return HttpUtilities.okResponse(request, responseBody)
    }

    /**
     * Use the specified report ID to find the lookup ID for the ETOR TI to use to retrieve metadata
     *
     * @param reportId DB Action that we are reviewing
     * @return the string lookup ID if found, otherwise and empty string
     */
    abstract fun getLookupId(reportId: UUID): UUID?

    /**
     * Look for an action related to the given id.
     * To reduce DB hits, if this object has a value set on currentAction,
     * its id will be compared with the input id and returned.
     *
     * @param id Either a reportId or actionId to look for matches on.
     * @return The action related to the given id.
     */
    fun actionFromId(id: String): Action {
        // Figure out whether we're dealing with an action_id or a report_id.
        val actionId = id.toLongOrNull()
        return if (currentAction != null && currentAction!!.actionId == actionId) {
            currentAction!!
        } else if (actionId == null) {
            val reportId = toUuidOrNull(id) ?: error("Bad format: $id must be a num or a UUID")
            reportFileFacade.fetchActionForReportId(reportId) ?: error("No such reportId: $reportId")
        } else {
            reportFileFacade.fetchAction(actionId) ?: error("No such actionId $actionId")
        }
    }

    /**
     * Container for extracted History API parameters.
     *
     * @property sortDir sort the table in ASC or DESC order.
     * @property sortColumn sort the table by specific column; default created_at.
     * @property cursor is the OffsetDateTime of the last result in the previous list.
     * @property since is the OffsetDateTime that dictates how far back returned results date.
     * @property until is the OffsetDateTime that dictates how recently returned results date.
     * @property pageSize is an Integer used for setting the number of results per page.
     * @property showFailed whether to include actions that failed to be sent.
     * @property reportId is the reportId to get results for.
     * @property fileName is the fileName to get results for.
     * @property receivingOrgSvcStatus is the customer status of the receiver to get results for.
     */
    data class HistoryApiParameters(
        val sortDir: HistoryDatabaseAccess.SortDir,
        val sortColumn: HistoryDatabaseAccess.SortColumn,
        val cursor: OffsetDateTime?,
        val since: OffsetDateTime?,
        val until: OffsetDateTime?,
        val pageSize: Int,
        val showFailed: Boolean,
        val reportId: String?,
        val fileName: String?,
        val receivingOrgSvcStatus: List<CustomerStatus>?,
    ) {
        constructor(query: Map<String, String>) : this(
            sortDir = extractSortDir(query),
            sortColumn = extractSortCol(query),
            cursor = extractDateTime(query, "cursor"),
            since = extractDateTime(query, "since"),
            until = extractDateTime(query, "until"),
            pageSize = extractPageSize(query),
            showFailed = extractShowFailed(query),
            reportId = query["reportId"],
            fileName = extractFileName(query),
            receivingOrgSvcStatus = extractReceivingOrgSvcStatus(query),
        )

        companion object {
            /**
             * Convert sorting direction from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractSortDir(query: Map<String, String>): HistoryDatabaseAccess.SortDir {
                val sort = query["sortdir"]
                return if (sort == null) {
                    HistoryDatabaseAccess.SortDir.DESC
                } else {
                    HistoryDatabaseAccess.SortDir.valueOf(sort)
                }
            }

            /**
             * Convert sorting column from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractSortCol(query: Map<String, String>): HistoryDatabaseAccess.SortColumn {
                val col = query["sortcol"]
                // check if col matches one of the values in HistoryDatabaseAccess.SortColumn
                return if (col != null && HistoryDatabaseAccess.SortColumn.values().any { it.name == col }) {
                    HistoryDatabaseAccess.SortColumn.valueOf(col)
                } else {
                    HistoryDatabaseAccess.SortColumn.CREATED_AT
                }
            }

            /**
             * Convert date time fields from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractDateTime(query: Map<String, String>, name: String): OffsetDateTime? {
                val dt = query[name]
                return if (dt != null) {
                    try {
                        OffsetDateTime.parse(dt)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("\"$name\" must be a valid datetime")
                    }
                } else {
                    null
                }
            }

            /**
             * Convert page size from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractPageSize(query: Map<String, String>): Int {
                val size = query.getOrDefault("pageSize", "50").toInt()
                require(size > 0) { "Page size must be a positive integer" }
                return size
            }

            /**
             * Convert show failed from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractShowFailed(query: Map<String, String>): Boolean = query["showfailed"]?.toBoolean() ?: false

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

            fun extractReceivingOrgSvcStatus(query: Map<String, String>): List<CustomerStatus>? =
                query["receivingOrgSvcStatus"]?.split(",")?.map {
                    CustomerStatus.valueOf(it)
            }
        }
    }

    /**
     * Utility function.  Mimic String.toLongOrNull()
     *
     * @param str Potential UUID
     * @return a valid UUID, or null if this [str] cannot be parsed into a valid UUID.
     */
    internal fun toUuidOrNull(str: String): UUID? = try {
            UUID.fromString(str)
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid format for report ID: $str", e)
            null
        }
}