package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import com.github.doyaaaaaken.kotlincsv.util.MalformedCSVException
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.cli.LookupTableCompareMappingCommand
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging

class SenderFunction(
    private val workflowEngine: WorkflowEngine = WorkflowEngine(),
    private val actionHistory: ActionHistory = ActionHistory(TaskAction.receive),
) : RequestFunction(workflowEngine),
    Logging {

    /**
     * POST a CSV with test codes and conditions to compare with existing
     * code to condition observation mapping table
     *
     * @return original request body data with mapping results in JSON format
     */
    @FunctionName("conditionCodeComparisonPostRequest")
    fun conditionCodeComparisonPostRequest(
        @HttpTrigger(
            name = "conditionCodeComparisonPostRequest",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "sender/conditionCode/comparison"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val senderName = extractClient(request)
        if (senderName.isBlank()) {
            return HttpUtilities.bad(request, "Expected a '$CLIENT_PARAMETER' query parameter")
        }

        actionHistory.trackActionParams(request)
        try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            val sender = workflowEngine.settings.findSender(senderName)
                ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown client")

            if (!claims.authorizedForSendOrReceive(sender, request)) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            // Read request body CSV
            val bodyCsvText = request.body ?: ""

            // Check CSV headers in request body
            val csvHeader = bodyCsvText.lineSequence().firstOrNull()
            // 400 if CSV is empty
            if (csvHeader.isNullOrBlank()) {
                return HttpUtilities.bad(request, "CSV file is empty")
            }
            val requiredHeaders = setOf("test code", "test description", "coding system")
            val headersToCheck = csvHeader.split(",").toSet()
            val missingHeaders = requiredHeaders.minus(headersToCheck)

            // 400 with list of missing headers if any
            if (missingHeaders.isNotEmpty()) {
                return HttpUtilities.bad(
                    request,
                    "CSV file is missing the following header column(s): '$missingHeaders'"
                )
            }

            val bodyCsv = csvReader().readAllWithHeader(bodyCsvText)
            // 400 if CSV contains no data rows
            if (bodyCsv.isEmpty()) {
                return HttpUtilities.bad(request, "CSV file contains no rows of data")
            } else {
                // Find rows that have columns with empty values
                val rowsWithEmptyValues = bodyCsv.filter { row ->
                    row.filter { (_, v) -> v.isEmpty() }.isNotEmpty()
                }
                // 400 if there are rows with columns with empty values
                if (rowsWithEmptyValues.isNotEmpty()) {
                    return HttpUtilities.bad(request, "CSV file contains rows with empty values")
                }
            }

            // Get observation mapping table
            val tableMapper = LookupTableConditionMapper(workflowEngine.metadata)
            val observationMappingTable = tableMapper.mappingTable.caseSensitiveDataRowsMap
            val tableTestCodeMap = observationMappingTable.associateBy { it[ObservationMappingConstants.TEST_CODE_KEY] }

            // Compare request CSV with table using CLI wrapper
            val conditionCodeComparison = LookupTableCompareMappingCommand.compareMappings(
                compendium = bodyCsv, tableTestCodeMap = tableTestCodeMap
            )

            // Create output JSON with mapping comparison result
            // For this output specifically, rename the keys for the JSON output
            val comparisonsRemappedKeys = conditionCodeComparison.map { entry ->
                entry.map { (key, value) ->
                    when (key) {
                        "test code" -> "testCode" to value
                        "test description" -> "testDescription" to value
                        "coding system" -> "codingSystem" to value
                        "mapped?" -> "mapped" to value
                        else -> key to value
                    }
                }.toMap()
            }
            val conditionCodeComparisonJson = ObjectMapper().writeValueAsString(comparisonsRemappedKeys)

            return HttpUtilities.okResponse(request, conditionCodeComparisonJson)
        } catch (ex: Exception) {
            return when (ex) {
                is CSVFieldNumDifferentException -> {
                    logger.error(ex)
                    HttpUtilities.bad(request, "CSV file contains rows with missing data")
                }
                is CSVParseFormatException, is MalformedCSVException -> {
                    logger.error(ex)
                    HttpUtilities.bad(request, "Error parsing CSV")
                }
                else -> {
                    logger.error(ex)
                    HttpUtilities.internalErrorResponse(request)
                }
            }
        }
    }
}