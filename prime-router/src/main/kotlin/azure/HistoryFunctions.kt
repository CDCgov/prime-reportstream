package gov.cdc.prime.router.azure

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
import com.google.errorprone.annotations.CompatibleWith
import gov.cdc.prime.router.Organization
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
 
class Facility private constructor(
    val organization: String?,
    val facility: String?,
    val CLIA: String?,
    val positive: Long?,
    val total: Long? ){
    
    data class Builder(
        var organization: String? = null,
        var facility: String? = null,
        var CLIA: String? = null,
        var positive: Long? = null,
        var total: Long? = null ){
        
        fun organization( organization: String ) = apply { this.organization = organization }
        fun facility( facility: String ) = apply { this.facility = facility }
        fun CLIA( CLIA: String ) = apply { this.CLIA = CLIA }
        fun positive( positive: Long ) = apply { this.positive = positive }
        fun total( total: Long ) = apply { this.total = total }
        fun build() = Facility( organization, facility, CLIA, positive, total )
    }
}

class Action private constructor(
    val date: Int?,
    val user: String?,
    val action: String? ){
    
    data class Builder(
        var date: Int? = null,
        var user: String? = null,
        var action: String? = null ){

        fun date( date: Int ) = apply { this.date = date }
        fun user( user: String ) = apply { this.user = user }
        fun action( action: String ) = apply { this.action = action }
        fun build() = Action( date, user, action )
    }
}

class Report private constructor( 
    val sent: String?,
    val via: String?,
    val positive: Long?,
    val total: Long?,
    val fileType: String?,
    val type: String?,
    val reportId: String?,
    val expires: Long?,
    val facilities: Array<Facility>?,
    val actions: Array<Action>? ){
    
    data class Builder(
        var sent: String? = null,
        var via: String? = null,
        var positive: Long? = null,
        var total: Long? = null, 
        var fileType: String? = null,
        var type: String? = null,
        var reportId: String? = null, 
        var expires: Long? = null,
        var facilities: Array<Facility>? = null,
        var actions: Array<Action>? = null ){

        fun sent( sent: String ) = apply { this.sent = sent }
        fun via( via: String ) = apply { this.via = via }
        fun positive( positive: Long ) = apply { this.positive = positive }
        fun total( total: Long ) = apply{ this.total = total }
        fun fileType( fileType: String ) = apply { this.fileType = fileType }
        fun type( type: String ) = apply { this.type = type }
        fun reportId( reportId: String ) = apply { this.reportId = reportId }
        fun expires( expires: Long ) = apply { this.expires = expires }
        fun facilities( facilities: Array<Facility> ) = apply { this.facilities = facilities }
        fun actions( actions: Array<Action> ) = apply { this.actions = actions }
        fun build() = Report( sent, via, positive, total, fileType, type, reportId, expires, facilities, actions )
    }

}

class GetReports :
    BaseHistoryFunction() {

    @FunctionName("getReports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReports",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val authClaims = AuthClaims(
            "myoung",
            Organization(
                "pima-az-phd",
                "pima county",
                Organization.Jurisdiction.COUNTY,
                "AZ",
                "Pima"
            )
        )
        return GetReports(request, authClaims)
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
        val authClaims = AuthClaims(
            "myoung",
            Organization(
                "pima-az-phd",
                "pima county",
                Organization.Jurisdiction.COUNTY,
                "AZ",
                "Pima"
            )
        )
        return GetReportById(request, authClaims, reportId)
    }
}

class GetSummaryPositive {
    @FunctionName("getSummaryPositive")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getSummaryPositive",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/summary/positive"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        return request.createResponseBuilder(HttpStatus.OK)
            .body("[{ \"title\": \"Case\"}]") 
            .header("Content-Type", "application/json")
            .build();
  
    }
}

open class BaseHistoryFunction {

    val DAYS_TO_SHOW = 7L
    val workflowEngine = WorkflowEngine()

    fun GetReports(request: HttpRequestMessage<String?>, authClaims: AuthClaims): HttpResponseMessage {
        val headers = workflowEngine.db.fetchDownloadableTasks(
            OffsetDateTime.now().minusDays(DAYS_TO_SHOW), authClaims.organization.name
        )

        var reports = headers.map {
            Report.Builder()
            .reportId( it.reportId.toString() )
            .sent( it.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) )
            .via( "SFTP")
            .positive( 123 )
            .total( it.itemCount.toLong() )
            .fileType( it.bodyFormat )
            .type( "ELR" )
            .expires( DAYS_TO_SHOW - it.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS), )
            .facilities( emptyArray<Facility>() )
            .actions( emptyArray<Action>() )
            .build()        }


        return request.createResponseBuilder(HttpStatus.OK)
            .body( reports )
            .header("Content-Type", "application/json")
            .build()
    }

    fun GetReportById(
        request: HttpRequestMessage<String?>,
        authClaims: AuthClaims,
        reportId: String
    ): HttpResponseMessage {
        return request.createResponseBuilder(HttpStatus.NOT_IMPLEMENTED).build()
    }

    data class AuthClaims(
        val userName: String,
        val organization: Organization
    )

    /**
     * returns null if not authorized, otherwise returns a set of claims.
     */
    private fun checkAuthenticated(request: HttpRequestMessage<String?>, context: ExecutionContext): AuthClaims? {
        var userName = ""
        var orgName = ""
        val cookies = request.headers["cookie"] ?: ""
        var jwtString = ""
        cookies.replace(" ", "").split(";").forEach {
            val cookie = it.split("=")
            jwtString = if (cookie[0] == "jwt") cookie[1] else ""
            if (jwtString.isNotBlank()) {
                try {
                    val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                        .setIssuer("https://${System.getenv("OKTA_baseUrl")}/oauth2/default")
                        .build()
                    val jwt = jwtVerifier.decode(jwtString)
                    userName = jwt.getClaims().get("sub").toString()
                    val orgs = jwt.getClaims().get("organization")
                    var org = if (orgs !== null) (orgs as List<String>)[0] else ""
                    orgName = if (org.length > 3) org.substring(2) else ""
                } catch (ex: Throwable) {
                    System.out.println(ex)
                }
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
}