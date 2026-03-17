package gov.cdc.prime.router.cli

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.common.base.Preconditions
import de.m3y.kformat.Table
import de.m3y.kformat.table
import gov.cdc.prime.router.azure.LookupTableFunctions
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.cli.FileUtilities.saveTableAsCSV
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.HttpClientUtils
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.UriType
import org.hl7.fhir.r4.model.ValueSet
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
class LookupTableEndpointUtilities(
    val environment: Environment,
    val useThisToken: String? = null,
    val httpClient: HttpClient? = null,
) {
    /**
     * Increase from the default read timeout in case of a super-duper long table.
     */
    private val requestTimeoutMillis = 130000

    /**
     * The Access Token.
     */
    private val accessToken = useThisToken ?: OktaCommand.fetchAccessToken(environment.oktaApp)
    ?: throw PrintMessage(
        "Missing access token. Run ./prime login to fetch/refresh your access token.", printError = true
    )

    /**
     * Fetches the list of tables from the API.
     * @return if [listInactive] is false then only a list of active tables is returned, otherwise all tables are listed
     * @throws IOException if there is a server or API error
     */
    fun fetchList(listInactive: Boolean = false): List<LookupTableVersion> {
        val (response, respStr) = HttpClientUtils.getWithStringResponse(
            url = environment.formUrl("$endpointRoot/list").toString(),
            accessToken = accessToken,
            timeout = requestTimeoutMillis.toLong(),
            queryParameters = mapOf(
                Pair(LookupTableFunctions.showInactiveParamName, listInactive.toString())
            ),
            httpClient = httpClient
        )

        return if (response.status == HttpStatusCode.OK) {
            try {
                mapper.readValue(respStr)
            } catch (e: MismatchedInputException) {
                // chain up the cause for details
                throw IOException(
                    "Invalid response body found," +
                        " response status: ${response.status.value}, " +
                        "body $respStr.",
                    e
                )
            }
        } else {
            throw IOException("Error response: response status: ${response.status.value}, body: $respStr")
        }
    }

    /**
     * Submits the activation of a table to the API given [tableName] and [version].
     * @return the table version information for the activated table
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun activateTable(tableName: String, version: Int): LookupTableVersion {
        val url = environment.formUrl("$endpointRoot/$tableName/$version/activate").toString()
        // seems need to destruct a pair by assignment
        val (response, respStr) = HttpClientUtils.putWithStringResponse(
            url = url,
            accessToken = accessToken,
            timeout = requestTimeoutMillis.toLong(),
            httpClient = httpClient
        )
        return getTableInfoResponse(response, respStr)
    }

    /**
     * Finds the active version of the specified [tableName].
     * @return the active version of the table
     * @throws TableNotFoundException if the table is not found
     * @throws IOException if there is a server or API error
     */
    fun findActiveVersion(tableName: String): Int {
        val tableList = try {
            this.fetchList()
        } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", printError = true)
        }
        val activeVersion = (tableList.firstOrNull { it.tableName == tableName })?.tableVersion ?: 0
        if (activeVersion == 0) {
            throw PrintMessage(
                "Could not find lookup table: $tableName", printError = true
            )
        }
        return activeVersion
    }

    /**
     * Fetches the table content from the API given [tableName] and [version].
     * @return the table content
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun fetchTableContent(tableName: String, version: Int): List<Map<String, String>> {
        val url = environment.formUrl("$endpointRoot/$tableName/$version/content").toString()
        val (response, respStr) = HttpClientUtils.getWithStringResponse(
            url = url,
            accessToken = accessToken,
            timeout = requestTimeoutMillis.toLong(),
            httpClient = httpClient
        )

        checkResponseForCommonErrors(response, respStr)

        try {
            return mapper.readValue(respStr)
        } catch (e: MismatchedInputException) {
            // chain up the root cause, here e.g. might have where the json parsing choked
            throw IOException(
                "Invalid response body found, " +
                    "response status: ${response.status.value}" +
                    ", body: $respStr",
                e
            )
        }
    }

    /**
     * Fetches the table version information from the API given [tableName] and [version].
     * @return the table version information
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun fetchTableInfo(tableName: String, version: Int): LookupTableVersion {
        val url = environment.formUrl("$endpointRoot/$tableName/$version/info").toString()
        val (response, respStr) = HttpClientUtils.getWithStringResponse(
            url = url,
            accessToken = accessToken,
            timeout = requestTimeoutMillis.toLong(),
            httpClient = httpClient
        )
        return getTableInfoResponse(response, respStr)
    }

    /**
     * Submit to the API a new table version using [tableName], [tableData], and [forceTableToCreate].
     * @return the table version information for the created table
     * @throws TableNotFoundException if the table and/or version is not found
     * @throws IOException if there is a server or API error
     */
    fun createTable(tableName: String, tableData: List<Map<String, String>>, forceTableToCreate: Boolean):
        LookupTableVersion {
        val url = environment
            .formUrl(
                "$endpointRoot/$tableName?table&forceTableToCreate=$forceTableToCreate"
            ).toString()
        val (response, respStr) =
            HttpClientUtils.postWithStringResponse(
                url = url,
                accessToken = accessToken,
                timeout = requestTimeoutMillis.toLong(),
                jsonPayload = mapper.writeValueAsString(tableData),
                httpClient = httpClient
            )
        return getTableInfoResponse(response, respStr)
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
         * Gets a table version information object from a [response] response body [respStr] returned by the API.
         * @return a table version information object
         * @throws TableNotFoundException if the table and/or version is not found
         * @throws IOException if there is a server or API error
         */
        internal fun getTableInfoResponse(response: HttpResponse, respStr: String): LookupTableVersion {
            checkResponseForCommonErrors(response, respStr)
            try {
                val info = mapper.readValue<LookupTableVersion>(respStr)
                if (info.tableName.isNullOrBlank() ||
                    info.tableVersion < 1 ||
                    info.createdBy.isNullOrBlank() ||
                    info.createdAt.toString().isBlank()
                ) {
                    throw IOException(
                        "Invalid version information in the response, " +
                            "response status: ${response.status.value}, body: $respStr, " +
                            "LookupTableVersion object: tableName: ${info.tableName}, " +
                            "tableVersion: ${info.tableVersion}, " +
                            "createdBy: ${info.createdBy}, " +
                            "createdAt: ${info.createdAt}."
                    )
                } else {
                    return info
                }
            } catch (e: MismatchedInputException) {
                // chain up the root cause
                throw IOException(
                    "Invalid JSON response, response status: ${response.status.value}" +
                        ", body: $respStr.",
                    e
                )
            }
        }

        /**
         * Check for an error response from a [response] and may be the response body [respStr] from the API.
         * @throws TableNotFoundException if the table and/or version is not found
         * @throws IOException if there is a server or API error
         */
        internal fun checkResponseForCommonErrors(response: HttpResponse, respStr: String) {
            when {
                // resource not found
                response.status == HttpStatusCode.NotFound -> {
                    val notFoundMsg = "Response status: ${response.status.value}, NOT FOUND"
                    try {
                        when {
                            // If we get a 404 with no response body then it is an endpoint not found error
                            respStr.isEmpty() -> throw IOException("$notFoundMsg, endpoint not found.")
                            // If we do get a 404 with a JSON error message then it is because the table was not found
                            mapper.readValue<Map<String, String>>(
                                respStr
                            ).containsKey("error") ->
                                throw TableNotFoundException("$notFoundMsg, Error message: $respStr")

                            else -> throw IOException("$notFoundMsg, Error message: $respStr")
                        }
                    } catch (e: MismatchedInputException) {
                        // The error message is not valid JSON.
                        throw IOException("$notFoundMsg, Error message: $respStr", e)
                    }
                }
                // resource conflict, create a resource that already there
                response.status == HttpStatusCode.Conflict ->
                    throw TableConflictException(respStr)

                response.status.value >= 300 ->
                    throw IOException("Response status: ${response.status.value}, response body: $respStr")
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
        internal fun setTableRowToJson(row: Map<String, String>): JSONB = JSONB.jsonb(mapper.writeValueAsString(row))
    }
}

/**
 * Commands to manipulate lookup tables.
 */
class LookupTableCommands :
    CliktCommand(
        name = "lookuptables",
    ) {
    override fun help(context: Context): String = "Manage lookup tables"

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
            addRowNum: Boolean = true,
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
            showAll: Boolean,
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
                        if (index < 2) {
                            diffBuffer.add(row.oldLine)
                        } else if (row.tag == DiffRow.Tag.EQUAL && changed) {
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
            } else {
                diffBuffer.clear()
            }
            return diffBuffer
        }
    }
}

/**
 * Generic lookup table command.
 * parameter [httpClient] - inject a custom http client
 */
abstract class GenericLookupTableCommand(name: String, val help: String, val httpClient: HttpClient? = null) :
    CliktCommand(name = name) {
    override fun help(context: Context): String = help

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
    val tableUtil get() = LookupTableEndpointUtilities(environment, httpClient = httpClient)
}

/**
 * Print out a lookup table.
 */
class LookupTableGetCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "get",
        help = "Fetch the contents of a lookup table",
        httpClient = httpClient
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
            throw PrintMessage("The table $tableName version $version was not found.", printError = true)
        } catch (e: IOException) {
            throw PrintMessage(
                "Error fetching the contents of table $tableName version $version: ${e.message}",
                printError = true
            )
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
                saveTableAsCSV(outputFile!!.outputStream(), tableList)
                echo(
                    "Saved ${tableList.size} rows of table $tableName version $version " +
                        "to ${outputFile!!.absolutePath} "
                )
            }
        } else {
            echo("Table $tableName version $version has no rows.")
        }
    }
}

/**
 * Compare a sender compendium with an observation mapping lookup table.
 */
class LookupTableCompareMappingCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "compare-mapping",
        help = "Compares a sender compendium against an observation mapping lookup table, outputting an annotated CSV",
        httpClient = httpClient
    ) {
    /**
     * The input file to get the table data from.
     */
    private val inputFile by option("-i", "--input-file", help = "Input CSV file with the sender compendium table data")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * Optional output file to save the annotated table to.
     */
    private val outputFile by option("-o", "--output-file", help = "Specify file to save annotated table data as CSV")
        .file(false, canBeDir = false)

    /**
     * Table name option.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the comparison on")
        .required()

    /**
     * Table version option.
     */
    private val tableVersion by option("-v", "--version", help = "The version of the table to get").int()

    companion object {
        private const val SENDER_COMPENDIUM_CODE_KEY = "test code"
        private const val SENDER_COMPENDIUM_CODESYSTEM_KEY = "coding system"
        private const val SENDER_COMPENDIUM_MAPPED_KEY = "mapped?"
        private const val SENDER_COMPENDIUM_MAPPED_TRUE = "Y"
        private const val SENDER_COMPENDIUM_MAPPED_FALSE = "N"

        /**
         * Annotates a sender [compendium] with a new column indicating whether the value is in the lookup table's
         * [tableTestCodeMap], a map grouping mappings by test code
         * @return a list of maps representing a sender compendium CSV with a new column: `mapped?`
         */
        fun compareMappings(
            compendium: List<Map<String, String>>,
            tableTestCodeMap: Map<String?, Map<String, String>>,
        ): List<Map<String, String>> = compendium.map {
            // process every code in the compendium
            if (tableTestCodeMap[it.getValue(SENDER_COMPENDIUM_CODE_KEY)]?.get(
                    ObservationMappingConstants.TEST_CODESYSTEM_KEY
                ) == it.getValue(SENDER_COMPENDIUM_CODESYSTEM_KEY)
            ) { // check for a matching code and code system i.e. mapped
                it + (SENDER_COMPENDIUM_MAPPED_KEY to SENDER_COMPENDIUM_MAPPED_TRUE)
            } else {
                it + (SENDER_COMPENDIUM_MAPPED_KEY to SENDER_COMPENDIUM_MAPPED_FALSE)
            }
        }
    }

    override fun run() {
        // Read the input file.
        val inputData = csvReader().readAllWithHeader(inputFile)

        // Check the supplied compendium
        if (inputData.isEmpty()) {
            throw PrintMessage("Input file ${inputFile.absolutePath} has no data.", printError = true)
        }
        arrayOf(SENDER_COMPENDIUM_CODE_KEY, SENDER_COMPENDIUM_CODESYSTEM_KEY).forEach {
            if (it !in inputData[0].keys) throw PrintMessage("Supplied compendium is missing column: $it")
        }

        val loadTableVersion: Int = tableVersion ?: tableUtil.findActiveVersion(tableName)

        // Verify the table/version exists
        try {
            tableUtil.fetchTableInfo(tableName, loadTableVersion)
        } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
            throw PrintMessage("The table $tableName version $loadTableVersion was not found.", printError = true)
        } catch (e: IOException) {
            throw PrintMessage(
                "Error fetching table version for $tableName version $loadTableVersion: ${e.message}",
                printError = true
            )
        }

        // Load the active or specified version of the table
        val tableData = try {
            tableUtil.fetchTableContent(tableName, loadTableVersion)
        } catch (e: Exception) {
            throw PrintMessage("Error fetching table content for table $tableName: ${e.message}", printError = true)
        }

        // Check loaded table for needed columns
        arrayOf(ObservationMappingConstants.TEST_CODE_KEY, ObservationMappingConstants.TEST_CODESYSTEM_KEY).forEach {
            if (it !in tableData[0].keys) throw PrintMessage("Loaded table $tableName missing column: $it")
        }

        // Create lookup table of codes
        val tableMap = tableData.associateBy { it[ObservationMappingConstants.TEST_CODE_KEY] }

        // Add a mapped? value to each row of table data
        val outputData = compareMappings(inputData, tableMap)

        // Save an output file and print the resulting table data
        if (outputFile != null) {
            saveTableAsCSV(outputFile!!.outputStream(), outputData)
            echo("Saved ${outputData.size} rows to ${outputFile!!.absolutePath}")
        }
        echo(LookupTableCommands.rowsToPrintableTable(outputData, outputData[0].keys.toList()))
    }
}

/**
 * Update an observation mapping table using the NLM Value Set Authority.
 */
class LookupTableUpdateMappingCommand :
    GenericLookupTableCommand(
        name = "update-mapping",
        help = "Update an observation mapping table using the NLM Value Set Authority."
    ) {
    /**
     * Optional output file to save the updated table to.
     */
    private val outputFile by option("-o", "--output-file", help = "Specify file to save updated table data as CSV")
        .file(false, canBeDir = false)

    /**
     * Table name option.
     */
    private val tableName by option("-n", "--name", help = "The name of the table to perform the update on")
        .required()

    /**
     * API key option.
     */
    private val apiKey by option("-k", "--api-key", help = "The authentication key used for the NMLS VSAC")
        .required()

    /**
     * OID of valueset to update.
     */
    private val oids by option("-d", "--oids", help = "Specify OIDs (comma delimited) to update (default: all OIDs)")

    /**
     * Activate a created table in one shot.
     */
    private val activate by option("-a", "--activate", help = "Activate the table upon creation")
        .flag(default = false)

    /**
     * Silent running.  No table contents or diff output or confirmation if true.
     */
    private val silent by option("-s", "--silent", help = "Do not generate diff or ask for confirmation").flag()

    /**
     * The input file to get the table data from.
     */
    private val inputFile by option(
        "-i", "--input-file",
        help = "Input CSV file with table data to be updated"
    ).file(true, canBeDir = false, mustBeReadable = true)

    /**
     * Table version option.
     */
    private val tableVersion by option("-v", "--version", help = "The version of the table to get").int()

    companion object {
        private const val OBX_MAPPING_CSV_PATH = "metadata/tables/local/observation-mapping.csv" // to update local csv
        private const val OBX_MAPPING_NO_OID_KEY = "NO_OID" // to group mappings without OIDs
        private const val OBX_MAPPING_NON_RCTC_KEY = "NON_RCTC" // to group non-RCTC mappings
        private const val OBX_MAPPING_RCTC_VALUE_SOURCE = "RCTC" // value source value representing RCTC mappings
        private const val TERMINOLOGY_SERVER_ENDPOINT = "https://cts.nlm.nih.gov/fhir"
        private val OBX_MAPPING_FILTER = listOf(OBX_MAPPING_NO_OID_KEY, OBX_MAPPING_NON_RCTC_KEY)

        /**
         * Builds a HTTP client for the NMLS VSAC using the provided [apiKey]
         * @return a ktor [HttpClient] for the NMLS VSAC with response status code validation
         */
        fun buildAPIClient(apiKey: String): IGenericClient =
            FhirContext.forR4().newRestfulGenericClient(TERMINOLOGY_SERVER_ENDPOINT).apply {
                this.registerInterceptor(BasicAuthInterceptor("apikey", apiKey))
            }

        /**
         * Looks up the ValueSet for a member [oid] using the supplied http [client] in the NIH National Library of
         * Medicine Value Set Authority.
         * @return a [ValueSet] for the supplied [oid]
         */
        fun fetchValueSetForOID(oid: String, client: IGenericClient): ValueSet {
            val outParams = client
                .operation()
                .onType(ValueSet::class.java)
                .named("expand")
                .withParameter(Parameters::class.java, "url", UriType(oid))
                .execute()

            return outParams.getParameter().get(0).resource as ValueSet
        }

        /**
         * Fetches latest test data for all [oids] supplied using the specified http [client]
         * @return updated test data grouped by OID, suitable for use with syncMappings()
         */
        fun fetchLatestTestData(oids: List<String>, client: IGenericClient): Map<String, ValueSet> =
            runBlocking {
                // wait
                // for each oid, fetch the ValueSet and create a test map
                oids.associateWith { oid ->
                    // create a map of oids to their associated tests in the valueset
                    async {
                        try {
                            fetchValueSetForOID(oid, client)
                        } catch (e: ResourceNotFoundException) {
                            throw PrintMessage("Could not find a ValueSet for oid '$oid'")
                        } catch (e: FhirClientConnectionException) {
                            throw PrintMessage("Could not connect to the VSAC service")
                        }
                    }
                }.mapValues { it.value.await() } // un-defer all values after starting coroutines
            }

        /**
         * Build a list of observation mappings from the [ValueSet] and [conditionData]
         * @return a list of observation mappings with condition data provided
         */
        fun ValueSet.toMappings(conditionData: Map<String, String> = emptyMap()): List<Map<String, String>> =
            this.expansion.contains.map { test ->
                mapOf(
                    ObservationMappingConstants.TEST_CODE_KEY to test.code,
                    ObservationMappingConstants.TEST_CODESYSTEM_KEY to // coerce to our values
                        ObservationMappingConstants.TEST_CODESYSTEM_MAP.getOrDefault(
                            test.system,
                            test.system
                        ),
                    ObservationMappingConstants.TEST_OID_KEY to this.idElement.idPart,
                    ObservationMappingConstants.TEST_NAME_KEY to this.name,
                    ObservationMappingConstants.TEST_DESCRIPTOR_KEY to test.display,
                    ObservationMappingConstants.TEST_STATUS_KEY to this.status.toString()
                        .lowercase().replaceFirstChar(Char::titlecase),
                    ObservationMappingConstants.TEST_VERSION_KEY to test.version.split('/').last()
                ) + conditionData
            }

        /**
         * Build a map of observation mappings keyed by member oid from an observation mapping [tableData]
         * @return a map of lists of mappings grouped by oids, with special entries NON_RCTC and NO_OID
         */
        fun buildOIDMap(tableData: List<Map<String, String>>) =
            tableData.groupBy {
                if (it[ObservationMappingConstants.CONDITION_VALUE_SOURCE_KEY] != OBX_MAPPING_RCTC_VALUE_SOURCE) {
                    OBX_MAPPING_NON_RCTC_KEY // not an RCTC mapping - cannot be updated
                } else {
                    it[ObservationMappingConstants.TEST_OID_KEY].let { oid ->
                        // group by OID
                        if (oid.isNullOrBlank()) OBX_MAPPING_NO_OID_KEY else oid // group mappings with blank/null OIDs
                    }
                }
            }

        /**
         * Generate a new observation mapping table with updated test data [updateOIDMap] using condition data from an
         * existing observation mappings [tableOIDMap]; both maps grouping mappings by member oid
         * @return updated observation mapping table as a list of maps
         */
        fun syncMappings(
            tableOIDMap: Map<String, List<Map<String, String>>>,
            updateOIDMap: Map<String, ValueSet>,
        ): List<Map<String, String>> = updateOIDMap.map { update ->
            // process every oid test group update
            val conditionData = tableOIDMap[update.key]!![0].filterKeys {
                it in ObservationMappingConstants.CONDITION_KEYS // fetch existing condition data for this oid
            }
            update.value.toMappings(conditionData)
            // flatten + add carryover
        }.flatten() + tableOIDMap.filterKeys { it !in updateOIDMap.keys }.values.flatten()

        /**
         * Load an [inputFile] or an existing lookup table by [tableName] and [tableVersion] using [tableUtil] and
         * validate it for an observation mapping update
         * @return the loaded table as a list of maps
         */
        fun loadAndValidateTableData(
            inputFile: File?,
            tableName: String,
            tableUtil: LookupTableEndpointUtilities,
            tableVersion: Int?,
        ): List<Map<String, String>> =
            if (inputFile != null) { // Start with data from a file
                val inputData = csvReader().readAllWithHeader(inputFile)
                inputData.ifEmpty {
                    throw PrintMessage("Input file ${inputFile.absolutePath} has no data.", printError = true)
                }
            } else { // Start with data from existing lookup table
                val loadTableVersion: Int = tableVersion ?: tableUtil.findActiveVersion(tableName)

                // Verify the table/version exists and load it
                try {
                    tableUtil.fetchTableInfo(tableName, loadTableVersion)
                    tableUtil.fetchTableContent(tableName, loadTableVersion)
                } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
                    throw PrintMessage(
                        "The table $tableName version $loadTableVersion was not found.", printError = true
                    )
                } catch (e: IOException) {
                    throw PrintMessage(
                        "Error fetching table version for $tableName version $loadTableVersion: ${e.message}",
                        printError = true
                    )
                } catch (e: Exception) {
                    throw PrintMessage(
                        "Error fetching table content for table $tableName: ${e.message}",
                        printError = true
                    )
                }
            }.also { tableData ->
                // Verify the loaded table contains the appropriate columns
                ObservationMappingConstants.ALL_KEYS.forEach {
                    if (it !in tableData[0].keys) throw PrintMessage("Loaded data is missing column: $it")
                }
            }
    }

    override fun run() {
        // Load data from either the input file or specified table
        val tableData = loadAndValidateTableData(inputFile, tableName, tableUtil, tableVersion)

        // Build an API client to query the NMLS VSAC
        val client = buildAPIClient(apiKey)

        // Create lookup table of codes
        val tableOIDMap = buildOIDMap(tableData)

        // Fetch the update data
        val updateData = fetchLatestTestData(
            oids?.split(',') ?: tableOIDMap.keys.filter { it !in OBX_MAPPING_FILTER }, // use oid list if provided
            client
        )

        // Sync the update data with current data
        val outputData = syncMappings(tableOIDMap, updateData)

        // Save an output file if specified
        if (outputFile != null) {
            saveTableAsCSV(outputFile!!.outputStream(), outputData)
            echo("Saved ${outputData.size} rows to ${outputFile!!.absolutePath}")
        }

        // Save local csv of updated table
        if ((
                !silent &&
                    YesNoPrompt(
                        "Save an updated local observation-mapping.csv with ${outputData.size} rows?",
                        currentContext.terminal
                    ).ask() == true
                ) ||
            silent
        ) {
            val outputCSV = File(OBX_MAPPING_CSV_PATH)
            saveTableAsCSV(outputCSV.outputStream(), outputData)
            echo("Saved ${outputData.size} rows to ${outputCSV.absolutePath} ")
        }

        // Save updated table to the database
        if ((
                !silent &&
                    YesNoPrompt(
                        "Continue to create a new version of $tableName with ${outputData.size} rows?",
                        currentContext.terminal
                    ).ask() == true
                ) ||
            silent
        ) {
            val newTableInfo = try {
                tableUtil.createTable(tableName, outputData, true)
            } catch (e: IOException) {
                throw PrintMessage("\tError creating new table version for $tableName: ${e.message}", printError = true)
            } catch (e: LookupTableEndpointUtilities.Companion.TableConflictException) {
                val dupVersion = e.message?.substringAfterLast("version")
                echo(
                    "Skipping creation of duplicate table $tableName since it is duplicated with version$dupVersion."
                )
                return
            }

            // Always have an active version, so if this is the first version then activate it.
            if (activate || newTableInfo.tableVersion == 1) {
                try {
                    tableUtil.activateTable(tableName, newTableInfo.tableVersion)
                } catch (e: Exception) {
                    throw PrintMessage(
                        "\tError activating table $tableName version ${newTableInfo.tableVersion}. " +
                            "Table was created.  Try to activate it. : ${e.message}",
                        printError = true
                    )
                }
                echo("\tTable version ${newTableInfo.tableVersion} is now active.")
            } else {
                echo(
                    "\tTable version ${newTableInfo.tableVersion} " +
                        "left inactive, so don't forget to activate it."
                )
            }
        }
    }
}

/**
 * Create a new lookup table.
 */
class LookupTableCreateCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "create",
        help = "Create a new version of a lookup table",
        httpClient = httpClient
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
            throw PrintMessage("Error fetching the list of tables: ${e.message}", printError = true)
        }
        val activeVersion = (tableList.firstOrNull { it.tableName == tableName })?.tableVersion ?: 0
        if (!silent && activeVersion > 0) {
            val activeTable = try {
                tableUtil.fetchTableContent(tableName, activeVersion)
            } catch (e: Exception) {
                throw PrintMessage(
                    "Error fetching active table content for table $tableName: ${e.message}", printError = true
                )
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
                !silent &&
                    YesNoPrompt(
                        "Continue to create a new version of $tableName with ${inputData.size} rows?",
                        currentContext.terminal
                    ).ask() == true
                ) ||
            silent
        ) {
            val newTableInfo = try {
                tableUtil.createTable(tableName, inputData, forceTableToCreate)
            } catch (e: IOException) {
                throw PrintMessage("\tError creating new table version for $tableName: ${e.message}", printError = true)
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
                try {
                    tableUtil.activateTable(tableName, newTableInfo.tableVersion)
                } catch (e: Exception) {
                    throw PrintMessage(
                        "\tError activating table $tableName version ${newTableInfo.tableVersion}. " +
                            "Table was created.  Try to activate it. : ${e.message}",
                        printError = true
                    )
                }
                echo("\tTable version ${newTableInfo.tableVersion} is now active.")
            } else {
                echo(
                    "\tTable version ${newTableInfo.tableVersion} " +
                        "left inactive, so don't forget to activate it."
                )
            }
        } else {
            echo("\tAborted the creation of the lookup table.")
        }
    }
}

/**
 * List the available lookup tables.
 */
class LookupTableListCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "list",
        help = "List the lookup tables",
        httpClient = httpClient
    ) {
    /**
     * List all the tables including inactive ones if set.
     */
    private val showInactive by option("-a", "--all", help = "List all active and inactive tables")
        .flag(default = false)

    override fun run() {
        val data = try {
            tableUtil.fetchList(showInactive)
        } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", printError = true)
        }
        if (showInactive) {
            echo("Listing all lookup tables including inactive.")
        } else {
            echo("Listing only active lookup tables.")
        }

        if (data.isNotEmpty()) {
            echo(
                LookupTableCommands
                    .infoToPrintableTable(data)
            )
            echo("")
        } else {
            if (!showInactive) {
                echo("No lookup tables were found.")
            } else {
                echo("No active lookup tables were found.")
            }
        }
    }
}

/**
 * Show a diff between two lookup tables.
 */
class LookupTableDiffCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "diff",
        help = "Generate a difference between two versions of a lookup table",
        httpClient = httpClient
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
            throw PrintMessage("The table $tableName version $version1 was not found.", printError = true)
        } catch (e: IOException) {
            throw PrintMessage(
                "Error fetching table version for $tableName version $version1: ${e.message}", printError = true
            )
        }
        val version2Info = try {
            tableUtil.fetchTableInfo(tableName, version2)
        } catch (e: LookupTableEndpointUtilities.Companion.TableNotFoundException) {
            throw PrintMessage("The table $tableName version $version2 was not found.", printError = true)
        } catch (e: IOException) {
            throw PrintMessage(
                "Error fetching table version for $tableName version $version2: ${e.message}", printError = true
            )
        }

        // Now get the content.
        val version1Table = try {
            tableUtil.fetchTableContent(tableName, version1)
        } catch (e: Exception) {
            throw PrintMessage(
                "Error fetching table content for $tableName version $version1: ${e.message}", printError = true
            )
        }
        val version2Table = try {
            tableUtil.fetchTableContent(tableName, version2)
        } catch (e: Exception) {
            throw PrintMessage(
                "Error fetching table content for $tableName version $version2: ${e.message}", printError = true
            )
        }

        // Generate the diff.
        echo("Comparing lookup table $tableName versions $version1 and $version2:")
        echo(LookupTableCommands.infoToPrintableTable(listOf(version1Info, version2Info)))
        echo("")

        val output = LookupTableCommands.generateDiff(version1Table, version2Table, fullDiff)

        if (output.isNotEmpty()) {
            output.forEach { echo(it) }
        } else {
            echo("Lookup table $tableName version $version1 and $version2 are identical.")
        }
    }
}

/**
 * Activate a lookup table.
 */
class LookupTableActivateCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "activate",
        help = "Activate a specific version of a lookup table",
        httpClient = httpClient
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
            throw PrintMessage("The table $tableName version $version was not found.", printError = true)
        } catch (e: IOException) {
            throw PrintMessage(
                "Error fetching table version for $tableName version $version: ${e.message}", printError = true
            )
        }

        // Find the currently active version
        val tableList = try {
            tableUtil.fetchList(true)
        } catch (e: IOException) {
            throw PrintMessage("Error fetching the list of tables: ${e.message}", printError = true)
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

        if (YesNoPrompt("Set $version as active?", currentContext.terminal).ask() == true) {
            val activatedStatus = tableUtil.activateTable(tableName, version)
            if (activatedStatus.isActive) {
                echo("Version $version for lookup table $tableName was set active.")
            } else {
                error("Unknown error when setting lookup table $tableName Version $version to active.")
            }
        } else {
            echo("Aborted the activation of the lookup table.")
        }
    }
}

/**
 * Load lookup tables from a directory.
 */
class LookupTableLoadAllCommand(httpClient: HttpClient? = null) :
    GenericLookupTableCommand(
        name = "loadall",
        help = "Load all the tables stored as CSV in the specified directory",
        httpClient = httpClient
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
    private val tableCreator = LookupTableCreateCommand(httpClient)

    override fun run() {
        if (environment != Environment.LOCAL) error("This command is only allowed in the local environment.")

        // First wait for the API to come online
        echo("Waiting for the API at ${environment.url} to be available...")
        CommandUtilities.waitForApi(environment, connRetries)

        // Get the list of current tables to only update or create new ones.
        val tableUpdateTimes = if (checkLastModified) {
            LookupTableEndpointUtilities(environment).fetchList().map {
                it.tableName to it.createdAt
            }.toMap()
        } else {
            emptyMap()
        }

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