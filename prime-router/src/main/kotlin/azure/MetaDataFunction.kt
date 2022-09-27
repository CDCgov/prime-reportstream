package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.apache.logging.log4j.kotlin.Logging

/**
 * The data class that wraps the LIVD data
 */
data class LivdData(
    val manufacturer: String,
    val model: String,
    val vendorAnalyteName: String,
    val vendorSpecimenDescription: List<String>,
    val vendorResultDescription: List<String>,
    val testPerformedLoincCode: String,
    val testPerformedLoincLongName: String,
    val testOrderedLoincCode: String,
    val testOrderedLoincLongName: String,
    val vendorComment: String,
    val vendorAnalyteCode: String,
    val vendorReferenceId: String,
    val testKitNameId: String,
    val testKitNameIdType: String,
    val equipmentUid: String,
    val equipmentUidType: String,
    val component: String,
    val property: String,
    val time: String,
    val system: String,
    val scale: String,
    val method: String,
    val publicationVersionId: String,
    val loincVersionId: String,
)

/**
 * some common metadata querying functions
 */
class MetaDataFunction : Logging {
    private val workflowEngine = WorkflowEngine()

    /**
     * An endpoint for getting LIVD values
     */
    @FunctionName("getLivdData")
    @StorageAccount("AzureWebJobsStorage")
    fun livdReport(
        @HttpTrigger(
            name = "getLivdData",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "metadata/livd"
        ) request: HttpRequestMessage<String?>,
        @Suppress("UNUSED_PARAMETER")
        context: ExecutionContext,
    ): HttpResponseMessage {
        val filters = request.queryParameters
        val rows = getLivdTable(filters) ?: return HttpUtilities.internalErrorResponse(request)
        logger.info("Found ${rows.count()} rows for LIVD table.")
        val responseBody = JacksonMapperUtilities.defaultMapper.writeValueAsString(rows)
        // create response body
        return HttpUtilities.okResponse(request, responseBody)
    }

    /**
     * gets the LIVD table filtered by the values passed in
     */
    private fun getLivdTable(filters: Map<String, String>): List<LivdData>? {
        logger.info("Getting the LIVD table for the metadata api")
        val livdTable = workflowEngine
            .metadata
            .findLookupTable("LIVD-SARS-CoV-2") ?: return null
        logger.info("Pulling rows")
        val tableFilter = livdTable.FilterBuilder()
        filters.forEach { (t, u) -> tableFilter.equalsIgnoreCase(t, u) }
        return tableFilter.filter().dataRows.map {
            val specimenDescriptions = it[3].split("\n")
            val resultDescriptions = it[4].split("\n")
            LivdData(
                it[0], it[1], it[2], specimenDescriptions, resultDescriptions,
                it[5], it[6], it[7], it[8], it[9], it[10], it[11], it[12], it[13], it[14],
                it[15], it[16], it[17], it[18], it[19], it[20], it[21], it[22], it[23]
            )
        }.sortedBy {
            "${it.manufacturer}${it.model}".uppercase()
        }
    }
}