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
    val loincVersionId: String
) {
    companion object {
        /**
         * Takes the map from below and constructs a new instance of the LivdData object. Hides some complexity
         */
        fun build(dataRow: Map<String, String>): LivdData {
            // the specimen and result descriptions are lists inside the CSV document. not great, but we'll convert
            // them into lists for the purposes of representing the data correctly in the JSON
            val specimenDescriptions = dataRow["vendor specimen description"]?.split("\n") ?: listOf("")
            val resultDescriptions = dataRow["vendor result description"]?.split("\n") ?: listOf("")
            // create the object
            return LivdData(
                dataRow["manufacturer"] ?: "",
                dataRow["model"] ?: "",
                dataRow["vendor analyte name"] ?: "",
                specimenDescriptions,
                resultDescriptions,
                dataRow["test performed loinc code"] ?: "",
                dataRow["test performed loinc long name"] ?: "",
                dataRow["test ordered loinc code"] ?: "",
                dataRow["test ordered loinc long name"] ?: "",
                dataRow["vendor comment"] ?: "",
                dataRow["vendor analyte code"] ?: "",
                dataRow["vendor reference id"] ?: "",
                dataRow["testkit name id"] ?: "",
                dataRow["testkit name id type"] ?: "",
                dataRow["equipment uid"] ?: "",
                dataRow["equipment uid type"] ?: "",
                dataRow["component"] ?: "",
                dataRow["property"] ?: "",
                dataRow["time"] ?: "",
                dataRow["system"] ?: "",
                dataRow["scale"] ?: "",
                dataRow["method"] ?: "",
                dataRow["publication version id"] ?: "",
                dataRow["loinc version id"] ?: ""
            )
        }
    }
}

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
        context: ExecutionContext
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
        // given the filtered table, this reads the list of maps and creates a new object out of the mapped
        // column values
        return tableFilter.filter().dataRowsMap.map {
            LivdData.build(it)
        }.sortedBy {
            "${it.manufacturer}${it.model}".uppercase()
        }.toList()
    }
}