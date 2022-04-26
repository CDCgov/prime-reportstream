package gov.cdc.prime.router.azure

import com.azure.storage.blob.models.BlobStorageException
import com.fasterxml.jackson.core.JsonFactory
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException
import java.time.OffsetDateTime
import java.util.UUID

class CovidResultMetaDataFunction : Logging {
    private val workflowEngine = WorkflowEngine()

    @FunctionName("save-covid-result-metadata")
    fun run(
        @HttpTrigger(
            name = "saveTestData",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        context.logger.info("Entering test data extraction api")
        val reportIdStr = request.queryParameters["reportId"]
            ?: return HttpUtilities.badRequestResponse(request, "Missing reportId\n")
        return if (reportIdStr.lowercase() == "all") {
            context.logger.info("Starting test data extraction for all reports")
            saveCovidResultMetaDataForAllReports(context, request)
        } else {
            val reportId = UUID.fromString(reportIdStr)
            context.logger.info("Starting test data extraction for $reportId")
            saveCovidResultMetaDataForSingleReport(reportId, context, request)
        }
    }

    private fun saveCovidResultMetaDataForAllReports(
        context: ExecutionContext,
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val results = mutableMapOf<UUID, String>()
        val createdDateTime = OffsetDateTime.now().minusDays(30)
        workflowEngine.db.transact { txn ->
            workflowEngine.db.fetchAllInternalReports(createdDateTime, txn).forEach { reportFile ->
                val reportId = reportFile.reportId
                val schema = workflowEngine.metadata.findSchema(reportFile.schemaName)
                if (schema == null) {
                    results[reportId] = "Schema name ${reportFile.schemaName} not found in metadata."
                    return@forEach
                }
                try {
                    context.logger.info("Trying to download report $reportId")
                    val report = getReport(reportFile, schema)
                    context.logger.info("Extracting the deidentified data for $reportId")
                    val deidentifiedData = report.getDeidentifiedResultMetaData()
                    context.logger.info("Extracted ${deidentifiedData.count()} rows to insert for $reportId")
                    context.logger.info("Removing old test data records for report id $reportId")
                    workflowEngine.db.deleteTestDataForReportId(reportId, txn)
                    context.logger.info("Inserting deidentified data")
                    DatabaseAccess.saveTestData(deidentifiedData, txn)
                    context.logger.info("Done saving data for $reportId")
                    // add to collection for the response body
                    results[reportId] = "Saved ${deidentifiedData.count()} records"
                } catch (ex: Exception) {
                    results[reportFile.reportId] = "Exception processing report: ${ex.localizedMessage}"
                    context.logger.severe(ex.message)
                    context.logger.severe(ex.stackTraceToString())
                }
            }
        }
        return HttpUtilities.okResponse(request, writeResponseBody(results))
    }

    private fun saveCovidResultMetaDataForSingleReport(
        reportId: UUID,
        context: ExecutionContext,
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return try {
            val reportFile = workflowEngine.db.fetchReportFile(reportId)
            // check format
            if (reportFile.bodyFormat != "INTERNAL") {
                val msg = "Report ID is not for an internal format. Will not continue."
                context.logger.warning(msg)
                return HttpUtilities.badRequestResponse(request, msg)
            }
            // check sender
            if (reportFile.sendingOrg.isNullOrEmpty()) {
                val msg = "Will not extract data from a child report. Sending org should not be null, but it is."
                context.logger.warning(msg)
                return HttpUtilities.badRequestResponse(request, msg)
            }
            val schema = workflowEngine.metadata.findSchema(reportFile.schemaName)
                ?: return HttpUtilities.badRequestResponse(
                    request,
                    "Cannot find schema for ${reportFile.schemaName} in metadata."
                )
            context.logger.info("Trying to download the report")
            val report = getReport(reportFile, schema)
            context.logger.info("Extracting the deidentified data")
            val deidentifiedData = report.getDeidentifiedResultMetaData()
            context.logger.info("Extracted ${deidentifiedData.count()} rows to insert")
            context.logger.info("Removing old test data records for report id $reportId")
            workflowEngine.db.transact { txn ->
                workflowEngine.db.deleteTestDataForReportId(reportId, txn)
                context.logger.info("Inserting deidentified data")
                DatabaseAccess.saveTestData(deidentifiedData, txn)
                context.logger.info("Done saving data")
            }
            HttpUtilities.okResponse(request, writeResponseBody(reportId, deidentifiedData.count()))
        } catch (iex: IllegalStateException) {
            val msg = "Report does not exist for id $reportId"
            context.logger.severe(msg)
            context.logger.severe(iex.localizedMessage)
            context.logger.severe(iex.stackTraceToString())
            HttpUtilities.badRequestResponse(request, msg)
        } catch (bex: BlobStorageException) {
            val msg = "Unable to download report blob for $reportId"
            context.logger.severe(msg)
            context.logger.severe(bex.localizedMessage)
            context.logger.severe(bex.stackTraceToString())
            HttpUtilities.badRequestResponse(request, msg)
        }
    }

    private fun getReport(
        reportFile: ReportFile,
        schema: Schema
    ): Report {
        val bytes = BlobAccess.downloadBlob(reportFile.bodyUrl)
        return workflowEngine.csvSerializer.readInternal(
            schema.name,
            ByteArrayInputStream(bytes),
            emptyList(),
            blobReportId = reportFile.reportId
        )
    }

    companion object {
        private fun writeResponseBody(reportId: UUID, rowsInserted: Int): String {
            val jsonFactory = JsonFactory()
            val stream = ByteArrayOutputStream()
            jsonFactory.createGenerator(stream).use {
                it.useDefaultPrettyPrinter()
                it.writeStartObject()
                it.writeStringField("reportId", reportId.toString())
                it.writeNumberField("rowsInserted", rowsInserted)
                it.writeEndObject()
            }
            return stream.toString()
        }

        private fun writeResponseBody(reportIdCounts: Map<UUID, String>): String {
            val jsonFactory = JsonFactory()
            val stream = ByteArrayOutputStream()
            jsonFactory.createGenerator(stream).use {
                it.useDefaultPrettyPrinter()
                it.writeStartObject()
                it.writeNumberField("countOfReports", reportIdCounts.keys.count())
                it.writeArrayFieldStart("results")
                reportIdCounts.keys.forEach { key ->
                    it.writeStartObject()
                    it.writeStringField("reportId", key.toString())
                    it.writeStringField("result", reportIdCounts[key])
                    it.writeEndObject()
                }
                it.writeEndArray()
                it.writeEndObject()
            }
            return stream.toString()
        }
    }
}