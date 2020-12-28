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

    @FunctionName("download")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val file: String = request.queryParameters["file"] ?: ""
        if (file.isBlank())
            return responsePage(request)
        else
            return responseFile(request, file)
    }

    private fun redirectToAuthenticate(request: HttpRequestMessage<String?>): HttpResponseMessage {
        val htmlTemplate = Files.readString(Path.of(LOGIN_PAGE))
        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(htmlTemplate)
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

    private fun getSessionVariables(key: String): String {
        if (key == "user") return "qtv1@cdc.gov"
        else if (key == "receiver") return "az-phd.elr"
        else return ""
    }

    private fun responsePage(request: HttpRequestMessage<String?>): HttpResponseMessage {
        val htmlTemplate = Files.readString(Path.of(DOWNLOAD_PAGE))
        val headers = DatabaseAccess().fetchHeaders(OffsetDateTime.now().minusDays(DAYS_TO_SHOW), getSessionVariables("receiver"))

        val attr = mapOf(
            "description" to "Pima county, AZ",
            "user" to getSessionVariables("user"),
            "today" to Calendar.getInstance(),
            "todays" to generateTodaysTestResults(headers),
            "previous" to generatePreviousTestResults(headers),
            "days_to_show" to DAYS_TO_SHOW
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)

        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun responseFile(request: HttpRequestMessage<String?>, fileName: String): HttpResponseMessage {
        val header = DatabaseAccess().fetchHeader(ReportId.fromString(fileName))

        val body = WorkflowEngine().readBody(header)
        var response = request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=test-results.csv")
            .body(body)
            .build()

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
        val jwt: String = request.queryParameters["code"] ?: ""
        return jwt.isNotBlank()
    }
}