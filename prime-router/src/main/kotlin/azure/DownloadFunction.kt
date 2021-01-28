package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.okta.jwt.JwtVerifiers
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.UUID
import java.util.logging.Level

class DownloadFunction {
    val DAYS_TO_SHOW = 7L
    val LOGIN_PAGE = "./assets/csv-download-site/login__inline.html"
    val DOWNLOAD_PAGE = "./assets/csv-download-site/index__inline.html"
    val FILENOTFOUND_PAGE = "./assets/csv-download-site/nosuchfile__inline.html"

    data class TestResult(
        val date: String?,
        val receiver: String?,
        val expires: Long?,
        val total: Any? = null,
        val positive: Any? = null,
        val file: String? = null,
        val format: String? = null,
        val fileName: String? = null,
    )

    var organization: Organization? = null // todo hack.   Move into the run method.

    @FunctionName("download")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var authenticated = checkAuthenticated(request)
        if (authenticated.first) {
            organization = WorkflowEngine().metadata.findOrganization(authenticated.third.replace('_', '-'))
            val file: String = request.queryParameters["file"] ?: ""
            if (file.isBlank())
                return responsePage(request, authenticated.second, authenticated.third)
            else
                return responseFile(request, file, authenticated.second, authenticated.third, context)
        } else {
            return serveAuthenticatePage(request)
        }
    }

    private fun serveAuthenticatePage(request: HttpRequestMessage<String?>): HttpResponseMessage {
        val htmlTemplate = Files.readString(Path.of(LOGIN_PAGE))

        val attr = mapOf(
            "OKTA_baseUrl" to System.getenv("OKTA_baseUrl"),
            "OKTA_clientId" to System.getenv("OKTA_clientId"),
            "OKTA_redirect" to System.getenv("OKTA_redirect")
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)
        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun generateTestResults(headers: List<DatabaseAccess.Header>, userName: String, orgName: String): List<TestResult> {
        return headers.sortedByDescending {
            it.task.createdAt
        }.map {
            val org = WorkflowEngine().metadata.findOrganization(orgName.replace('_', '-'))
            val svc = WorkflowEngine().metadata.findService(it.task.receiverName)
            val orgDesc = if (org !== null) org.description else "Unknown"
            val receiver = if (svc !== null && svc.description.isNotBlank()) svc.description else orgDesc
            System.out.println("org = $org")
            TestResult(
                it.task.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                receiver,
                DAYS_TO_SHOW - it.task.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS),
                it.task.itemCount,
                0,
                it.task.reportId.toString(),
                it.task.bodyFormat
            )
        }
    }

    private fun generateTodaysTestResults(headers: List<DatabaseAccess.Header>, userName: String, orgName: String): List<TestResult> {
        var filtered = headers.filter { filter(it) }
        return generateTestResults(filtered, userName, orgName)
    }

    private fun filter(it: DatabaseAccess.Header): Boolean {
        val now = OffsetDateTime.now()
        return it.task.createdAt.year == now.year && it.task.createdAt.monthValue == now.monthValue && it.task.createdAt.dayOfMonth == now.dayOfMonth
    }

    private fun generatePreviousTestResults(headers: List<DatabaseAccess.Header>, userName: String, orgName: String): List<TestResult> {
        var filtered = headers.filterNot { filter(it) }
        return generateTestResults(filtered, userName, orgName)
    }

    private fun responsePage(request: HttpRequestMessage<String?>, userName: String, orgName: String): HttpResponseMessage {
        val htmlTemplate: String = Files.readString(Path.of(DOWNLOAD_PAGE))
        val headers = DatabaseAccess(dataSource = DatabaseAccess.dataSource).fetchHeaders(OffsetDateTime.now().minusDays(DAYS_TO_SHOW), orgName)
        val attr = mapOf(
            "description" to (organization?.description ?: ""),
            "user" to userName,
            "today" to Calendar.getInstance(),
            "todays" to generateTodaysTestResults(headers, userName, orgName),
            "previous" to generatePreviousTestResults(headers, userName, orgName),
            "days_to_show" to DAYS_TO_SHOW,
            "OKTA_redirect" to System.getenv("OKTA_redirect"),
            "showTables" to true
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)

        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun responseFile(
        request: HttpRequestMessage<String?>,
        requestedFile: String,
        userName: String,
        orgName: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        val reportId = ReportId.fromString(requestedFile)
        val header = DatabaseAccess(dataSource = DatabaseAccess.dataSource).fetchHeader(reportId, orgName)
        var response: HttpResponseMessage

        try {
            val body = WorkflowEngine().readBody(header)
            if (body.size <= 0)
                response = responsePage(request, userName, orgName)
            else {
                // Give the external report a new UUID, so we can track its history distinct from the
                // internal blob.   This is going to be very confusing.
                val externalReportId = UUID.randomUUID()
                val filename = Report.formExternalFilename(header.task.receiverName, reportId, header.task.bodyFormat)
                response = request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=$filename")
                    .body(body)
                    .build()
                val actionHistory = ActionHistory(TaskAction.download, context)
                actionHistory.trackActionRequestResponse(request, response)
                actionHistory.trackDownloadedReport(header, filename, reportId, externalReportId, userName, organization)
                WorkflowEngine().recordAction(actionHistory)
                return response
            }
        } catch (ex: Exception) {
            context.logger.log(Level.WARNING, "Exception during download of $requestedFile", ex)
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND).build()
        }
        return response
    }

    private fun getTemplateEngine(): TemplateEngine {
        val templateEngine = TemplateEngine()
        val stringTemplateResolver = StringTemplateResolver()
        templateEngine.setTemplateResolver(stringTemplateResolver)
        return templateEngine
    }

    private fun getTemplateFromAttributes(htmlContent: String, attr: Map<String, Any?>): String {
        val templateEngine = getTemplateEngine()
        val context = Context()
        if (!attr.isEmpty())
            attr.forEach { (k, v) -> context.setVariable(k, v) }
        return templateEngine.process(htmlContent, context)
    }

    private fun checkAuthenticated(request: HttpRequestMessage<String?>): Triple<Boolean, String, String> {
        var userName = ""
        var orgName = ""

        val cookies = request.headers["cookie"] ?: ""

        System.out.println(cookies)

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
        return Triple(userName.isNotBlank() && orgName.isNotBlank(), userName, orgName)
    }
}