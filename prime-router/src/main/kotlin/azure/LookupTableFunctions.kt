package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.JSONB
import org.jooq.exception.DataAccessException

/**
 * Functions to manage lookup tables.
 */
class LookupTableFunctions(
    private val lookupTableAccess: DatabaseLookupTableAccess = DatabaseLookupTableAccess(),
    private val oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) : Logging {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * Fetch the list of lookup tables.  If the showAll query parameter is set to true then only active tables
     * are returned.
     */
    @FunctionName("getLookupTableList")
    fun getLookupTableList(
        @HttpTrigger(
            name = "getLookupTableList",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            try {
                val showAll = request.queryParameters["showAll"]?.equals("true", true) ?: false
                // Return only what's active if showAll is true
                val list = lookupTableAccess.fetchTableList().filter { (showAll && it.isActive) || !showAll }
                val json = mapper.writeValueAsString(list)
                HttpUtilities.okResponse(request, json)
            } catch (e: DataAccessException) {
                logger.error("Unable to fetch lookup table list", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Fetch the latest version of a lookup table or create a new version of a table.
     */
    @FunctionName("getLookupTable")
    fun getLookupTable(
        @HttpTrigger(
            name = "getLookupTable",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            val latestVersion = lookupTableAccess.fetchLatestVersion(tableName)
            if (latestVersion == null)
                HttpUtilities.notFoundResponse(
                    request,
                    "Table $tableName does not exist."
                )

            when (request.httpMethod) {
                // Get the table data
                HttpMethod.GET ->
                    try {
                        HttpUtilities.okResponse(request, getTableData(tableName, latestVersion!!))
                    } catch (e: DataAccessException) {
                        logger.error("Unable to fetch lookup table $tableName", e)
                        HttpUtilities.internalErrorResponse(request)
                    }

                // Create a new table version
                HttpMethod.POST -> {
                    val inputData = if (!request.body.isNullOrBlank()) Json.parseToJsonElement(request.body!!) else null

                    when {
                        request.body.isNullOrBlank() ->
                            HttpUtilities.badRequestResponse(
                                request,
                                """{"error": "The request is missing a body with the table contents"}"""
                            )

                        inputData == null || inputData !is JsonArray ->
                            HttpUtilities.badRequestResponse(
                                request,
                                """{"error": "The request body must be a JSON Array"}"""
                            )

                        inputData.isEmpty() ->
                            HttpUtilities.badRequestResponse(
                                request,
                                """{"error": "The request body must be a non-empty JSON Array"}"""
                            )

                        inputData.any { it !is JsonObject } ->
                            HttpUtilities.badRequestResponse(
                                request,
                                """{"error": "All rows in the provided array must be a JSON object"}"""
                            )

                        inputData.any { it is JsonObject && it.keys.isEmpty() } ->
                            HttpUtilities.badRequestResponse(
                                request,
                                """{"error": "All rows in the provided array must not be empty JSON objects"}"""
                            )

                        // At this point we have a JSON array we can work with
                        else -> {
                            val colNames = DatabaseLookupTableAccess
                                .extractTableHeadersFromJson(JSONB.jsonb(inputData[0].toString()))

                            // Test all the rows to make sure they all have the same columns.
                            if (inputData.any { row ->
                                colNames.any { colName ->
                                    !(row as JsonObject).containsKey(colName)
                                }
                            }
                            )
                                HttpUtilities.badRequestResponse(
                                    request,
                                    """{"error": "All rows in the provided array must contain the same column names"}"""
                                )

                            // Ok, now we are good to go with the data.
                            val tableRows = inputData.map { row ->
                                JSONB.jsonb(row.toString())
                            }
                            val newVersion = latestVersion!! + 1
                            lookupTableAccess.createTable(tableName, newVersion, tableRows)

                            // Return the table version info
                            val json = mapper
                                .writeValueAsString(lookupTableAccess.fetchVersionInfo(tableName, newVersion))
                            HttpUtilities.okResponse(request, json)
                        }
                    }
                }

                // This will never happen as we declare the methods as part of the function, but klint wants an else.
                else -> throw IllegalStateException("Unsupported HTTP method")
            }
        }
    }

    /**
     * Fetch or activate a table version.
     */
    @FunctionName("getLookupTableWithVersion")
    fun getLookupTableWithVersion(
        @HttpTrigger(
            name = "getLookupTableWithVersion",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}/{tableVersion}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String,
        @BindingName("tableVersion") tableVersion: Int,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            if (!lookupTableAccess.doesTableExist(tableName, tableVersion))
                HttpUtilities.notFoundResponse(
                    request,
                    "Table $tableName with version $tableVersion does not exist."
                )

            when (request.httpMethod) {
                // Get the table data
                HttpMethod.GET ->
                    try {
                        HttpUtilities.okResponse(request, getTableData(tableName, tableVersion))
                    } catch (e: DataAccessException) {
                        logger.error("Unable to fetch lookup table with version $tableVersion", e)
                        HttpUtilities.internalErrorResponse(request)
                    }

                // Activate a table
                HttpMethod.POST -> {
                    try {
                        lookupTableAccess.activateTable(tableName, tableVersion)
                        val json = mapper
                            .writeValueAsString(lookupTableAccess.fetchVersionInfo(tableName, tableVersion))
                        HttpUtilities.okResponse(request, json)
                    } catch (e: DataAccessException) {
                        HttpUtilities.internalErrorResponse(request)
                    }
                }

                // This will never happen as we declare the methods as part of the function, but klint wants an else.
                else -> throw IllegalStateException("Unsupported HTTP method")
            }
        }
    }

    /**
     * Gets the rows for a [tableName] and [tableVersion] and converts it to a JSON string.
     * @return a JSON string with the table rows
     */
    private fun getTableData(tableName: String, tableVersion: Int): String {
        val rows = lookupTableAccess.fetchTable(tableName, tableVersion)
        val jsonRows = rows.map { row ->
            Json.parseToJsonElement(row.data.data())
        }
        return JsonArray(jsonRows).toString()
    }
}