package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
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
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.LookupTableFunctions
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.apache.commons.io.FileUtils
import org.apache.http.HttpStatus
import org.jooq.JSONB
import java.io.File
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.time.Instant

/**
 * Utilities to submit and get data from the Lookup Tables API.
 * If a [useThisToken] is not specified, then attempt to get a token from Otka.
 * Otherwise, [useThisToken] is sent as the bearer token to the ReportStream server.
 */
class LookupTableEndpointUtilities(val environment: Environment, val useThisToken: String? = null) {
    /**
     * Increase from the default read timeout in case of a super-duper long table.
     */
    private val requestTimeoutMillis = 130000

    /**
     * The Access Token.
     */
    private val accessToken = useThisToken ?: OktaCommand.fetchAccessToken(environment.oktaApp)
        ?: throw PrintMessage("Missing access token. Run ./prime login to fetch/refresh your access token.", true)

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
            .bearer(accessToken)
            .timeoutRead(requestTimeoutMillis)
            .responseJson()
        checkCommonErrorsFromResponse(result, response)
        try {
            return mapper.readValue(result.get().content)
        } catch (e: MismatchedInputException) {
            throw IOException("Invalid response body found.")
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
            .bearer(accessToken)
            .timeoutRead(requestTimeoutMillis)
            .responseJson()
        return getTableInfoFromResponse(result, response)
    }

    /**
     * Fetches the table content from the API given [tableName] and [version].
     * @return the table content
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun fetchTableContent(tableName: String, version: Int): List<Map<String, String>> {
        val apiUrl = environment.formUrl("$endpointRoot/$tableName/$version/content")
        val (_, response, result) = Fuel
            .get(apiUrl.toString())
            .authentication()
            .bearer(accessToken)
            .timeoutRead(requestTimeoutMillis)
            .responseJson()
        checkCommonErrorsFromResponse(result, response)
        try {
            return mapper.readValue(result.get().content)
        } catch (e: MismatchedInputException) {
            throw IOException("Invalid response body found.")
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
            .bearer(accessToken)
            .timeoutRead(requestTimeoutMillis)
            .responseJson()
        return getTableInfoFromResponse(result, response)
    }

    /**
     * Submit to the API a new table version using [tableName], [tableData], and [forceTableToCreate].
     * @return the table version information for the created table
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun createTable(tableName: String, tableData: List<Map<String, String>>, forceTableToCreate: Boolean):
        LookupTableVersion {
        val apiUrl = environment.formUrl("$endpointRoot/$tableName?table&forceTableToCreate=$forceTableToCreate")
        val jsonPayload = mapper.writeValueAsString(tableData)

        val (_, response, result) = Fuel
            .post(apiUrl.toString())
            .header(Headers.CONTENT_TYPE to HttpUtilities.jsonMediaType)
            .jsonBody(jsonPayload.toString())
            .authentication()
            .bearer(accessToken)
            .timeoutRead(requestTimeoutMillis)
            .responseJson()
        return getTableInfoFromResponse(result, response)
    }

    companion object {
        /**
         * The lookup table API endpoint.
         */
        const val endpointRoot = "/api/lookuptables"

        /**
         * Mapper to convert objects to JSON.
         */
        private val mapper = JacksonMapperUtilities.defaultMapper

        /**
         * Exception thrown with a [message] if a table is not found.
         */
        class TableNotFoundException(message: String) : Exception(message)

        /**
         * Exception thrown with a [message] if a table is not found.
         */
        class TableConflictException(message: String) : Exception(message)

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
                val info = mapper.readValue<LookupTableVersion>(result.get().content)
                if (info.tableName.isNullOrBlank() || info.tableVersion < 1 || info.createdBy.isNullOrBlank() ||
                    info.createdBy.isNullOrBlank()
                ) throw IOException("Invalid version information in the response.")
                else return info
            } catch (e: MismatchedInputException) {
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
                            // If we get a 404 with no response body then it is an endpoint not found error
                            result.error.response.body().isEmpty() -> throw IOException(error)

                            // If we do get a 404 with a JSON error message then it is because the table was not found
                            mapper.readValue<Map<String, String>>(
                                result.error.response.body()
                                    .asString(HttpUtilities.jsonMediaType)
                            ).containsKey("error") ->
                                throw TableNotFoundException(error)

                            else -> throw IOException(error)
                        }
                    } catch (e: MismatchedInputException) {
                        // The error message is not valid JSON.
                        throw IOException(error)
                    }
                }

                result is Result.Failure && response.statusCode == HttpStatus.SC_CONFLICT ->
                    throw TableConflictException(getErrorFromResponse(result))

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

                    else ->
                        mapper.readValue<Map<String, String>>(
                            result.error.response.body()
                                .asString(HttpUtilities.jsonMediaType)
                        )["error"]
                            ?: result.error.toString()
                }
            } catch (e: Exception) {
                (result as Result.Failure).error.toString()
            }
        }

        /**
         * Extract table data from a JSON [row] given a list of [colNames].
         * @return a list of data from the row in the same order as the given [colNames]
         */
        internal fun extractTableRowFromJson(row: JSONB, colNames: List<String>): List<String> {
            val decodedRow = mapper.readValue<Map<String, String>>(row.data())
            return colNames.map { colName ->
                decodedRow[colName] ?: ""
            }
        }

        /**
         * Sets the JSON for a table [row].
         * @return the JSON representation of the data
         */
        internal fun setTableRowToJson(row: Map<String, String>): JSONB {
            return JSONB.jsonb(mapper.writeValueAsString(row))
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
         * Regex used to replace new lines in table data.
         */
        private val newLineRegex = Regex("\\r\\n|\\r|\\n")

        /**
         * Converts table data in [tableRows] to a human-readable table using [colNames].
         * @param addRowNum set to true to add row numbers to the left of the table
         * @return the human-readable table
         */
        fun rowsToPrintableTable(
            tableRows: List<Map<String, String>>,
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
                    val data = colNames.map {
                        // Remove any new lines as it will affect diffs and table printouts
                        row[it]?.replace(newLineRegex, "") ?: ""
                    }.toMutableList()
                    if (addRowNum) data.add(0, (index + 1).toString())
                    // Row takes varargs, so we convert the list to varargs
                    row(values = data.map { it }.toTypedArray())
                }
            }.render()
        }

        /**
         * Converts a [versionList] to a human-readable table.
         * @return the human-readable table
         */
        fun infoToPrintableTable(versionList: List<LookupTableVersion>): StringBuilder {
            Preconditions.checkArgument(versionList.isNotEmpty())

            return table {
                hints {
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header(
                    "Table Name", "Version", "Table Sha256 Checksum", "Is Active", "Created By",
                    "Created At"
                )
                versionList.forEach {
                    row(
                        it.tableName, it.tableVersion, it.tableSha256Checksum, it.isActive.toString(),
                        it.createdBy, it.createdAt.toString()
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
            version1Table: List<Map<String, String>>,
            version2Table: List<Map<String, String>>,
            showAll: Boolean
        ): List<String> {
            val generator = DiffRowGenerator.create()
                .showInlineDiffs(true)
                .mergeOriginalRevised(true)
                .inlineDiffByWord(true)
                .ignoreWhiteSpaces(true)
                .oldTag { start: Boolean? ->
                    if (true == start) "\u001B[9;101m" else "\u001B[0m" // Use strikethrough and red for deleted changes
                }
                .newTag { start: Boolean? ->
                    if (true == start) "\u001B[1;42m" else "\u001B[0m" // Use bold and green for additions
                }
                .build()

            // We need to make sure to use the same order of column names
            val colNames = version2Table[0].keys.toList()
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
                diffBuffer.add("")
                diffBuffer.add("Found:")
                diffBuffer.add("\tChanges  : ${diff.count { it.tag == DiffRow.Tag.CHANGE }}")
                diffBuffer.add("\tAdditions: ${diff.count { it.tag == DiffRow.Tag.INSERT }}")
                diffBuffer.add("\tDeletions: ${diff.count { it.tag == DiffRow.Tag.DELETE }}")
            } else diffBuffer.clear()
            return diffBuffer
        }
    }
}

/**
 * Generic lookup table command.
 */
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
                echo("")
                echo("Table name: $tableName")
                echo("Version: $version")
                val colNames = tableList[0].keys.toList()
                echo(LookupTableCommands.rowsToPrintableTable(tableList, colNames))
                echo("")
            } else {
                saveTable(outputFile!!, tableList)
                echo(
                    "Saved ${tableList.size} rows of table $tableName version $version " +
                        "to ${outputFile!!.absolutePath} "
                )
            }
        } else {
            echo("Table $tableName version $version has no rows.")
        }
    }

    /**
     * Save table data in [tableRows] to an [outputFile] in CSV format.
     */
    private fun saveTable(outputFile: File, tableRows: List<Map<String, String>>) {
        val colNames = tableRows[0].keys.toList()
        val rows = mutableListOf(colNames)
        tableRows.forEach { row ->
            rows.add(
                colNames.map { colName ->
                    row[colName] ?: ""
                }
            )
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
     * Silent running.  No table contents or diff output or confirmation if true.
     */
    private val silent by option("-s", "--silent", help = "Do not generate diff or ask for confirmation").flag()

    /**
     * Activate a created table in one shot.
     */
    private val activate by option("-a", "--activate", help = "Activate the table upon creation")
        .flag(default = false)

    /**
     * The table name.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the operation on")
        .required()

    /**
     * Show the raw input table being created if set to true.
     */
    private val showTable by option("--show-table", help = "Always show the table to be created")
        .flag(default = false)

    /**
     * Force to create table(s).
     */
    private val forceTableToCreate by option(
        "-f", "--force",
        help = "Force the creation of new table(s) even if it is already exist"
    ).flag()

    companion object {
        /**
         * Maximum number of table columns allowed to be displayed by default.  If a table exceeds this then the user
         * must use the --show-table option to show the raw table.
         */
        private const val SMALL_TABLE_MAX_COLS = 7

        /**
         * Maximum number of table rows allowed to be displayed by default.  If a table exceeds this then the user
         * must use the --show-table option to show the raw table.
         */
        private const val SMALL_TABLE_MAX_ROWS = 50
    }

    override fun run() {
        // Read the input file.
        val inputData = csvReader().readAllWithHeader(inputFile)
        // Note: csvReader returns size of data-row(s) and NOT include the header-row.
        // (i.e. If the file contains of header row, it returns size = 0)
        if (inputData.isEmpty()) {
            echo("ERROR: Input file ${inputFile.absolutePath} has no data.")
            return
        }

        // Output the data for review specified.
        val isLargeTable = inputData.size > SMALL_TABLE_MAX_ROWS || inputData[0].keys.size > SMALL_TABLE_MAX_COLS
        // Display the table when not silent, plus display it only if it is a small table unless told otherwise.
        if (!silent && (showTable || !isLargeTable)) {
            echo("Here is the table data to be created:")
            val colNames = inputData[0].keys.toList()
            echo(LookupTableCommands.rowsToPrintableTable(inputData, colNames))
            echo("")
        }

        // If there is an existing active version then present a diff.
        val tableList = try {
            tableUtil.fetchList()
        } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", true)
        }
        val activeVersion = (tableList.firstOrNull { it.tableName == tableName })?.tableVersion ?: 0
        if (!silent && activeVersion > 0) {
            val activeTable = try { tableUtil.fetchTableContent(tableName, activeVersion) } catch (e: Exception) {
                throw PrintMessage("Error fetching active table content for table $tableName: ${e.message}", true)
            }
            echo("Generating a diff view of the changes.  For large tables this can take a while...")
            val diffOutput = LookupTableCommands.generateDiff(activeTable, inputData, false)
            if (diffOutput.isNotEmpty()) {
                echo("Here is the diff compared to the active version $activeVersion:")
                diffOutput.forEach { echo(it) }
                echo("")
            } else {
                echo(
                    "Error: The table you are trying to create is identical to the active version " +
                        "$activeVersion."
                )
                return
            }
        }

        // Now we are ready.  Ask if we should proceed.
        if ((
            !silent && confirm("Continue to create a new version of $tableName with ${inputData.size} rows?")
                == true
            ) || silent
        ) {
            val newTableInfo = try {
                tableUtil.createTable(tableName, inputData, forceTableToCreate)
            } catch (e: IOException) {
                throw PrintMessage("\tError creating new table version for $tableName: ${e.message}", true)
            } catch (e: LookupTableEndpointUtilities.Companion.TableConflictException) {
                val dupVersion = e.message?.substringAfterLast("version")
                echo(
                    "Skipping creation of duplicate table $tableName since it is duplicated with version$dupVersion."
                )
                return
            }

            echo(
                "\t${inputData.size} rows created for lookup table $tableName version " +
                    "${newTableInfo.tableVersion}."
            )
            // Always have an active version, so if this is the first version then activate it.
            if (activate || newTableInfo.tableVersion == 1) {
                try { tableUtil.activateTable(tableName, newTableInfo.tableVersion) } catch (e: Exception) {
                    throw PrintMessage(
                        "\tError activating table $tableName version ${newTableInfo.tableVersion}. " +
                            "Table was created.  Try to activate it. : ${e.message}",
                        true
                    )
                }
                echo("\tTable version ${newTableInfo.tableVersion} is now active.")
            } else
                echo(
                    "\tTable version ${newTableInfo.tableVersion} " +
                        "left inactive, so don't forget to activate it."
                )
        } else
            echo("\tAborted the creation of the lookup table.")
    }
}

/**
 * List the available lookup tables.
 */
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
            echo("Listing all lookup tables including inactive.")
        else
            echo("Listing only active lookup tables.")

        if (data.isNotEmpty()) {
            echo(
                LookupTableCommands
                    .infoToPrintableTable(data)
            )
            echo("")
        } else {
            if (data.isEmpty() && !showInactive)
                echo("No lookup tables were found.")
            else
                echo("No active lookup tables were found.")
        }
    }
}

/**
 * Show a diff between two lookup tables.
 */
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
        echo("Comparing lookup table $tableName versions $version1 and $version2:")
        echo(LookupTableCommands.infoToPrintableTable(listOf(version1Info, version2Info)))
        echo("")

        val output = LookupTableCommands.generateDiff(version1Table, version2Table, fullDiff)

        if (output.isNotEmpty()) {
            output.forEach { echo(it) }
        } else
            echo("Lookup table $tableName version $version1 and $version2 are identical.")
    }
}

/**
 * Activate a lookup table.
 */
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
                echo("Lookup table $tableName is not currently active")

            else ->
                echo(
                    "Current Lookup table $tableName's active version number is " +
                        "${currentlyActiveTable.tableVersion}"
                )
        }

        if (confirm("Set $version as active?") == true) {
            val activatedStatus = tableUtil.activateTable(tableName, version)
            if (activatedStatus.isActive)
                echo("Version $version for lookup table $tableName was set active.")
            else
                error("Unknown error when setting lookup table $tableName Version $version to active.")
        } else
            echo("Aborted the activation of the lookup table.")
    }
}

/**
 * Load lookup tables from a directory.
 */
class LookupTableLoadAllCommand : GenericLookupTableCommand(
    name = "loadall",
    help = "Load all the tables stored as CSV in the specified directory"
) {
    /**
     * Default directory for tables.
     */
    private val defaultDir = "./metadata/tables/local"

    /**
     * The table name.
     */
    private val dir by option(
        "-d", "--directory",
        help = "The path to the directory with the table CSV files.  Defaults to $defaultDir"
    )
        .file(mustExist = true, canBeFile = false, canBeDir = true, mustBeReadable = true)
        .default(File(defaultDir))

    /**
     * Number of connection retries.
     */
    private val connRetries by option("-r", "--retries", help = "Number of seconds to retry waiting for the API")
        .int().default(30)

    /**
     * Number of connection retries.
     */
    private val checkLastModified by option(
        "--check-last-modified",
        help = "Update settings only if input file is newer"
    )
        .flag(default = false)

    /**
     * Activate a created table in one shot.
     */
    private val forceTableToCreate by option(
        "-f", "--force",
        help = "Force the creation of new table(s) even if it already exists"
    ).flag()

    /**
     * The reference to the table creator command.
     */
    private val tableCreator = LookupTableCreateCommand()

    override fun run() {
        if (environment != Environment.LOCAL) error("This command is only allowed in the local environment.")

        // First wait for the API to come online
        echo("Waiting for the API at ${environment.url} to be available...")
        CommandUtilities.waitForApi(environment, connRetries)

        // Get the list of current tables to only update or create new ones.
        val tableUpdateTimes = if (checkLastModified)
            LookupTableEndpointUtilities(environment).fetchList().map {
                it.tableName to it.createdAt
            }.toMap()
        else emptyMap()

        // Loop through all the files
        val files = try {
            FileUtils.listFiles(dir, arrayOf("csv"), false)
        } catch (e: NoSuchFileException) {
            error("Directory ${dir.absolutePath} does not exist")
        }

        echo("Loading ${files.size} tables from ${dir.absolutePath}...")
        files.forEach {
            val tableName = it.nameWithoutExtension

            var needToLoad = true
            // If we have a table in the database then only update it if the last modified time of the file is
            // greater than the created time in the database.
            if (checkLastModified && tableUpdateTimes.contains(tableName) && tableUpdateTimes[tableName] != null) {
                val fileUpdatedTime = Instant.ofEpochMilli(it.lastModified())
                if (!fileUpdatedTime.isAfter(tableUpdateTimes[tableName]!!.toInstant())) {
                    needToLoad = false
                    echo("Skipping $tableName since it has not been updated.")
                }
            }

            if (needToLoad) {
                echo("Creating table $tableName...")
                val args: MutableList<String> = mutableListOf(
                    "-e", environment.toString().lowercase(), "-n", tableName,
                    "-i", it.absolutePath, "-s", "-a"
                )
                if (forceTableToCreate) args.add("-f")
                tableCreator.main(args)
            }
        }
        echo("Done.")
    }
}