package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import java.io.ByteArrayInputStream
import java.util.UUID

@FunctionName("save-test-data")
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
    val reportId = UUID.fromString(reportIdStr)
    context.logger.info("Starting test data extraction")
    val workflowEngine = WorkflowEngine()
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
    val bytes = workflowEngine.blob.downloadBlob(reportFile.bodyUrl)
    val report = workflowEngine.csvSerializer.readInternal(
        schema.name,
        ByteArrayInputStream(bytes),
        emptyList()
    )
    context.logger.info("Extracting the deidentified data")
    val deidentifiedData = report.getDeidentifiedTestData()
    context.logger.info("Extracted ${deidentifiedData.count()} rows to insert")
    context.logger.info("Removing old test data records for report id $reportId")
    workflowEngine.db.transact { txn ->
        workflowEngine.db.deleteTestDataForReportId(reportId, txn)
        context.logger.info("Inserting deidentified data")
        workflowEngine.db.saveTestData(deidentifiedData, txn)
        context.logger.info("Done saving data")
    }
    return HttpUtilities.okResponse(request, "")
}
