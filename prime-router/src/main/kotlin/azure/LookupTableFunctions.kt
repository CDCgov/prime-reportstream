package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.JSONB

/**
 * Functions to manage lookup tables.
 */
class LookupTableFunctions(
    private val lookupTableAccess: DatabaseLookupTableAccess = DatabaseLookupTableAccess()
) : Logging {
    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper = JacksonMapperUtilities.defaultMapper

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
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        // No authorization check!   Anyone authenticated in ReportStream is allowed to access this function.
        logger.info("User ${claims.userName} is authorized for endpoint ${request.uri}")

        return try {
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
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        // No authorization check!   Anyone authenticated in ReportStream is allowed to access this function.
        logger.info("User ${claims.userName} is authorized for endpoint ${request.uri}")

        return try {
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

    /**
     * Fetch the data for a specific table version.
     */
    @FunctionName("getActiveLookupTableData")
    fun getActiveLookupTableData(
        @HttpTrigger(
            name = "getActiveLookupTableData",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "lookuptables/{tableName}/content"
        ) request: HttpRequestMessage<String?>,
        @BindingName("tableName") tableName: String,
    ): HttpResponseMessage {
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        // No authorization check!   Anyone authenticated in ReportStream is allowed to access this function.
        logger.info("User ${claims.userName} is authorized for endpoint ${request.uri}")

        return try {
            val tableVersion = lookupTableAccess.fetchActiveVersion(tableName)
            if (tableVersion == null) {
                HttpUtilities.notFoundResponse(
                    request,
                    "Unable to find any active version of lookup table named '$tableName'."
                )
            } else {
                val rows = lookupTableAccess.fetchTable(tableName, tableVersion)
                HttpUtilities.okResponse(request, convertTableDataToJsonString(rows))
            }
        } catch (e: Exception) {
            logger.error("Unable to fetch an active version of lookup table named `$tableName`.", e)
            HttpUtilities.internalErrorResponse(request)
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
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        // No authorization check!   Anyone authenticated in ReportStream is allowed to access this function.
        logger.info("User ${claims.userName} is authorized for endpoint ${request.uri}")

        return try {
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
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        // Do authorization
        if (!authorizedForLookupWrite(claims)) {
            return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
        }
        logger.info("User ${claims.userName} is authorized for endpoint ${request.uri}")

        return try {
            val forceTableToLoad = request.queryParameters[forceQueryParameter].toBoolean()
            val inputData: List<Map<String, String>> = mapper.readValue(request.body!!.toString())
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
                        claims.userName, forceTableToLoad
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
        // Do authentication
        val claims = AuthenticatedClaims.authenticate(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        // Do authorization
        if (!authorizedForLookupWrite(claims)) {
            return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
        }
        logger.info("User ${claims.userName} is authorized for endpoint ${request.uri}")

        return try {
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

    /**
     * @return true if these [claims] allow the user to do writes to lookup tables.  Otherwise return false
     */
    private fun authorizedForLookupWrite(claims: AuthenticatedClaims): Boolean {
        return if (!claims.isPrimeAdmin) {
            logger.warn("Request to write lookup tables is Unauthorized.  Must be a PrimeAdmin.")
            false
        } else {
            true
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