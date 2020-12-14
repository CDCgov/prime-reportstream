package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.OrganizationClient
import gov.cdc.prime.router.Report
import org.apache.commons.text.TextStringBuilder
import java.io.ByteArrayInputStream
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ValidateReportFunction {
    private val clientName = "client"
    private val csvMimeType = "text/csv"

    /**
     * Run ./test-ingest.sh to get an example curl call that calls this function.
     */
    @FunctionName("reports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        // Validate the incoming CSV by converting it to a report.
        val workflowEngine = WorkflowEngine()
        val report = try {
            val (client, content) = getAndValidate(request)
            createReport(workflowEngine, client, content)
        } catch (e: Exception) {
            val msgs = TextStringBuilder()
            e.suppressedExceptions.forEach { msgs.appendln(it.message) }
            msgs.appendln(e.message)
            context.logger.log(Level.INFO, "Bad request.  $msgs", e)
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(msgs.toString())
                .build()
        }
        context.logger.info("Successfully read ${report.id}. Preparing to queue it for processing.")

        // Queue the report for further processing.
        return try {
            workflowEngine.dispatchReport(ReportEvent(Event.Action.TRANSLATE, report.id), report)
            request
                .createResponseBuilder(HttpStatus.CREATED)
                .body(createResponseBody(report))
                .build()
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to ingest into database and queue for further work.")
                .build()
        }
    }

    private fun getAndValidate(request: HttpRequestMessage<String?>): Pair<OrganizationClient, String> {
        val errors: MutableList<String> = mutableListOf()

        val name = request.headers.getOrDefault(clientName, "")
        var client: OrganizationClient? = null
        if (name.isNotBlank()) {
            try {
                client = WorkflowEngine.metadata.findClient(name)
            } catch (e: Exception) {
                val betterException = Exception("Error: unknown client '$name'")
                betterException.addSuppressed(e)
                throw betterException
            }
        } else {
            errors.add("Error: missing 'client' header")
        }

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.toLowerCase(), "")
        if (contentType.isBlank()) {
            errors.add("Error: expecting a content-type header")
        } else if (client != null && client.format.mimeType != contentType) {
            errors.add("Error: expecting a '${client.format.mimeType}' content-type header")
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            errors.add("Error: expecting a post message with content")
        }

        if (errors.isNotEmpty()) {
            val e = Exception()
            errors.forEach { e.addSuppressed(Exception(it)) }
            throw e
        }

        return Pair(client!!, content)
    }

    private fun createReport(engine: WorkflowEngine, client: OrganizationClient, content: String): Report {
        val schema = engine.metadata.findSchema(client.schema) ?: error("missing schema for $clientName")
        return when (client.format) {
            OrganizationClient.Format.CSV -> {
                engine.csvConverter.read(
                    schema,
                    ByteArrayInputStream(content.toByteArray()),
                    ClientSource(organization = client.organization.name, client = client.name)
                )
            }
        }
    }

    private fun createResponseBody(report: Report): String {
        return """
            {
              "id": "${report.id}"
            }
        """.trimIndent()
    }
}