package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.Preconditions
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.JSONB
import org.jooq.exception.DataAccessException

/**
 * Functions to manage lookup tables.
 */
class LookupTableFunctions(
    private val lookupTableAccess: DatabaseLookupTableAccess = DatabaseLookupTableAccess(),
    private var oktaAuthentication: OktaAuthentication? = null
) : Logging {

    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper: ObjectMapper = jacksonObjectMapper()

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * Get the Okta authenticator based on the [level].  If it was specified via the constructor then use that
     * as it is useful for unit tests.
     * @return the Okta authenticator
     */
    private fun getOktaAuthenticator(level: PrincipalLevel? = null): OktaAuthentication {
        return oktaAuthentication ?: if (level != null) OktaAuthentication(level) else OktaAuthentication()
    }

    /**
     * Fetch the list of lookup tables.  If the showInactive query parameter is set to true then show
     * both active and inactive tables
     */
    @FunctionName("getLookupTableList")
    fun getLookupTableList(
        @HttpTrigger(
            name = "getLookupTableList",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/list"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return getOktaAuthenticator().checkAccess(request) {
            try {
                val showInactive = request.queryParameters[showInactiveParamName]
                    ?.equals("true", true) ?: false
                // Return everything if showInactive is true
                val list = lookupTableAccess.fetchTableList(showInactive)
                val json = mapper.writeValueAsString(list)
                HttpUtilities.okResponse(request, json)
            } catch (e: DataAccessException) {
                logger.error("Unable to fetch lookup table list", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Fetch the data for a specific table version.
     */
    @FunctionName("getLookupTableData")
    fun getLookupTableData(
        @HttpTrigger(
            name = "getLookupTableData",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}/{tableVersion}/content"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String,
        @BindingName("tableVersion") tableVersion: Int
    ): HttpResponseMessage {
        return getOktaAuthenticator().checkAccess(request) {
            try {
                if (!lookupTableAccess.doesTableExist(tableName, tableVersion))
                    HttpUtilities.notFoundResponse(
                        request,
                        "Table $tableName with version $tableVersion does not exist."
                    )
                else
                    HttpUtilities.okResponse(request, convertTableDataToJsonString(tableName, tableVersion))
            } catch (e: DataAccessException) {
                logger.error("Unable to fetch lookup table with version $tableVersion", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Fetch the version information for a specific table version.
     */
    @FunctionName("getLookupTableInfo")
    fun getLookupTableInfo(
        @HttpTrigger(
            name = "getLookupTableInfo",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}/{tableVersion}/info"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String,
        @BindingName("tableVersion") tableVersion: Int
    ): HttpResponseMessage {
        return getOktaAuthenticator().checkAccess(request) {
            try {
                val tableInfo = lookupTableAccess.fetchVersionInfo(tableName, tableVersion)
                if (tableInfo == null)
                    HttpUtilities.notFoundResponse(
                        request,
                        "Table $tableName with version $tableVersion does not exist."
                    )
                else
                    HttpUtilities.okResponse(request, mapper.writeValueAsString(tableInfo))
            } catch (e: DataAccessException) {
                logger.error("Unable to fetch lookup table with version $tableVersion", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Create a lookup table. The first version of a table is automatically activated.
     */
    @FunctionName("createLookupTable")
    fun createLookupTable(
        @HttpTrigger(
            name = "createLookupTable",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String
    ): HttpResponseMessage {
        return getOktaAuthenticator(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) {
            try {
                val inputData = if (!request.body.isNullOrBlank()) Json.parseToJsonElement(request.body!!)
                else null
                val errorResponse = checkCreateRequest(request, inputData)
                if (errorResponse != null)
                    errorResponse
                else {
                    Preconditions.checkNotNull(inputData) // A sanity check
                    val checkedData = inputData!! as JsonArray
                    val colNames = DatabaseLookupTableAccess
                        .extractTableHeadersFromJson(JSONB.jsonb(checkedData[0].toString()))

                    // Test all the rows to make sure they all have the same columns.
                    if (checkedData.any { row ->
                        colNames.any { colName ->
                            !(row as JsonObject).containsKey(colName)
                        }
                    }
                    )
                        HttpUtilities.badRequestResponse(
                            request,
                            createErrorMsg("All rows in the provided array must contain the same column names")
                        )
                    else {
                        // Ok, now we are good to go with the data.
                        val tableRows = checkedData.map { row ->
                            JSONB.jsonb(row.toString())
                        }
                        val latestVersion = lookupTableAccess.fetchLatestVersion(tableName) ?: 0
                        val newVersion = latestVersion + 1
                        lookupTableAccess.createTable(tableName, newVersion, tableRows)

                        // Return the table version info
                        val json = mapper
                            .writeValueAsString(lookupTableAccess.fetchVersionInfo(tableName, newVersion))
                        HttpUtilities.okResponse(request, json)
                    }
                }
            } catch (e: DataAccessException) {
                logger.error("Unable to create lookup table $tableName", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Activate a lookup table.
     */
    @FunctionName("activateLookupTable")
    fun activateLookupTable(
        @HttpTrigger(
            name = "activateLookupTable",
            methods = [HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}/{tableVersion}/activate"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String,
        @BindingName("tableVersion") tableVersion: Int
    ): HttpResponseMessage {
        return getOktaAuthenticator(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) {
            try {
                if (!lookupTableAccess.doesTableExist(tableName, tableVersion))
                    HttpUtilities.notFoundResponse(
                        request,
                        "Table $tableName with version $tableVersion does not exist."
                    )
                else {
                    lookupTableAccess.activateTable(tableName, tableVersion)
                    val json = mapper
                        .writeValueAsString(lookupTableAccess.fetchVersionInfo(tableName, tableVersion))
                    HttpUtilities.okResponse(request, json)
                }
            } catch (e: DataAccessException) {
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Gets the rows for a [tableName] and [tableVersion] and converts it to a JSON string.
     * @return a JSON string with the table rows
     */
    internal fun convertTableDataToJsonString(tableName: String, tableVersion: Int): String {
        val rows = lookupTableAccess.fetchTable(tableName, tableVersion)
        val jsonRows = rows.map { row ->
            Json.parseToJsonElement(row.data.data())
        }
        return JsonArray(jsonRows).toString()
    }

    companion object {
        /**
         * Name of the query parameter to show inactive tables.
         */
        const val showInactiveParamName = "showInactive"

        /**
         * Create the JSON representation of an error [message].
         * @return the JSON with the error message
         */
        internal fun createErrorMsg(message: String): String {
            return JsonObject(mapOf("error" to JsonPrimitive(message))).toString()
        }

        /**
         * Check the create post [request] and [bodyAsJson] for errors.
         * @return an HTTP response with an error, or null if no error
         */
        internal fun checkCreateRequest(request: HttpRequestMessage<String?>, bodyAsJson: JsonElement?):
            HttpResponseMessage? {
            return when {
                request.body.isNullOrBlank() ->
                    HttpUtilities.badRequestResponse(
                        request,
                        createErrorMsg("The request is missing a body with the table contents")
                    )

                bodyAsJson == null || bodyAsJson !is JsonArray ->
                    HttpUtilities.badRequestResponse(
                        request,
                        createErrorMsg("The request body must be a JSON Array")
                    )

                bodyAsJson.isEmpty() ->
                    HttpUtilities.badRequestResponse(
                        request,
                        createErrorMsg("The request body must be a non-empty JSON Array")
                    )

                bodyAsJson.any { it !is JsonObject } ->
                    HttpUtilities.badRequestResponse(
                        request,
                        createErrorMsg("All rows in the provided array must be a JSON object")
                    )

                bodyAsJson.any { it is JsonObject && it.keys.isEmpty() } ->
                    HttpUtilities.badRequestResponse(
                        request,
                        createErrorMsg("All rows in the provided array must not be empty JSON objects")
                    )

                else -> null
            }
        }
    }
}