package gov.cdc.prime.router.azure

import com.azure.storage.blob.models.BlobStorageException
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
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Level
import kotlin.collections.ArrayList

class Facility private constructor(
    val organization: String?,
    val facility: String?,
    val CLIA: String?,
    val positive: Long?,
    val total: Long?
) {

    data class Builder(
        var organization: String? = null,
        var facility: String? = null,
        var CLIA: String? = null,
        var positive: Long? = null,
        var total: Long? = null
    ) {

        fun organization(organization: String) = apply { this.organization = organization }
        fun facility(facility: String) = apply { this.facility = facility }
        fun CLIA(CLIA: String) = apply { this.CLIA = CLIA }
        fun positive(positive: Long) = apply { this.positive = positive }
        fun total(total: Long) = apply { this.total = total }
        fun build() = Facility(organization, facility, CLIA, positive, total)
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
            val reports = headers.sortedByDescending { it.createdAt }.mapNotNull {
                val facilities = workflowEngine.db.getFacilitiesForDownloadableReport(it.reportId)
                val actions = arrayListOf<Action>()
                // get the org passed in
                val adminOrg = workflowEngine.settings.organizations.firstOrNull { org ->
                    org.name.lowercase() == organizationName
                }
                val header = try {
                    workflowEngine.fetchHeader(it.reportId, adminOrg ?: authClaims.organization)
                } catch (ex: BlobStorageException) {
                    context.logger.severe("Unable to find file for ${it.reportId} ${ex.message}")
                    null
                }

                if (header != null) {
                    val content = if (header.content !== null) String(header.content) else ""
                    val filename = Report.formExternalFilename(header)
                    val mimeType = Report.Format.safeValueOf(header.reportFile.bodyFormat).mimeType
                    val externalOrgName = header.receiver?.displayName

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
                        .displayName(if (it.externalName.isNullOrBlank()) it.receivingOrgSvc else it.externalName)
                        .content("") // don't get the content for now. that can get beefy
                        .fileName(filename)
                        .mimeType(mimeType)
                        .build()
                } else {
                    null
                }
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

                val fileReturn = FileReturn(String(header.content), filename, mimeType)
                response = request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(fileReturn)
                    .build()

                val actionHistory = ActionHistory(TaskAction.download, context)
                actionHistory.trackActionRequestResponse(request, response)
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
                WorkflowEngine().recordAction(actionHistory)

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

    data class AuthClaims(
        val userName: String,
        val organization: Organization
    )

    /**
     * returns null if not authorized, otherwise returns a set of claims.
     */
    fun checkAuthenticated(request: HttpRequestMessage<String?>, context: ExecutionContext): AuthClaims? {
        var userName = ""
        // orgs in the settings table of the database have a format of "zz-phd",
        // while the auth service claims has a format of "DHzz_phd"
        // claimsOrgName will have the format of "DHzz_phd"
        val claimsOrgName = request.headers["organization"] ?: ""

        // orgName will have the format of "zz-phd" and is used to look up in the settings table of the database
        var orgName = getOrgNameFromHeader(claimsOrgName)

        var jwtToken = request.headers["authorization"] ?: ""

        jwtToken = if (jwtToken.length > 7) jwtToken.substring(7) else ""

        if (jwtToken.isNotBlank()) {
            try {
                // get the access token verifier
                val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                    .setIssuer("https://${System.getenv("OKTA_baseUrl")}/oauth2/default")
                    .build()
                // get it to decode the token from the header
                val jwt = jwtVerifier.decode(jwtToken)
                    ?: throw Throwable("Error in validation of jwt token")
                // get the user name and org
                userName = jwt.claims["sub"].toString()
                val orgs = jwt.claims["organization"]
                @Suppress("UNCHECKED_CAST")
                val org = if (orgs !== null) (orgs as List<String>)[0] else ""
                orgName = if (org.length > 3) org.substring(2) else ""
            } catch (ex: Throwable) {
                context.logger.log(Level.WARNING, "Error in verification of token", ex)
                return null
            }
        }
        if (userName.isNotBlank() && orgName.isNotBlank()) {
            val organization = WorkflowEngine().settings.findOrganization(orgName.replace('_', '-'))
            if (organization != null) {
                return AuthClaims(userName, organization)
            } else {
                context.logger.info("User $userName failed auth: Organization $orgName is unknown to the system.")
            }
        }
        return null
    }

    fun getOrgNameFromHeader(orgNameHeader: String): String {
        return if (orgNameHeader.isNotEmpty()) orgNameHeader.substring(2).replace("_", "-") else ""
    }
}