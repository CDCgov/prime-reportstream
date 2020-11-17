package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.*
import java.io.ByteArrayInputStream
import java.util.*
import java.util.logging.Level


/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
class ProcessFunction {
    @FunctionName("Process")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "request",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")

        val inputReport = try {
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata.loadAll("$baseDir/metadata")

            val clientName = request.queryParameters["client"] ?: error("Expected a client query parameter")
            val client = Metadata.findClient(clientName) ?: error("Could not find $clientName")
            val contentType = request.headers["content-type"] ?: error("Expected content type header")
            if (!contentType.equals("text/csv", ignoreCase = true)) error("Expected csv content type")
            val schema = Metadata.findSchema(client.schema ?: "") ?: error("Could not find ${client.schema}")

            val body = request.body.orElseThrow()
            ByteArrayInputStream(body.toByteArray()).use {
                CsvConverter.read(schema, it, ClientSource(client))
            }
        } catch (e: Exception) {
            context.logger.log(Level.INFO, "bad request parameters", e)
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(e.message).build()
        }

        return try {
            val outputReports = OrganizationService.filterAndMapByService(inputReport, Metadata.organizationServices)

            val container = BlobStorage.getBlobContainer("processed")
            outputReports.forEach { (report, service) ->
                BlobStorage.uploadBlob(container, report, service)
            }

            request.createResponseBuilder(HttpStatus.OK).build()
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "top-level catch", e)
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message).build()
        }
    }
}
