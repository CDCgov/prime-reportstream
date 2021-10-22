package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.common.base.Preconditions
import de.m3y.kformat.Table
import de.m3y.kformat.table
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.LookupTableFunctions
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.common.Environment
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.http.HttpStatus
import org.jooq.JSONB
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Utilities to submit and get data from the Lookup Tables API.
 */
class LookupTableEndpointUtilities(val environment: Environment) {
    /**
     * The Okta Access Token.
     */
    private val oktaAccessToken = OktaCommand.fetchAccessToken(environment.oktaApp)
        ?: throw PrintMessage("Invalid access token. Run ./prime login to fetch/refresh your access token.", true)

    /**
     * Fetches the list of tables from the API.
     * @return if [listInactive] is false then only a list of active tables is returned, otherwise all tables are listed
     * @throws IOException if there is a server or API error
     */
    fun fetchList(listInactive: Boolean = false): List<LookupTableVersion> {
        val apiUrl = environment.formUrl("$endpointRoot/list")
        val (_, response, result) = Fuel
            .get(apiUrl.toString(), listOf(LookupTableFunctions.showInactiveParamName to listInactive.toString()))
            .authentication()
            .bearer(oktaAccessToken)
            .responseJson()
        val jsonArray = getArrayFromResponse(result, response)
        return jsonArray.map {
            convertJsonToInfo(it as JsonObject)
        }
    }

    /**
     * Submits the activation of a table to the API given [tableName] and [version].
     * @return the table version information for the activated table
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun activateTable(tableName: String, version: Int):
        LookupTableVersion {
        val apiUrl = environment.formUrl("$endpointRoot/$tableName/$version/activate")
        val (_, response, result) = Fuel
            .put(apiUrl.toString())
            .authentication()
            .bearer(oktaAccessToken)
            .responseJson()
        return getTableInfoFromResponse(result, response)
    }

    /**
     * Fetches the table content from the API given [tableName] and [version].
     * @return the table content
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun fetchTableContent(tableName: String, version: Int): List<LookupTableRow> {
        val apiUrl = environment.formUrl("$endpointRoot/$tableName/$version/content")
        val (_, response, result) = Fuel
            .get(apiUrl.toString())
            .authentication()
            .bearer(oktaAccessToken)
            .responseJson()
        val jsonArray = getArrayFromResponse(result, response)
        return jsonArray.map {
            val row = LookupTableRow()
            row.data = JSONB.jsonb(it.toString())
            row
        }
    }

    /**
     * Fetches the table version information from the API given [tableName] and [version].
     * @return the table version information
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun fetchTableInfo(tableName: String, version: Int): LookupTableVersion {
        val apiUrl = environment.formUrl("$endpointRoot/$tableName/$version/info")
        val (_, response, result) = Fuel
            .get(apiUrl.toString())
            .authentication()
            .bearer(oktaAccessToken)
            .responseJson()
        return getTableInfoFromResponse(result, response)
    }

    /**
     * Submit to the API a new table version using [tableName] and [tableData].
     * @return the table version information for the created table
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun createTable(tableName: String, tableData: List<LookupTableRow>):
        LookupTableVersion {
        val apiUrl = environment.formUrl("$endpointRoot/$tableName")
        val jsonRows = tableData.map {
            Json.parseToJsonElement(it.data.toString())
        }
        val jsonPayload = JsonArray(jsonRows)

        val (_, response, result) = Fuel
            .post(apiUrl.toString())
            .header(Headers.CONTENT_TYPE to HttpUtilities.jsonMediaType)
            .jsonBody(jsonPayload.toString())
            .authentication()
            .bearer(oktaAccessToken)
            .responseJson()
        return getTableInfoFromResponse(result, response)
    }

    companion object {
        /**
         * The lookup table API endpoint.
         */
        const val endpointRoot = "/api/lookuptables"

        /**
         * Exception thrown with a [message] if a table is not found.
         */
        class TableNotFoundException(message: String) : Exception(message)

        internal fun convertJsonToInfo(info: JsonObject): LookupTableVersion {
            val newVersion = LookupTableVersion()
            try {
                newVersion.tableName = (info["tableName"] as JsonPrimitive).content
                newVersion.tableVersion = (info["tableVersion"] as JsonPrimitive).int
                newVersion.isActive = (info["isActive"] as JsonPrimitive).content.toBoolean()
                newVersion.createdBy = (info["createdBy"] as JsonPrimitive).content
                newVersion.createdAt = OffsetDateTime.parse((info["createdAt"] as JsonPrimitive).content)
            } catch (e: NullPointerException) {
                throw IOException("One or more version fields were empty in table version info.")
            } catch (e: NumberFormatException) {
                throw IOException("Invalid version number in table version info.")
            } catch (e: DateTimeParseException) {
                throw IOException("Invalid created at time in table version info.")
            }
            return newVersion
        }
        /**
         * Gets a JSON array from a [result] and [response] returned by the API.
         * @return a JSON array
         * @throws TableNotFoundException if the table and/or version is not found
         * @throws IOException if there is a server or API error
         */
        internal fun getArrayFromResponse(result: Result<FuelJson, FuelError>, response: Response): JsonArray {
            checkCommonErrorsFromResponse(result, response)
            try {
                when {
                    Json.parseToJsonElement(result.get().content) !is JsonArray ->
                        throw IOException("Invalid data returned in response is not an array.")

                    Json.parseToJsonElement(result.get().content).jsonArray.any { it !is JsonObject } ->
                        throw IOException("Invalid data returned in response is not an array ob objects.")

                    else -> return Json.parseToJsonElement(result.get().content).jsonArray
                }
            } catch (e: SerializationException) {
                throw IOException("Invalid JSON response.")
            }
        }

        /**
         * Gets a table version information object from a [result] and [response] returned by the API.
         * @return a table version information object
         * @throws TableNotFoundException if the table and/or version is not found
         * @throws IOException if there is a server or API error
         */
        internal fun getTableInfoFromResponse(
            result: Result<FuelJson, FuelError>,
            response: Response
        ): LookupTableVersion {
            checkCommonErrorsFromResponse(result, response)
            try {
                return when {
                    Json.parseToJsonElement(result.get().content) !is JsonObject ->
                        throw IOException("Invalid response body")

                    else -> {
                        convertJsonToInfo(Json.parseToJsonElement(result.get().content) as JsonObject)
                    }
                }
            } catch (e: SerializationException) {
                throw IOException("Invalid JSON response.")
            }
        }

        /**
         * Check for an error response from a [result] and [response] from the API.
         * @throws TableNotFoundException if the table and/or version is not found
         * @throws IOException if there is a server or API error
         */
        internal fun checkCommonErrorsFromResponse(result: Result<FuelJson, FuelError>, response: Response) {
            when {
                result is Result.Failure && response.statusCode == HttpStatus.SC_NOT_FOUND -> {
                    val error = getErrorFromResponse(result)
                    try {
                        when {
                            // If we get a 404 with no response body then it is an endpoint not found erro
                            result.error.response.body().isEmpty() -> throw IOException(error)

                            // If we do get a 404 with a JSON error message then it is because the table was not found
                            Json.parseToJsonElement(result.error.response.body().asString(HttpUtilities.jsonMediaType))
                                .jsonObject.containsKey("error") ->
                                throw TableNotFoundException(error)

                            else -> throw IOException(error)
                        }
                    } catch (e: SerializationException) {
                        // The error message is not JSON.
                        throw IOException(error)
                    }
                }

                result is Result.Failure ->
                    throw IOException(getErrorFromResponse(result))

                result.get().content.isBlank() ->
                    throw IOException("Empty response body")
            }
        }

        /**
         * Get the error message from a [result] as returned by the API.
         * @return The error as a string or null if no error is found.
         */
        internal fun getErrorFromResponse(result: Result<FuelJson, FuelError>): String {
            return try {
                when {
                    result !is Result.Failure -> ""

                    result.error.response.body().isEmpty() -> result.error.toString()

                    Json.parseToJsonElement(
                        result.error.response.body()
                            .asString(HttpUtilities.jsonMediaType)
                    ) !is JsonObject ->
                        result.error.toString()

                    !Json.parseToJsonElement(result.error.response.body().asString(HttpUtilities.jsonMediaType))
                        .jsonObject.containsKey("error") ->
                        result.error.toString()

                    else -> Json.parseToJsonElement(result.error.response.body().asString(HttpUtilities.jsonMediaType))
                        .jsonObject["error"]?.jsonPrimitive?.contentOrNull ?: ""
                }
            } catch (e: SerializationException) {
                (result as Result.Failure).error.toString()
            }
        }
    }
}

/**
 * Commands to manipulate lookup tables.
 */
class LookupTableCommands : CliktCommand(
    name = "lookuptables",
    help = "Manage lookup tables"
) {

    override fun run() {
        // No operation.  The help will be printed out as default.
    }

    companion object {
        /**
         * Extract table data from a JSON [row] given a list of [colNames].
         * @return a list of data from the row in the same order as the given [colNames]
         */
        internal fun extractTableRowFromJson(row: JSONB, colNames: List<String>): List<String> {
            val rowData = mutableListOf<String>()
            val jsonData = Json.parseToJsonElement(row.data()) as JsonObject
            colNames.forEach { colName ->
                val value = if (jsonData[colName] != null)
                    (jsonData[colName] as JsonPrimitive).content
                else
                    ""
                rowData.add(value)
            }
            return rowData
        }

        /**
         * Sets the JSON for a table [row].
         * @return the JSON representation of the data
         */
        internal fun setTableRowToJson(row: Map<String, String>): JSONB {
            val colNames = row.keys.toList()
            val retVal = buildJsonObject {
                colNames.forEach { col ->
                    put(col, JsonPrimitive(row[col]))
                }
            }
            return JSONB.jsonb(retVal.toString())
        }

        /**
         * Converts table data in [tableRows] to a human readable table using [colNames].
         * @param addRowNum set to true to add row numbers to the left of the table
         * @return the human readable table
         */
        fun rowsToPrintableTable(
            tableRows: List<LookupTableRow>,
            colNames: List<String>,
            addRowNum: Boolean = true
        ): StringBuilder {
            Preconditions.checkArgument(tableRows.isNotEmpty())

            return table {
                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }

                val headers = colNames.toMutableList()
                if (addRowNum) headers.add(0, "Row #")
                header(headers)
                tableRows.forEachIndexed { index, row ->
                    val data = extractTableRowFromJson(row.data, colNames).toMutableList()
                    if (addRowNum) data.add(0, (index + 1).toString())
                    // Row takes varargs, so we convert the list to varargs
                    row(values = data.map { it }.toTypedArray())
                }
            }.render()
        }

        /**
         * Converts a [versionList] to a human readable table.
         * @return the human readable table
         */
        fun infoToPrintableTable(versionList: List<LookupTableVersion>): StringBuilder {
            Preconditions.checkArgument(versionList.isNotEmpty())

            return table {
                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Table Name", "Version", "Is Active", "Created By", "Created At")
                versionList.forEach {
                    row(
                        it.tableName, it.tableVersion, it.isActive.toString(), it.createdBy,
                        it.createdAt.toString()
                    )
                }
            }.render()
        }

        /**
         * Generate a diff between [version1Table] and [version2Table].  If [showAll] is true then unchanged
         * lines are included in the diff.
         * @return a string with the text output or an empty string if there are no differences
         */
        fun generateDiff(
            version1Table: List<LookupTableRow>,
            version2Table: List<LookupTableRow>,
            showAll: Boolean
        ): List<String> {
            val generator = DiffRowGenerator.create()
                .showInlineDiffs(true)
                .mergeOriginalRevised(true)
                .inlineDiffByWord(true)
                .oldTag { start: Boolean? ->
                    if (true == start) "\u001B[9m" else "\u001B[0m" // Use strikethrough for deleted changes
                }
                .newTag { start: Boolean? ->
                    if (true == start) "\u001B[1m" else "\u001B[0m" // Use bold for additions
                }
                .build()

            // We need to make sure to use the same order of column names
            val colNames = DatabaseLookupTableAccess.extractTableHeadersFromJson(version1Table[0].data)
            val diff = generator.generateDiffRows(
                rowsToPrintableTable(version1Table, colNames, false).toString().split("\n"),
                rowsToPrintableTable(version2Table, colNames, false).toString().split("\n")
            )

            val diffBuffer = mutableListOf<String>()
            val hasChanges = diff.any { it.tag != DiffRow.Tag.EQUAL }
            if (hasChanges) {
                if (showAll) {
                    diff.forEach { row ->
                        diffBuffer.add(row.oldLine)
                    }
                } else {
                    var changed = true
                    diff.forEachIndexed { index, row ->
                        if (index < 2)
                            diffBuffer.add(row.oldLine)
                        else if (row.tag == DiffRow.Tag.EQUAL && changed) {
                            diffBuffer.add("...")
                            changed = false
                        } else if (row.tag != DiffRow.Tag.EQUAL) {
                            diffBuffer.add(row.oldLine)
                            changed = true
                        }
                    }
                }
            } else diffBuffer.clear()
            return diffBuffer
        }
    }
}

abstract class GenericLookupTableCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
    /**
     * The environment to connect to.
     */
    private val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment.\nChoose between [local|test|staging|prod]"
    )
        .choice(
            Environment.LOCAL.envName, Environment.STAGING.envName,
            Environment.TEST.envName, Environment.PROD.envName
        )
        .default(Environment.LOCAL.envName, "local environment")

    /**
     * The environment the command needs to run on.
     */
    internal val environment get() = Environment.get(env)

    /**
     * The lookup table utility.
     */
    internal val tableUtil get() = LookupTableEndpointUtilities(environment)
}

/**
 * Print out a lookup table.
 */
class LookupTableGetCommand : GenericLookupTableCommand(
    name = "get",
    help = "Fetch the contents of a lookup table"
) {
    /**
     * Optional output file to save the table to.
     */
    private val outputFile by option("-o", "--output-file", help = "Specify a file to save the table's data as CSV")
        .file(false, canBeDir = false)

    /**
     * Table name option.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * Table version option.
     */
    private val version by option("-v", "--version", help = "The version of the table to get").int()
        .required()

    override fun run() {
        val tableList = try {
            tableUtil.fetchTableContent(tableName, version)
        } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
            throw PrintMessage("The table $tableName version $version was not found.", true)
        } catch (e: IOException) {
            throw PrintMessage("Error fetching the contents of table $tableName version $version: ${e.message}", true)
        }
        if (tableList.isNotEmpty()) {
            // Output to a file if requested, otherwise output to the screen.
            if (outputFile == null) {
                TermUi.echo("")
                TermUi.echo("Table name: $tableName")
                TermUi.echo("Version: $version")
                val colNames = DatabaseLookupTableAccess.extractTableHeadersFromJson(tableList[0].data)
                TermUi.echo(LookupTableCommands.rowsToPrintableTable(tableList, colNames))
                TermUi.echo("")
            } else {
                saveTable(outputFile!!, tableList)
                TermUi.echo(
                    "Saved ${tableList.size} rows of table $tableName version $version " +
                        "to ${outputFile!!.absolutePath} "
                )
            }
        } else {
            TermUi.echo("Table $tableName version $version has no rows.")
        }
    }

    /**
     * Save table data in [tableRows] to an [outputFile] in CSV format.
     */
    private fun saveTable(outputFile: File, tableRows: List<LookupTableRow>) {
        val colNames = DatabaseLookupTableAccess.extractTableHeadersFromJson(tableRows[0].data)
        val rows = mutableListOf(colNames)
        tableRows.forEach { row ->
            // Row takes varargs, so we convert the list to varargs
            rows.add(LookupTableCommands.extractTableRowFromJson(row.data, colNames))
        }
        csvWriter().writeAll(rows, outputFile.outputStream())
    }
}

/**
 * Create a new lookup table.
 */
class LookupTableCreateCommand : GenericLookupTableCommand(
    name = "create",
    help = "Create a new version of a lookup table"
) {
    /**
     * The input file to get the table data from.
     */
    private val inputFile by option("-i", "--input-file", help = "Input CSV file with the table data")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    override fun run() {
        // Read the input file.
        val inputData = csvReader().readAllWithHeader(inputFile)
        if (inputData.size <= 1)
            error("Input file ${inputFile.absolutePath} has no data.")
        val newTableData = inputData.map { row ->
            val tableRow = LookupTableRow()
            tableRow.data = LookupTableCommands.setTableRowToJson(row)
            tableRow
        }

        // Output the data for review.
        TermUi.echo("Here is the table data to be created:")
        val colNames = DatabaseLookupTableAccess.extractTableHeadersFromJson(newTableData[0].data)
        TermUi.echo(LookupTableCommands.rowsToPrintableTable(newTableData, colNames))
        TermUi.echo("")

        // If there is an existing active version then present a diff.
        val tableList = try {
            tableUtil.fetchList()
        } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", true)
        }
        val activeVersion = (tableList.firstOrNull { it.tableName == tableName })?.tableVersion ?: 0
        if (activeVersion > 0) {
            val activeTable = try { tableUtil.fetchTableContent(tableName, activeVersion) } catch (e: Exception) {
                throw PrintMessage("Error fetching active table content for table $tableName: ${e.message}", true)
            }
            val diffOutput = LookupTableCommands.generateDiff(activeTable, newTableData, false)
            if (diffOutput.isNotEmpty()) {
                TermUi.echo("Here is the diff compared to the active version $activeVersion:")
                diffOutput.forEach { TermUi.echo(it) }
                TermUi.echo("")
            } else {
                TermUi.echo(
                    "Error: The table you are trying to create is identical to the active version " +
                        "$activeVersion."
                )
                return
            }
        }

        // Now we are ready.  Ask if we should proceed.
        if (TermUi.confirm("Continue to create a new version of $tableName with ${newTableData.size} rows?")
            == true
        ) {
            val newTableInfo = try { tableUtil.createTable(tableName, newTableData) } catch (e: IOException) {
                throw PrintMessage("Error creating new table version for $tableName: ${e.message}", true)
            }
            TermUi.echo(
                "${newTableData.size} rows created for lookup table $tableName version " +
                    "${newTableInfo.tableVersion}."
            )
            // Always have an active version, so if this is the first version then activate it.
            if (newTableInfo.tableVersion == 1) {
                try { tableUtil.activateTable(tableName, newTableInfo.tableVersion) } catch (e: Exception) {
                    throw PrintMessage(
                        "Error activating table $tableName version ${newTableInfo.tableVersion}. " +
                            "Table was created.  Try to activate it. : ${e.message}",
                        true
                    )
                }
                TermUi.echo("Table version ${newTableInfo.tableVersion} is now active.")
            } else
                TermUi.echo("Table version ${newTableInfo.tableVersion} left inactive, so don't forget to activate it.")
        } else
            TermUi.echo("Aborted the creation of the lookup table.")
    }
}

class LookupTableListCommand : GenericLookupTableCommand(
    name = "list",
    help = "List the lookup tables"
) {
    /**
     * List all the tables including inactive ones if set.
     */
    private val showInactive by option("-a", "--all", help = "List all active and inactive tables")
        .flag(default = false)

    override fun run() {
        val data = try { tableUtil.fetchList(showInactive) } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", true)
        }
        if (showInactive)
            TermUi.echo("Listing all lookup tables including inactive.")
        else
            TermUi.echo("Listing only active lookup tables.")

        if (data.isNotEmpty()) {
            TermUi.echo(
                LookupTableCommands
                    .infoToPrintableTable(data)
            )
            TermUi.echo("")
        } else {
            if (data.isEmpty() && !showInactive)
                TermUi.echo("No lookup tables were found.")
            else
                TermUi.echo("No active lookup tables were found.")
        }
    }
}

class LookupTableDiffCommand : GenericLookupTableCommand(
    name = "diff",
    help = "Generate a difference between two versions of a lookup table"
) {
    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * The table version to compare to.
     */
    private val version1 by option("-v1", "--version1", help = "original version of the table to compare from")
        .int().required()

    /**
     * The table version to compare from.
     */
    private val version2 by option("-v2", "--version2", help = "revised version of the table to compare to")
        .int().required()

    /**
     * Display the entire diff output including unchanged lines if set.
     */
    private val fullDiff by option("-a", "--all", help = "Show all diff lines including unchanged lines")
        .flag(default = false)

    override fun run() {
        // Get the version information for the tables.  This checks if the tables exist.
        val version1Info = try {
            tableUtil.fetchTableInfo(tableName, version1)
        } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
            throw PrintMessage("The table $tableName version $version1 was not found.", true)
        } catch (e: IOException) {
            throw PrintMessage("Error fetching table version for $tableName version $version1: ${e.message}", true)
        }
        val version2Info = try {
            tableUtil.fetchTableInfo(tableName, version2)
        } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
            throw PrintMessage("The table $tableName version $version2 was not found.", true)
        } catch (e: IOException) {
            throw PrintMessage("Error fetching table version for $tableName version $version2: ${e.message}", true)
        }

        // Now get the content.
        val version1Table = try { tableUtil.fetchTableContent(tableName, version1) } catch (e: Exception) {
            throw PrintMessage("Error fetching table content for $tableName version $version1: ${e.message}", true)
        }
        val version2Table = try { tableUtil.fetchTableContent(tableName, version2) } catch (e: Exception) {
            throw PrintMessage("Error fetching table content for $tableName version $version2: ${e.message}", true)
        }

        // Generate the diff.
        TermUi.echo("Comparing lookup table $tableName versions $version1 and $version2:")
        TermUi.echo(LookupTableCommands.infoToPrintableTable(listOf(version1Info, version2Info)))
        TermUi.echo("")

        val output = LookupTableCommands.generateDiff(version1Table, version2Table, fullDiff)

        if (output.isNotEmpty()) {
            output.forEach { TermUi.echo(it) }
        } else
            TermUi.echo("Lookup table $tableName version $version1 and $version2 are identical.")
    }
}

class LookupTableActivateCommand : GenericLookupTableCommand(
    name = "activate",
    help = "Activate a specific version of a lookup table"
) {
    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * Table version option.
     */
    private val version by option("-v", "--version").int().required()

    override fun run() {
        // Verify the table exists
        try {
            tableUtil.fetchTableInfo(tableName, version)
        } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
            throw PrintMessage("The table $tableName version $version was not found.", true)
        } catch (e: IOException) {
            throw PrintMessage("Error fetching table version for $tableName version $version: ${e.message}", true)
        }

        // Find the currently active version
        val tableList = try { tableUtil.fetchList(true) } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", true)
        }
        val currentlyActiveTable = tableList.firstOrNull { it.tableName == tableName && it.isActive }
        when {
            currentlyActiveTable != null && currentlyActiveTable.tableVersion == version ->
                throw PrintMessage(
                    "Nothing to do. Lookup table $tableName's active version number is already " +
                        "$version."
                )

            currentlyActiveTable == null ->
                TermUi.echo("Lookup table $tableName is not currently active")

            else ->
                TermUi.echo("Current Lookup table $tableName's active version number is $version")
        }

        if (TermUi.confirm("Set $version as active?") == true) {
            val activatedStatus = tableUtil.activateTable(tableName, version)
            if (activatedStatus.isActive)
                TermUi.echo("Version $version for lookup table $tableName was set active.")
            else
                error("Unknown error when setting lookup table $tableName Version $version to active.")
        } else
            TermUi.echo("Aborted the activation of the lookup table.")
    }
}