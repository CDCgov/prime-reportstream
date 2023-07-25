package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.JsonMappingException
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.okta.jwt.JwtVerifiers
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.db.ReportFileApiSearch
import gov.cdc.prime.router.db.ReportFileDatabaseAccess
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.util.UUID

class Facility private constructor(
    val organization: String?,
    val facility: String?,
    val location: String?,
    val CLIA: String?,
    val positive: Long?,
    val total: Long?
) {

    data class Builder(
        var organization: String? = null,
        var facility: String? = null,
        var location: String? = null,
        var CLIA: String? = null,
        var positive: Long? = null,
        var total: Long? = null
    ) {

        fun organization(organization: String) = apply { this.organization = organization }
        fun facility(facility: String) = apply { this.facility = facility }
        fun location(location: String) = apply { this.location = location }
        fun CLIA(CLIA: String) = apply { this.CLIA = CLIA }
        fun positive(positive: Long) = apply { this.positive = positive }
        fun total(total: Long) = apply { this.total = total }
        fun build() = Facility(organization, facility, location, CLIA, positive, total)
    }
}

class Action private constructor(
    val date: String?,
    val user: String?,
    val action: String?
) {

    data class Builder(
        var date: String? = null,
        var user: String? = null,
        var action: String? = null
    ) {

        fun date(date: String) = apply { this.date = date }
        fun user(user: String) = apply { this.user = user }
        fun action(action: String) = apply { this.action = action }
        fun build() = Action(date, user, action)
    }
}

class ReportView private constructor(
    val sent: Long?,
    val via: String?,
    val positive: Long?,
    val total: Long?,
    val fileType: String?,
    val type: String?,
    val reportId: String?,
    val expires: Long?,
    val sendingOrg: String?,
    val receivingOrg: String?,
    val receivingOrgSvc: String?,
    val facilities: ArrayList<Facility>?,
    val actions: ArrayList<Action>?,
    val displayName: String?,
    val content: String?,
    val fileName: String?,
    val mimeType: String?
) {
    data class Builder(
        var sent: Long? = null,
        var via: String? = null,
        var positive: Long? = null,
        var total: Long? = null,
        var fileType: String? = null,
        var type: String? = null,
        var reportId: String? = null,
        var expires: Long? = null,
        var sendingOrg: String? = null,
        var receivingOrg: String? = null,
        var receivingOrgSvc: String? = null,
        var facilities: ArrayList<Facility>? = ArrayList<Facility>(),
        var actions: ArrayList<Action>? = ArrayList<Action>(),
        var displayName: String? = null,
        var content: String? = null,
        var fileName: String? = null,
        var mimeType: String? = null
    ) {

        fun sent(sent: Long) = apply { this.sent = sent }
        fun via(via: String) = apply { this.via = via }
        fun positive(positive: Long) = apply { this.positive = positive }
        fun total(total: Long) = apply { this.total = total }
        fun fileType(fileType: String) = apply { this.fileType = fileType }
        fun type(type: String) = apply { this.type = type }
        fun reportId(reportId: String) = apply { this.reportId = reportId }
        fun expires(expires: Long) = apply { this.expires = expires }
        fun sendingOrg(sendingOrg: String) = apply { this.sendingOrg = sendingOrg }
        fun receivingOrg(receivingOrg: String) = apply { this.receivingOrg = receivingOrg }
        fun receivingOrgSvc(receivingOrgSvc: String) = apply { this.receivingOrgSvc = receivingOrgSvc }
        fun facilities(facilities: ArrayList<Facility>) = apply { this.facilities = facilities }
        fun actions(actions: ArrayList<Action>) = apply { this.actions = actions }
        fun displayName(displayName: String) = apply { this.displayName = displayName }
        fun content(content: String) = apply { this.content = content }
        fun fileName(fileName: String) = apply { this.fileName = fileName }
        fun mimeType(mimeType: String) = apply { this.mimeType = mimeType }

        fun build() = ReportView(
            sent,
            via,
            positive,
            total,
            fileType,
            type,
            reportId,
            expires,
            sendingOrg,
            receivingOrg,
            receivingOrgSvc,
            facilities,
            actions,
            displayName,
            content,
            fileName,
            mimeType
        )
    }
}

data class FileReturn(val content: String, val filename: String, val mimetype: String)

class GetReports :
    BaseHistoryFunction() {
    @FunctionName("getReports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReports",
            methods = [HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val organization = request.headers["organization"] ?: ""
        context.logger.info("organization = $organization")
        return if (organization.isBlank()) getReports(request, context) else getReports(request, context, organization)
    }

    /**
     * An endpoint available only to admins for a ReportFile.
     *
     * Primarily exists as a reference implementation for [ApiSearch]
     */
    @FunctionName("searchReports")
    fun searchReports(
        @HttpTrigger(
            name = "searchReports",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/reports/search"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf("*.*.primeadmin"))) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        val reportDbAccess = ReportFileDatabaseAccess()
        val search = try {
            ReportFileApiSearch.parse(request)
        } catch (ex: JsonMappingException) {
            return HttpUtilities.badRequestResponse(request, "Improperly formatted search")
        }
        val reports = reportDbAccess.getReports(search)
        return HttpUtilities.okJSONResponse(
            request,
            ApiResponse.buildFromApiSearch("ReportFile", search, reports)
        )
    }
}

class GetReportById :
    BaseHistoryFunction() {
    @FunctionName("getReportById")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReportById",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report/{reportId}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: String,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return getReportById(request, reportId, context)
    }
}

class GetFacilitiesByReportId :
    BaseHistoryFunction() {
    @FunctionName("getFacilitiesByReportId")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getFacilitiesByReportId",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report/{reportId}/facilities"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: String,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return getFacilitiesForReportId(request, reportId, context)
    }
}

open class BaseHistoryFunction : Logging {
    val DAYS_TO_SHOW = 30L
    val workflowEngine = WorkflowEngine()

    fun getReports(
        request: HttpRequestMessage<String?>,
        context: ExecutionContext,
        organizationName: String? = null
    ): HttpResponseMessage {
        logger.info("Checking authorization for getReports")
        val authClaims = checkAuthenticated(request, context)
            ?: return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
        var response: HttpResponseMessage
        try {
            logger.info("Getting reports for ${organizationName ?: authClaims.organization.name}")
            val headers = workflowEngine.db.fetchDownloadableReportFiles(
                OffsetDateTime.now().minusDays(DAYS_TO_SHOW),
                organizationName ?: authClaims.organization.name
            )
            @Suppress("NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER")
            val reports = headers.sortedByDescending { it.createdAt }.map {
                // removing the call for facilities for now so we can call a
                // method directly to just get the facilities and display them then
                val facilities = listOf<Facility>() // workflowEngine.db.getFacilitiesForDownloadableReport(it.reportId)
                val actions = arrayListOf<Action>()
                // get the org passed in
                val adminOrg = workflowEngine.settings.organizations.firstOrNull { org ->
                    org.name.lowercase() == organizationName
                }
                val header =
                    try {
                        workflowEngine.fetchHeader(
                            it.reportId,
                            adminOrg ?: authClaims.organization,
                            fetchBlobBody = false
                        )
                    } catch (ex: Exception) {
                        context.logger.severe("Unable to find file for ${it.reportId} ${ex.message}")
                        null
                    }
                val receiver = workflowEngine.settings.findReceiver("${it.receivingOrg}.${it.receivingOrgSvc}")

                val filename = Report.formExternalFilename(
                    it.bodyUrl,
                    it.reportId,
                    it.schemaName,
                    Report.Format.safeValueOf(it.bodyFormat),
                    it.createdAt
                )

                val content = if (header !== null && header.content !== null) String(header.content) else ""
                val mimeType = Report.Format.safeValueOf(it.bodyFormat).mimeType
                val externalOrgName = receiver?.displayName

                ReportView.Builder()
                    .reportId(it.reportId.toString())
                    .sent(it.createdAt.toEpochSecond() * 1000)
                    .via(it.bodyFormat)
                    .total(it.itemCount.toLong())
                    .fileType(it.bodyFormat)
                    .type("ELR")
                    .expires(it.createdAt.plusDays(DAYS_TO_SHOW).toEpochSecond() * 1000)
                    .facilities(ArrayList(facilities))
                    .actions(actions)
                    .receivingOrg(it.receivingOrg)
                    .receivingOrgSvc(externalOrgName ?: it.receivingOrgSvc)
                    .sendingOrg(it.sendingOrg ?: "")
                    .displayName(if (it.externalName.isNullOrBlank()) it.receivingOrgSvc else it.externalName)
                    .content(content) // don't get the content for now. that can get beefy
                    .fileName(filename)
                    .mimeType(mimeType)
                    .build()
            }

            response = request.createResponseBuilder(HttpStatus.OK)
                .body(reports)
                .header("Content-Type", "application/json")
                .build()
        } catch (ex: Exception) {
            context.logger.info("Exception during creating of reports list - file not found")
            context.logger.severe(ex.message)
            context.logger.severe(ex.stackTraceToString())
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File not found")
                .header("Content-Type", "text/html")
                .build()
        }
        return response
    }

    fun getReportById(
        request: HttpRequestMessage<String?>,
        reportIdIn: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        val authClaims = checkAuthenticated(request, context)
            ?: return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()

        var response: HttpResponseMessage
        try {
            // get the organization based on the header, if it exists, and if it
            // doesn't, use the organization from the authClaim
            val reportOrg = workflowEngine.settings.organizations.firstOrNull {
                it.name.lowercase() == request.headers["organization"]?.lowercase()
            } ?: authClaims.organization
            val reportId = ReportId.fromString(reportIdIn)
            val header = workflowEngine.fetchHeader(reportId, reportOrg)
            if (header.content == null || header.content.isEmpty())
                response = request.createResponseBuilder(HttpStatus.NOT_FOUND).build()
            else {
                val filename = Report.formExternalFilename(header)
                val mimeType = Report.Format.safeValueOf(header.reportFile.bodyFormat).mimeType
                val report = ReportView.Builder()
                    .reportId(header.reportFile.reportId.toString())
                    .sent(header.reportFile.createdAt.toEpochSecond() * 1000)
                    .via(header.reportFile.bodyFormat)
                    .total(header.reportFile.itemCount.toLong())
                    .fileType(header.reportFile.bodyFormat)
                    .type("ELR")
                    .expires(header.reportFile.createdAt.plusDays(DAYS_TO_SHOW).toEpochSecond() * 1000)
                    .receivingOrg(header.reportFile.receivingOrg)
                    .receivingOrgSvc(header.reportFile.receivingOrgSvc)
                    .sendingOrg(header.reportFile.sendingOrg ?: "")
                    .displayName(
                        if (header.reportFile.externalName.isNullOrBlank()) header.reportFile.receivingOrgSvc
                        else header.reportFile.externalName
                    )
                    .content(String(header.content))
                    .fileName(filename)
                    .mimeType(mimeType)
                    .build()

                response = request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(report)
                    .build()

                val actionHistory = ActionHistory(TaskAction.download)
                actionHistory.trackActionRequestResponse(request, response)
                actionHistory.trackActionReceiverInfo(header.reportFile.receivingOrg, header.reportFile.receivingOrgSvc)
                // Give the external report_file a new UUID, so we can track its history distinct from the
                // internal blob.   This is going to be very confusing.
                val externalReportId = UUID.randomUUID()
                actionHistory.trackDownloadedReport(
                    header,
                    filename,
                    externalReportId,
                    authClaims.userName,
                )
                actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, externalReportId))
                workflowEngine.recordAction(actionHistory)

                return response
            }
        } catch (ex: Exception) {
            context.logger.warning("Exception during download of $reportIdIn - file not found")
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File $reportIdIn not found")
                .header("Content-Type", "text/html")
                .build()
        }
        return response
    }

    fun getFacilitiesForReportId(
        request: HttpRequestMessage<String?>,
        reportId: String?,
        context: ExecutionContext
    ): HttpResponseMessage {
        // make sure we're auth'd and error out if we're not
        checkAuthenticated(request, context)
            ?: return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()

        return try {
            // get the facilities
            val facilities = workflowEngine.db.getFacilitiesForDownloadableReport(ReportId.fromString(reportId))
            request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(facilities)
                .build()
        } catch (ex: Exception) {
            context.logger.warning("Exception during download of $reportId - file not found")
            request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File $reportId not found")
                .header("Content-Type", "text/html")
                .build()
        }
    }

    data class AuthClaims(
        val userName: String,
        val organization: Organization
    )

    /**
     * returns null if not authorized, otherwise returns a set of claims.
     */
    private fun checkAuthenticated(request: HttpRequestMessage<String?>, context: ExecutionContext): AuthClaims? {
        val userName: String? /* Format: email */
        val requestOrgName: String? = request.headers["organization"] /* Format: xx-phd */
        val oktaOrganizations: List<String?>
        val accessOrgName: String? /* Format: xx-phd */
        var jwtToken: String? = request.headers["authorization"] /* Format: Bearer ... */

        /* INFO:
        *   JWT cannot be parsed by OktaAuthentication object here because Admins who do not
        *   exist in one or more DHxx_phd groups on Okta will be unable to see any receiver's
        *   reports.
        *
        *   To fix this, below we apply the same verifier, and then we see if oktaOrganizations.contains(orgName) OR
        *   oktaOrganizations.contains("DHPrimeAdmins") before authorizing.
        */
        if (jwtToken.isNullOrBlank() || requestOrgName.isNullOrBlank()) return null

        try {
            /* Trims Bearer off token */
            jwtToken = jwtToken.substring(7)
            /* Build our verifier to spec with what's in OktaAuthentication class */
            val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                .setIssuer("https://${System.getenv("OKTA_baseUrl")}/oauth2/default")
                .build()
            val jwt = jwtVerifier.decode(jwtToken)
                ?: throw Throwable("Error in validation of jwt token")

            /* Set claims from parsed jwt */
            userName = jwt.claims["sub"].toString()
            @Suppress("UNCHECKED_CAST")
            oktaOrganizations = jwt.claims["organization"] as List<String>

            accessOrgName = if (isAuthorizedIgnoreDashes(oktaOrganizations, requestOrgName)) requestOrgName else null
        } catch (ex: Throwable) {
            logger.warn("Unable authenticate user: ${ex.message}: ${ex.cause?.message ?: ""}")
            logger.debug(ex)
            return null
        }

        if (userName.isNullOrBlank() || accessOrgName.isNullOrBlank()) return null

        val dbOrganization = workflowEngine.settings.findOrganization(accessOrgName)
        return if (dbOrganization != null) {
            AuthClaims(userName, dbOrganization)
        } else {
            context.logger.info("User $userName failed auth: Organization $accessOrgName is unknown to the system.")
            null
        }
    }

    companion object {
        /**
         * @return true if the [oktaOrgs] list included a PrimeAdmin role,
         * or if [requestedOrgName] is one of the organizations in [oktaOrgs].
         * In all other cases, the user is not authorized, and this returns false.
         *
         * The comparison treats dashes and underscores as identical.
         *
         * This is needed, temporarily, as a step to support fixing group names in Okta to exactly match
         * organization names in settings.   Detailed explanation is in #6263.
         */
        fun isAuthorizedIgnoreDashes(oktaOrgs: List<String?>, requestedOrgName: String): Boolean {
            if (requestedOrgName.isBlank()) return false
            oktaOrgs.forEach {
                when {
                    it == null -> { } // do nothing ; skip.
                    it.contains("DHPrimeAdmins") -> return true
                    it.replace('_', '-') == "DH${requestedOrgName.replace('_', '-')}"
                    -> return true
                }
            }
            return false
        }
    }
}