package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
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
            val bodyCsv = csvReader().readAllWithHeader(bodyCsvText)

            // Get observation mapping table
            val tableMapper = LookupTableConditionMapper(workflowEngine.metadata)
            val observationMappingTable = tableMapper.mappingTable.caseSensitiveDataRowsMap
            val tableTestCodeMap = observationMappingTable.associateBy { it[ObservationMappingConstants.TEST_CODE_KEY] }

            // Compare request CSV with table using CLI wrapper
            val conditionCodeComparison = LookupTableCompareMappingCommand.compareMappings(
                compendium = bodyCsv, tableTestCodeMap = tableTestCodeMap
            )

            // Create output JSON with mapping comparison result
            val conditionCodeComparisonJson = ObjectMapper().writeValueAsString(conditionCodeComparison)

            return HttpUtilities.okResponse(request, conditionCodeComparisonJson)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            return HttpUtilities.internalErrorResponse(request)
        }
    }
}