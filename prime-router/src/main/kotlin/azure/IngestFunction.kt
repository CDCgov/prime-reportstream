package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationClient
import gov.cdc.prime.router.Report
import org.apache.commons.text.TextStringBuilder
import java.io.ByteArrayInputStream
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger.
 */
class IngestFunction {
    private val clientName = "client"
    private val csvMimeType = "text/csv"

    /**
     * This function listens at endpoint "/api/report".
     * Run ./test-ingest.sh to get an example curl call that runs this function.
     * That curl returns something like the following upon success:
     *    {"filename":"lab1-test_results-17-42-31.csv","topic":"covid-19","schema":"pdi-covid-19.schema","action":"","blobURL":"http://azurite:10000/devstoreaccount1/ingested/lab1-test_results-17-42-31-pdi-covid-19.schema-3ddef736-55e1-4a45-ac41-f74086aaa654.csv"}
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
        // First load metadata
        try {
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata.loadAll("$baseDir/metadata")
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to load metadata")
                .build()
        }

        // Validate the incoming CSV by converting it to a report.
        val report = try {
            val (client, content) = getAndValidate(request)
            createReport(client, content)
        } catch (e: Exception) {
            val msgs = TextStringBuilder()
            e.suppressedExceptions.forEach { msgs.appendln(it.message) }

            context.logger.log(Level.INFO, "Bad request from e.message", e)
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(msgs.toString())
                .build()
        }
        context.logger.info("Successfully read ${report.id}. Preparing to queue it for processing.")

        // Queue the report for further processing.
        return try {
            ReportQueue.sendReport(ReportQueue.Name.INGESTED, report)
            request
                .createResponseBuilder(HttpStatus.CREATED)
                .body(createResponseBody(report))
                .build()
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send to queue for further work.")
                .build()
        }
    }

    private fun getAndValidate(request: HttpRequestMessage<String?>): Pair<OrganizationClient, String> {
        val errors: MutableList<String> = mutableListOf()

        val name = request.headers.getOrDefault(clientName, "")
        var client: OrganizationClient? = null
        if (!clientName.isBlank()) {
            client = Metadata.findClient(name)
            if (client == null)
                errors.add("Error: did not recognize $name as a valid client")
        } else {
            errors.add("Error: missing 'client' header")
        }

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.toLowerCase(), "")
        if (contentType.isBlank()) {
            errors.add("Error: expecting a content-type header")
        } else if (client != null && client.format.mimeType != contentType) {
            errors.add("Error: expecting a '${client.format.mimeType} content-type header")
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

    fun createReport(client: OrganizationClient, content: String): Report {
        val schema = Metadata.findSchema(client.schema) ?: error("missing schema for $clientName")
        return when (client.format) {
            OrganizationClient.Format.CSV -> {
                CsvConverter.read(
                    schema,
                    ByteArrayInputStream(content.toByteArray()),
                    ClientSource(organization = client.organization.name, client = client.name)
                )
            }
            else -> error("Content type is not supported")
        }
    }

    fun createResponseBody(report: Report): String {
        return """
            {
              "id": "${report.id}"
            }
            """.trimIndent()
    }


}