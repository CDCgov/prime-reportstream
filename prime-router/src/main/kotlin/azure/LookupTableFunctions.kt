package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.JSONB

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
    private val mapper: ObjectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

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
            methods = [HttpMethod.GET, HttpMethod.HEAD],
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
                when (request.httpMethod) {
                    HttpMethod.HEAD -> {
                        // Get the latest date of all the tables
                        val lastModified = list.maxWithOrNull(compareBy { it.createdAt })?.createdAt
                        HttpUtilities.okResponse(request, lastModified)
                    }
                    else -> {
                        HttpUtilities.okResponse(request, mapper.writeValueAsString(list))
                    }
                }
            } catch (e: Exception) {
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
                else {
                    val rows = lookupTableAccess.fetchTable(tableName, tableVersion)
                    HttpUtilities.okResponse(request, convertTableDataToJsonString(rows))
                }
            } catch (e: Exception) {
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
            } catch (e: Exception) {
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
        return getOktaAuthenticator(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) { oktaAuthenticatedClaim ->
            val inputData: List<Map<String, String>>
            try {
                val forceTableToLoad = request.queryParameters[forceQueryParameter].toBoolean()
                inputData = mapper.readValue(request.body!!.toString())
                if (inputData.isEmpty())
                    HttpUtilities.badRequestResponse(
                        request,
                        HttpUtilities.errorJson("Request body cannot be empty.")
                    )
                else {
                    val colNames = inputData[0].keys
                    // Test all the rows to make sure they all have the same columns and that we do not have extra
                    // columns.
                    if (inputData.any { row ->
                        row.keys.size != colNames.size ||
                            colNames.any { colName ->
                                !row.containsKey(colName)
                            }
                    }
                    )
                        HttpUtilities.badRequestResponse(
                            request,
                            HttpUtilities.errorJson("All rows in the provided array must contain the same column names")
                        )
                    else {
                        // Ok, now we are good to go with the data.
                        val tableRows = inputData.map { row ->
                            JSONB.jsonb(mapper.writeValueAsString(row))
                        }
                        val latestVersion = lookupTableAccess.fetchLatestVersion(tableName) ?: 0
                        val newVersion = latestVersion + 1
                        lookupTableAccess.createTable(
                            tableName, newVersion, tableRows,
                            oktaAuthenticatedClaim.userName, forceTableToLoad
                        )

                        // Return the table version info
                        val json = mapper
                            .writeValueAsString(lookupTableAccess.fetchVersionInfo(tableName, newVersion))
                        HttpUtilities.okResponse(request, json)
                    }
                }
            } catch (e: MismatchedInputException) {
                HttpUtilities.badRequestResponse(
                    request,
                    HttpUtilities.errorJson("Invalid request body.  Must be an array of objects")
                )
            } catch (e: IllegalStateException) {
                logger.error("Unable to create lookup table $tableName", e)
                HttpUtilities.internalErrorConflictResponse(request, "New Lookup Table ${e.message}")
            } catch (e: DatabaseLookupTableAccess.Companion.DuplicateTableException) {
                logger.warn("Ignoring creation of duplicate table data for table $tableName.")
                HttpUtilities.internalErrorConflictResponse(request, "New Lookup Table ${e.message}")
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                logger.error("Unable to activate lookup table $tableName", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Converts [rows] into a JSON string.
     * @return a JSON string with the table rows
     */
    internal fun convertTableDataToJsonString(rows: List<LookupTableRow>): String {
        val convertedRows = rows.map { row ->
            mapper.readValue<Map<String, String>>(row.data.data())
        }
        return mapper.writeValueAsString(convertedRows)
    }

    companion object {
        /**
         * Name of the query parameter to show inactive tables.
         */
        const val showInactiveParamName = "showInactive"

        /**
         * Name of the query parameter to force lookup table to create regardless.
         */
        const val forceQueryParameter = "forceTableToCreate"
    }
}