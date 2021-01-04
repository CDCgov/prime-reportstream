package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ReportId
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

class DownloadFunction {
    val DAYS_TO_SHOW = 7L
    val LOGIN_PAGE = "./assets/csv-download-site/login__inline.html"
    val DOWNLOAD_PAGE = "./assets/csv-download-site/index__inline.html"

    data class TestResult(
        val date: String,
        val expires: Long,
        val total: Any? = null,
        val positive: Any? = null,
        val file: String? = null
    )

    var orgName = ""
    var userName = ""

    @FunctionName("download")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        if (checkAuthenticated(request)) {
            val file: String = request.queryParameters["file"] ?: ""
            if (file.isBlank())
                return responsePage(request)
            else
                return responseFile(request, file)
        } else {
            return redirectToAuthenticate(request)
        }
    }

    private fun redirectToAuthenticate(request: HttpRequestMessage<String?>): HttpResponseMessage {
        val htmlTemplate = Files.readString(Path.of(LOGIN_PAGE))

        val attr = mapOf(
            "OKTA_baseUrl" to System.getenv("OKTA_baseUrl"),
            "OKTA_clientId" to System.getenv("OKTA_clientId"),
            "OKTA_redirect" to request.uri.toString()
        );

        val html = getTemplateFromAttributes(htmlTemplate, attr)
        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun generateTodaysTestResults(headers: List<DatabaseAccess.Header>): List<TestResult> {
        return headers.filter {
            val now = OffsetDateTime.now()
            it.task.createdAt.year == now.year && it.task.createdAt.monthValue == now.monthValue && it.task.createdAt.dayOfMonth == now.dayOfMonth
        }.map {
            TestResult(
                it.task.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                DAYS_TO_SHOW - it.task.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS),
                it.task.itemCount,
                0,
                it.task.reportId.toString()
            )
        }
    }

    private fun generatePreviousTestResults(headers: List<DatabaseAccess.Header>): List<TestResult> {
        return headers.filterNot {
            val now = OffsetDateTime.now()
            it.task.createdAt.year == now.year && it.task.createdAt.monthValue == now.monthValue && it.task.createdAt.dayOfMonth == now.dayOfMonth
        }
            .sortedByDescending {
                it.task.createdAt
            }
            .map {
                TestResult(
                    it.task.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    DAYS_TO_SHOW - it.task.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS),
                    it.task.itemCount,
                    0,
                    it.task.reportId.toString()
                )
            }
    }

    private fun responsePage(request: HttpRequestMessage<String?>): HttpResponseMessage {
        val htmlTemplate = Files.readString(Path.of(DOWNLOAD_PAGE))
        val headers = DatabaseAccess(dataSource = DatabaseAccess.dataSource).fetchHeaders(OffsetDateTime.now().minusDays(DAYS_TO_SHOW), orgName)

        val attr = mapOf(
            "description" to orgName,
            "user" to userName,
            "today" to Calendar.getInstance(),
            "todays" to generateTodaysTestResults(headers),
            "previous" to generatePreviousTestResults(headers),
            "days_to_show" to DAYS_TO_SHOW,
            "OKTA_redirect" to request.uri.toString()
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)

        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun responseFile(request: HttpRequestMessage<String?>, fileName: String): HttpResponseMessage {
        val header = DatabaseAccess(dataSource = DatabaseAccess.dataSource).fetchHeader(ReportId.fromString(fileName))
        var response: HttpResponseMessage
        try{
            val body = WorkflowEngine().readBody(header)
            response = request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=test-results.csv")
                .body(body)
                .build()
        }
        catch( ex: Exception ){
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND).build();
        }

        return response
    }

    private fun getTemplateEngine(): TemplateEngine {
        val templateEngine = TemplateEngine()
        val stringTemplateResolver = StringTemplateResolver()
        templateEngine.setTemplateResolver(stringTemplateResolver)
        return templateEngine
    }

    private fun getTemplateFromAttributes(htmlContent: String, attr: Map<String, Any>): String {
        val templateEngine = getTemplateEngine()
        val context = Context()
        if (!attr.isEmpty())
            attr.forEach { (k, v) -> context.setVariable(k, v) }
        return templateEngine.process(htmlContent, context)
    }

    private fun checkAuthenticated(request: HttpRequestMessage<String?>): Boolean {
        val cookies = request.headers["cookie"] ?: ""

        cookies.replace(" ", "").split(";").forEach {
            System.out.println("'$it'")
            val cookie = it.split("=")
            if (cookie[0] == "user") {
                userName = cookie[1]
            }
            if (cookie[0] == "organization") orgName = cookie[1]
        }

        System.out.println("user = '$userName'")
        System.out.println("org = '$orgName'")

        return userName.isNotBlank() && orgName.isNotBlank()
    }
}