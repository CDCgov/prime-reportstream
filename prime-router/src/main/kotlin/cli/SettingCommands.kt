package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.common.net.HttpHeaders
import de.m3y.kformat.Table
import de.m3y.kformat.table
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.apache.http.HttpStatus
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private const val apiPath = "/api/settings"
private const val dummyAccessToken = "dummy"
private const val jsonMimeType = "application/json"
private const val organizationsFile = "settings/organizations.yml"

/**
 * Base class to handle common stuff: authentication, calling, inputs and outputs.
 * It has a composable set of methods for listing, getting, diffing, putting and deleting settings.
 * The idea is to make concrete commands clear and concise by building primitives in this class.
 */
abstract class SettingCommand(
    name: String,
    help: String,
) : CliktCommand(name = name, help = help) {
    internal val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment.\nChoose between [local|test|staging|prod]"
    )
        .choice("local", "test", "staging", "prod")
        .default("local", "local environment")

    protected val outStream by option(
        "-o", "--output",
        help = "Output to file",
        metavar = "<file>"
    ).outputStream(createIfNotExist = true, truncateExisting = true).default(System.out)

    protected val verbose by option(
        "-v", "--verbose",
        help = "Verbose logging of each HTTP operation to console"
    ).flag(default = false)

    protected val silent by option(
        "-s", "--silent",
        help = "Do not echo progress or prompt for confirmation"
    ).flag(default = false)

    protected val inputOption = option(
        "-i", "--input",
        help = "Input from file",
        metavar = "<file>"
    ).file(mustBeReadable = true).required()

    protected val nameOption = option(
        "-n", "--name",
        metavar = "<name>",
        help = "The full name of the setting"
    ).required()

    protected val jsonOption = option(
        "--json",
        help = "Use the JSON format instead of YAML"
    ).flag(default = false)

    enum class Operation { LIST, GET, PUT, DELETE }
    enum class SettingType { ORGANIZATION, SENDER, RECEIVER }

    val jsonMapper = JacksonMapperUtilities.allowUnknownsMapper
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )

    init {
        yamlMapper.registerModule(JavaTimeModule())
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * The environment specified by the command line parameters
     */
    val environment: Environment by lazy {
        Environment.get(env)
    }

    /**
     * The access token left by a previous login command as specified by the command line parameters
     */
    val oktaAccessToken: String by lazy {
        if (environment.oktaApp == null) {
            dummyAccessToken
        } else {
            OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: abort(
                    "Invalid access token. " +
                        "Run ./prime login to fetch/refresh your access token for the $env environment."
                )
        }
    }

    /**
     * Put entity for [settingName]. [settingType] is needed for serialization
     */
    fun put(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String,
        payload: String
    ): String {
        val path = formPath(environment, Operation.PUT, settingType, settingName)
        verbose("PUT $path :: $payload")
        val output = SettingsUtilities.put(path, accessToken, payload)
        val (_, response, result) = output
        return when (result) {
            is Result.Failure -> handleHttpFailure(settingName, response, result)
            is Result.Success ->
                when (response.statusCode) {
                    HttpStatus.SC_OK -> {
                        // need to account for an older version of the API PUT method which only returned the "meta"
                        // object- whereas now we're returning the full JSON response
                        val version = if (result.value.obj().has("version"))
                            result.value.obj().getInt("version")
                        else
                            "[unknown - legacy data]"
                        "Success. Setting $settingName at version $version"
                    }
                    HttpStatus.SC_CREATED -> "Success. Created $settingName\n"
                    else -> error("Unexpected successful status code")
                }
        }
    }

    /**
     * Delete entity for [settingName]. [settingType] is needed for serialization
     */
    fun delete(environment: Environment, accessToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.DELETE, settingType, settingName)
        verbose("DELETE $path")
        val (_, response, result) = SettingsUtilities.delete(path, accessToken)
        return when (result) {
            is Result.Failure ->
                abort("Error on delete of $settingName: ${response.responseMessage} ${String(response.data)}")
            is Result.Success ->
                "Success $settingName: ${result.value}"
        }
    }

    /**
     * Get entity for [settingName]. [settingType] is needed for serialization
     */
    fun get(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String,
        abortOnError: Boolean = true
    ): String {
        val path = formPath(environment, Operation.GET, settingType, settingName)
        verbose("GET $path")
        val (_, response, result) = SettingsUtilities.get(path, accessToken)
        return when (result) {
            is Result.Failure -> {
                if (abortOnError)
                    abort(
                        "Error getting $settingName in the $env environment:" +
                            " ${response.responseMessage} ${String(response.data)}"
                    )
                else
                    ""
            }
            is Result.Success -> result.value
        }
    }

    /**
     * Get entities for [settingName]. [settingType] is needed for serialization
     */
    fun getMany(environment: Environment, accessToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.LIST, settingType, settingName)
        verbose("GET $path")
        val (_, response, result) = Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .timeoutRead(SettingsUtilities.requestTimeoutMillis)
            .responseJson()
        return when (result) {
            is Result.Failure -> handleHttpFailure(settingName, response, result)
            is Result.Success -> "[${result.value.array().join(",\n")}]"
        }
    }

    /**
     * List entities for [settingName]. [settingType] is needed for serialization
     */
    fun listNames(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String
    ): List<String> {
        val path = formPath(environment, Operation.LIST, settingType, settingName)
        verbose("GET $path")
        val (_, response, result) = Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .timeoutRead(SettingsUtilities.requestTimeoutMillis)
            .responseJson()
        return when (result) {
            is Result.Failure -> handleHttpFailure(settingName, response, result)
            is Result.Success -> {
                val resultObjs = result.value.array()
                val names = if (settingType == SettingType.ORGANIZATION) {
                    (0 until resultObjs.length())
                        .map { resultObjs.getJSONObject(it) }
                        .map { it.getString("name") }
                } else {
                    (0 until resultObjs.length())
                        .map { resultObjs.getJSONObject(it) }
                        .map { "${it.getString("organizationName")}.${it.getString("name")}" }
                }
                names.sorted()
            }
        }
    }

    // Class to capture the differences for particular setting object
    data class SettingsDiff(
        val settingType: SettingType,
        val settingName: String,
        val differences: List<CommandUtilities.Companion.DiffRow>
    )

    /**
     * Calculate the difference of [settingName] in [environment] to [payload].
     * [settingType] is needed for serialization. Returns a list of differences or an empty list for no differences.
     */
    fun diff(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String,
        payload: String
    ): List<SettingsDiff> {
        val base = get(environment, accessToken, settingType, settingName, abortOnError = false)
        val diffList = CommandUtilities.diffJson(base, payload)
            .filter { !it.name.startsWith("meta") } // Remove the meta differences
        return if (diffList.isNotEmpty())
            listOf(SettingsDiff(settingType, settingName, diffList))
        else
            emptyList()
    }

    /**
     * Difference the YAML [inputFile]. Returns a list of all settings with differences.
     */
    protected fun diffAll(inputFile: File): List<SettingsDiff> {
        return diffAll(readYaml(inputFile))
    }

    /**
     * Difference a list of organization settings [deepOrganizations] against a specified environment, or [environment]
     * as a default. If [env] is provided, a corresponding [accessToken] should also be provided.
     */
    protected fun diffAll(
        deepOrganizations: List<DeepOrganization>,
        env: Environment = environment,
        accessToken: String = oktaAccessToken
    ): List<SettingsDiff> {
        val settingsDiff = mutableListOf<SettingsDiff>()
        // diff organizations
        deepOrganizations.forEach { deepOrg ->
            val org = Organization(deepOrg)
            val payload = jsonMapper.writeValueAsString(org)
            settingsDiff += diff(env, accessToken, SettingType.ORGANIZATION, deepOrg.name, payload)
        }
        // diff senders
        deepOrganizations.flatMap { it.senders }.forEach { sender ->
            val payload = jsonMapper.writeValueAsString(sender)
            settingsDiff += diff(env, accessToken, SettingType.SENDER, sender.fullName, payload)
        }
        // diff receivers
        deepOrganizations.flatMap { it.receivers }.forEach { receiver ->
            val payload = jsonMapper.writeValueAsString(receiver)
            settingsDiff += diff(env, accessToken, SettingType.RECEIVER, receiver.fullName, payload)
        }
        return settingsDiff.sortedWith(compareBy({ it.settingType.name }, { it.settingName }))
    }

    /**
     * Echo the setting differences list in a single pretty table.
     */
    protected fun echoDiff(settingsDiff: List<SettingsDiff>) {
        if (settingsDiff.isEmpty()) return
        val renderedTable = table {
            hints {
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
            header("entity type", "entity name", "setting name", "environment value", "file value")
            settingsDiff.forEach { diff ->
                diff.differences.forEach { itemDiff ->
                    row(
                        diff.settingType.name.lowercase(),
                        diff.settingName,
                        itemDiff.name,
                        itemDiff.baseValue,
                        itemDiff.toValue
                    )
                }
            }
        }.render().toString()
        echo(renderedTable)
    }

    /**
     * Call [put] on all the settings in the [inputFile]. Return the list of results.
     */
    protected fun putAll(inputFile: File): List<String> {
        return putAll(readYaml(inputFile))
    }

    /**
     * Call [put] on a list of organization settings, [deepOrganizations] in a specified environment [env], which
     * defaults to [environment] if not specified. If [env] is provided, a corresponding [accessToken] should also be
     * provided.
     */
    protected fun putAll(
        deepOrganizations: List<DeepOrganization>,
        env: Environment = environment,
        accessToken: String = oktaAccessToken
    ): List<String> {
        val results = mutableListOf<String>()
        // Put organizations
        deepOrganizations.forEach { deepOrg ->
            val org = Organization(deepOrg)
            val payload = jsonMapper.writeValueAsString(org)
            results += put(env, accessToken, SettingType.ORGANIZATION, deepOrg.name, payload)
        }
        // Put senders
        deepOrganizations.flatMap { it.senders }.forEach { sender ->
            val payload = jsonMapper.writeValueAsString(sender)
            results += put(env, accessToken, SettingType.SENDER, sender.fullName, payload)
        }
        // Put receivers
        deepOrganizations.flatMap { it.receivers }.forEach { receiver ->
            val payload = jsonMapper.writeValueAsString(receiver)
            results += put(env, accessToken, SettingType.RECEIVER, receiver.fullName, payload)
        }
        return results
    }

    private fun readYaml(inputFile: File): List<DeepOrganization> {
        val input = readInput(inputFile)
        return yamlMapper.readValue(input)
    }

    fun readInput(inputFile: File): String {
        val input = String(inputFile.readBytes())
        if (input.isBlank()) abort("Blank input")
        return input
    }

    fun writeOutput(output: String) {
        outStream.write(output.toByteArray())
    }

    private fun handleHttpFailure(settingName: String, response: Response, result: Result<FuelJson, FuelError>):
        Nothing {
        abort(
            "Error: \n" +
                "  Setting Name: $settingName\n" +
                "  HTTP Result: ${result.component2()?.message}\n" +
                "  HTTP Response Message: ${response.responseMessage}\n" +
                "  HTTP Response Data: ${String(response.data)}"
        )
    }

    fun fromJson(input: String, settingType: SettingType): Pair<String, String> {
        return readStructure(input, settingType, jsonMapper)
    }

    fun fromYaml(input: String, settingType: SettingType): Pair<String, String> {
        return readStructure(input, settingType, yamlMapper)
    }

    private fun readStructure(input: String, settingType: SettingType, mapper: ObjectMapper): Pair<String, String> {
        return when (settingType) {
            SettingType.ORGANIZATION -> {
                val organization = mapper.readValue(input, OrganizationAPI::class.java)
                Pair(organization.name, jsonMapper.writeValueAsString(organization))
            }
            SettingType.SENDER -> {
                val sender = mapper.readValue(input, Sender::class.java)
                Pair(sender.fullName, jsonMapper.writeValueAsString(sender))
            }
            SettingType.RECEIVER -> {
                val receiver = mapper.readValue(input, ReceiverAPI::class.java)
                Pair(receiver.fullName, jsonMapper.writeValueAsString(receiver))
            }
        }
    }

    fun toYaml(output: String, settingType: SettingType): String {
        // DevNote: could be handled by inherited methods, but decided that keeping all these together was maintainable
        return when (settingType) {
            SettingType.ORGANIZATION -> {
                val organization = jsonMapper.readValue(output, OrganizationAPI::class.java)
                yamlMapper.writeValueAsString(organization)
            }
            SettingType.SENDER -> {
                val sender = jsonMapper.readValue(output, Sender::class.java)
                return yamlMapper.writeValueAsString(sender)
            }
            SettingType.RECEIVER -> {
                val receiver = jsonMapper.readValue(output, ReceiverAPI::class.java)
                return yamlMapper.writeValueAsString(receiver)
            }
        }
    }

    /**
     * Echo information to the console respecting the --silent flag
     */
    fun echo(message: String) {
        // clickt moved the echo command into the CliktCommand class, which means this needs to call
        // into the parent class, but Kotlin doesn't allow calls to super with default parameters
        if (!silent) super.echo(
            message,
            trailingNewline = true,
            err = false,
            currentContext.console.lineSeparator
        )
    }

    /**
     * Echo verbose information to the console respecting the --silent and --verbose flag
     */
    fun verbose(message: String) {
        try {
            if (verbose) echo(message)
        } catch (e: IllegalStateException) {
            // ignore this error that can occur if directly calling SettingsCommands (e.g. put) rather than from cmdline
        }
    }

    /**
     * Abort the program with the message
     */
    fun abort(message: String): Nothing {
        try {
            if (silent)
                throw ProgramResult(statusCode = 1)
            else
                throw PrintMessage(message, error = true)
        } catch (e: IllegalStateException) {
            // The if (silent) test can cause this exception if directly calling SettingsCommands, and not from cmdline.
            throw PrintMessage(message, error = true)
        }
    }

    /**
     * Confirm to continue or abort, if not in --silent mode. Display the [abortMessage] if exiting.
     */
    fun confirm(message: String = "Perform the above changes", abortMessage: String = "No change applied") {
        // Clikt moved the TermUI library internal, and exposed methods on CliktCommand instead, so we
        // can call super to get the same functionality, BUT calls to super in Kotlin don't allow you
        // to use default parameter values, so we have to explicitly define them here
        if (!silent && super.confirm(
                message,
                default = false,
                abort = false,
                promptSuffix = ": ",
                showDefault = true
            ) == false
        ) {
            abort(abortMessage)
        }
    }

    /**
     * Confirm that the api is available and that the CLI can access it. Abort if not available.
     */
    fun checkApi(environment: Environment) {
        if (!CommandUtilities.isApiAvailable(environment))
            abort("The $env environment's API is not available or you have an invalid access token.")
    }

    companion object {
        fun formPath(
            environment: Environment,
            operation: Operation,
            settingType: SettingType,
            settingName: String
        ): String {
            return environment.formUrl("$apiPath${settingPath(operation, settingType, settingName)}").toString()
        }

        private fun settingPath(operation: Operation, settingType: SettingType, settingName: String): String {
            return if (operation == Operation.LIST) {
                when (settingType) {
                    SettingType.ORGANIZATION -> "/organizations"
                    SettingType.SENDER -> "/organizations/$settingName/senders"
                    SettingType.RECEIVER -> "/organizations/$settingName/receivers"
                }
            } else {
                when (settingType) {
                    SettingType.ORGANIZATION -> "/organizations/$settingName"
                    SettingType.SENDER -> {
                        val (orgName, senderName) = Sender.parseFullName(settingName)
                        "/organizations/$orgName/senders/$senderName"
                    }
                    SettingType.RECEIVER -> {
                        val (orgName, receiverName) = Receiver.parseFullName(settingName)
                        "/organizations/$orgName/receivers/$receiverName"
                    }
                }
            }
        }
    }
}

/**
 * List a single object entity
 */
abstract class ListSettingCommand(
    name: String,
    help: String,
    val settingType: SettingType,
) : SettingCommand(name, help) {
    private val settingName: String by nameOption

    override fun run() {
        if (settingType != SettingType.ORGANIZATION && settingName.isBlank())
            abort("Missing organization name argument")
        checkApi(environment)
        val output = listNames(environment, oktaAccessToken, settingType, settingName)
        writeOutput(output.joinToString("\n"))
    }
}

/**
 * Get a single object entity
 */
abstract class GetSettingCommand(
    name: String,
    help: String,
    val settingType: SettingType
) : SettingCommand(name, help) {
    private val settingName: String by nameOption
    private val useJson: Boolean by jsonOption

    override fun run() {
        checkApi(environment)
        val output = get(environment, oktaAccessToken, settingType, settingName)
        if (useJson) writeOutput(output) else writeOutput(toYaml(output, settingType))
    }
}

/**
 * Remove a single entity
 */
abstract class DeleteSettingCommand(
    name: String,
    help: String,
    val settingType: SettingType
) : SettingCommand(name, help) {
    private val settingName: String by nameOption

    override fun run() {
        checkApi(environment)
        delete(environment, oktaAccessToken, settingType, settingName)
        writeOutput("Success. Removed $settingName\n")
    }
}

/**
 * Put an entity command with an input file
 */
abstract class PutSettingCommand(
    name: String,
    help: String,
    val settingType: SettingType
) : SettingCommand(name, help) {
    private val inputFile by inputOption
    private val useJson: Boolean by jsonOption

    override fun run() {
        checkApi(environment)
        val (name, payload) = if (useJson)
            fromJson(readInput(inputFile), settingType)
        else
            fromYaml(readInput(inputFile), settingType)
        if (!silent) {
            val differences = diff(environment, oktaAccessToken, settingType, name, payload)
            if (differences.isNotEmpty()) {
                echoDiff(differences)
                confirm()
            }
        }
        val output = put(environment, oktaAccessToken, settingType, name, payload)
        writeOutput(output)
    }
}

/**
 * Diff an entity command with an input file
 */
abstract class DiffSettingCommand(
    name: String,
    help: String,
    val settingType: SettingType
) : SettingCommand(name, help) {
    private val inputFile by inputOption
    private val useJson: Boolean by jsonOption

    override fun run() {
        checkApi(environment)
        val (name, payload) = if (useJson)
            fromJson(readInput(inputFile), settingType)
        else
            fromYaml(readInput(inputFile), settingType)
        val differences = diff(environment, oktaAccessToken, settingType, name, payload)
        if (differences.isNotEmpty())
            echoDiff(differences)
        else
            echo("No differences")
    }
}

/**
 * Organization setting commands
 */
class OrganizationSettings : CliktCommand(
    name = "organization",
    help = "Fetch and update settings for an organization"
) {
    init {
        subcommands(
            ListOrganizationSetting(),
            GetOrganizationSetting(),
            PutOrganizationSetting(),
            DeleteOrganizationSetting(),
            DiffOrganizationSetting(),
            TokenUrl(),
            AddPublicKey(),
            RemoveKey()
        )
    }

    override fun run() {
        // Does not run at this level
    }
}

class ListOrganizationSetting : SettingCommand(
    name = "list",
    help = "List the setting names of all organizations"
) {
    override fun run() {
        val output = listNames(environment, oktaAccessToken, SettingType.ORGANIZATION, "")
        writeOutput(output.joinToString("\n"))
    }
}

class GetOrganizationSetting : GetSettingCommand(
    name = "get",
    help = "Fetch an organization's settings",
    settingType = SettingType.ORGANIZATION,
)

class PutOrganizationSetting : PutSettingCommand(
    name = "set",
    help = "Update an organization's settings",
    settingType = SettingType.ORGANIZATION
)

class DeleteOrganizationSetting : DeleteSettingCommand(
    name = "remove",
    help = "Remove an organization",
    settingType = SettingType.ORGANIZATION,
)

class DiffOrganizationSetting : DiffSettingCommand(
    name = "diff",
    help = "Compare an organization's setting from an environment to those in an file",
    settingType = SettingType.ORGANIZATION
)

/**
 * Sender setting commands
 */
class SenderSettings : CliktCommand(
    name = "sender",
    help = "Fetch and update settings for a sender"
) {
    init {
        subcommands(
            ListSenderSetting(),
            GetSenderSetting(),
            PutSenderSetting(),
            DeleteSenderSetting(),
            DiffSenderSetting(),
        )
    }

    override fun run() {
        // Does not run at this level
    }
}

class ListSenderSetting : ListSettingCommand(
    name = "list",
    help = "List all sender names for an organization",
    settingType = SettingType.SENDER,
)

class GetSenderSetting : GetSettingCommand(
    name = "get",
    help = "Fetch a sender's settings",
    settingType = SettingType.SENDER,
)

class PutSenderSetting : PutSettingCommand(
    name = "set",
    help = "Update a sender's settings",
    settingType = SettingType.SENDER,
)

class DeleteSenderSetting : DeleteSettingCommand(
    name = "remove",
    help = "Remove a sender",
    settingType = SettingType.SENDER,
)

class DiffSenderSetting : DiffSettingCommand(
    name = "diff",
    help = "Compare sender's settings from an environment to those in a file",
    settingType = SettingType.SENDER,
)

/**
 * Receiver setting commands
 */
class ReceiverSettings : CliktCommand(
    name = "receiver",
    help = "Fetch and update settings for a receiver"
) {
    init {
        subcommands(
            ListReceiverSetting(),
            GetReceiverSetting(),
            PutReceiverSetting(),
            DeleteReceiverSetting(),
            DiffReceiverSetting(),
        )
    }

    override fun run() {
        // Does not run at this level
    }
}

class ListReceiverSetting : ListSettingCommand(
    name = "list",
    help = "Fetch the receiver names for an organization",
    settingType = SettingType.RECEIVER,
)

class GetReceiverSetting : GetSettingCommand(
    name = "get",
    help = "Fetch a receiver's settings",
    settingType = SettingType.RECEIVER,
)

class PutReceiverSetting : PutSettingCommand(
    name = "set",
    help = "Update a receiver's settings",
    settingType = SettingType.RECEIVER,
)

class DeleteReceiverSetting : DeleteSettingCommand(
    name = "remove",
    help = "Remove a receiver",
    settingType = SettingType.RECEIVER,
)

class DiffReceiverSetting : DiffSettingCommand(
    name = "diff",
    help = "Compare a receiver's settings from an environment to those in a file",
    settingType = SettingType.RECEIVER,
)

/**
 * Update multiple settings
 */
class MultipleSettings : CliktCommand(
    name = "multiple-settings",
    help = "Fetch and update multiple settings"
) {
    init {
        subcommands(PutMultipleSettings(), GetMultipleSettings(), DiffMultipleSettings())
    }

    override fun run() {
        // Does not run at this level
    }
}

class PutMultipleSettings : SettingCommand(
    name = "set",
    help = "Set all settings from a 'organizations.yml' file"
) {
    private val inputFile by inputOption

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

    override fun run() {
        // First wait for the API to come online
        echo("Waiting for the API at ${environment.url} to be available...")
        CommandUtilities.waitForApi(environment, connRetries)

        if (!checkLastModified || (checkLastModified && isFileUpdated())) {
            echo("Loading settings from ${inputFile.absolutePath}...")
            if (!silent) {
                val differences = diffAll(inputFile)
                if (differences.isNotEmpty()) {
                    echoDiff(differences)
                    confirm()
                }
            }
            val results = putAll(inputFile)
            val output = "${results.joinToString("\n")}\n"
            writeOutput(output)
        } else {
            echo("No new updates found for settings.")
        }
    }

    /**
     * Check if the settings from a file are newer than the data stored in the database for commands environment.
     * @return true if the file settings are newer or there is nothing in the database, false otherwise
     */
    private fun isFileUpdated(): Boolean {
        val url = formPath(environment, Operation.LIST, SettingType.ORGANIZATION, "")
        val (_, response, result) = Fuel.head(url).authentication()
            .bearer(oktaAccessToken)
            .timeoutRead(SettingsUtilities.requestTimeoutMillis)
            .response()
        return when (result) {
            is Result.Success -> {
                if (response[HttpHeaders.LAST_MODIFIED].isNotEmpty()) {
                    try {
                        val apiModifiedTime = OffsetDateTime.parse(
                            response[HttpHeaders.LAST_MODIFIED].first(),
                            HttpUtilities.lastModifiedFormatter
                        )
                        apiModifiedTime.toInstant().toEpochMilli() < inputFile.lastModified()
                    } catch (e: DateTimeParseException) {
                        error("Unable to decode last modified data from API call. $e")
                    }
                } else true // We have no last modified time, which means the DB is empty
            }
            else -> error("Unable to fetch settings last update time from API.  $result")
        }
    }
}

class DiffMultipleSettings : SettingCommand(
    name = "diff",
    help = "Compare all the settings from an environment to those in a file"
) {
    private val inputFile by inputOption

    override fun run() {
        checkApi(environment)
        echo("Loading settings from ${inputFile.absolutePath} to compare...")
        val differences = diffAll(inputFile)
        if (differences.isNotEmpty()) {
            echoDiff(differences)
        } else {
            echo("No differences")
        }
    }
}

class GetMultipleSettings : SettingCommand(
    name = "get",
    help = "Get all settings from an environment in yaml format"
) {
    val filter by option(
        "-f", "--filter",
        help = "filter the organizations, only returning those with names that start with <filter>",
        metavar = "<filter>"
    )

    private val loadToLocal by option(
        "-l", "--load-to-local",
        help = "Load settings to local database with transport modified to use SFTP. " +
            "You will have the chance to approve or decline a diff. " +
            "If the -a (--append-to-orgs) option is used in conjunction with the load option, the modified results " +
            "are used when appending to the organizations.yml file. If the -o (--output) option is used, the " +
            "original, unmodified settings will be output to that file."
    ).flag(default = false)

    private val appendToOrgs by option(
        "-a", "--append-to-orgs",
        help = "Append results to organizations.yml file."
    ).flag(default = false)

    private val localTransport = SFTPTransportType(
        host = "sftp",
        port = "22",
        filePath = "./upload",
        credentialName = "DEFAULT-SFTP"
    )

    override fun run() {
        checkApi(environment)
        val output = getAll(environment, oktaAccessToken)
        // Write out the settings exactly as retrieved
        echo("Outputting original settings...")
        val settings = yamlMapper.writeValueAsString(output)
        writeOutput(settings)
        // Handle load option.
        if (loadToLocal) {
            handleLoadToLocalOption(output)
            // This is an else because if the load option is used, the appending is handled
            // inside it so the version with modified transport can be appended to the organizations.yml
        } else if (appendToOrgs) {
            appendToOrgs(settings)
        }
    }

    private fun getAll(environment: Environment, accessToken: String): List<DeepOrganization> {
        // get organizations
        val organizationJson = getMany(environment, accessToken, SettingType.ORGANIZATION, settingName = "")
        var organizations = jsonMapper.readValue(organizationJson, Array<OrganizationAPI>::class.java)
        if (filter != null) {
            organizations = organizations.filter { it.name.startsWith(filter!!, ignoreCase = true) }.toTypedArray()
        }

        // get senders and receivers per org
        return organizations.map { org ->
            val sendersJson = getMany(environment, accessToken, SettingType.SENDER, org.name)
            val orgSenders = jsonMapper.readValue(sendersJson, Array<Sender>::class.java).map { it.makeCopy() }
            val receiversJson = getMany(environment, accessToken, SettingType.RECEIVER, org.name)
            val orgReceivers = jsonMapper.readValue(receiversJson, Array<ReceiverAPI>::class.java).map { Receiver(it) }
            DeepOrganization(org, orgSenders, orgReceivers)
        }
    }

    /**
     * Handles loading [settings] to the local database with the transport option modified to [localTransport] if the
     * [loadToLocal] flag is present. Also has special handling if the [appendToOrgs] flag is present to allow modified
     * transport to be appended into [organizationsFile].
     */
    private fun handleLoadToLocalOption(settings: List<DeepOrganization>) {
        if (settings.isNotEmpty()) {
            // Change transports to SFTP
            val modifiedOrgs = settings.map { org ->
                val modifiedReceivers = org.receivers.map {
                    Receiver(
                        it.name,
                        it.organizationName,
                        it.topic,
                        it.customerStatus,
                        it.translation,
                        it.jurisdictionalFilter,
                        it.qualityFilter,
                        it.routingFilter,
                        it.processingModeFilter,
                        it.reverseTheQualityFilter,
                        it.conditionFilter,
                        it.deidentify,
                        it.deidentifiedValue,
                        it.timing,
                        it.description,
                        localTransport,
                        it.externalName,
                        it.timeZone,
                        it.dateTimeFormat
                    )
                }
                DeepOrganization(org, org.senders, modifiedReceivers)
            }
            val differences = diffAll(modifiedOrgs, Environment.LOCAL, dummyAccessToken)
            if (differences.isNotEmpty()) {
                echoDiff(differences)
                confirm()
            }
            val output = putAll(modifiedOrgs, Environment.LOCAL, dummyAccessToken)
            echo("Loaded settings to local DB:")
            echo("${output.joinToString("\n")}\n")
            if (appendToOrgs) {
                echo("Adding settings that were loaded to the local database to the organizations.yml file")
                appendToOrgs(yamlMapper.writeValueAsString(modifiedOrgs))
            }
        } else {
            echo("No settings fitting your parameters were returned.")
        }
    }

    /**
     * Appends [output] to [organizationsFile] file. Since we know this is an existing file, and are appending, not
     * overwriting, we remove "---" from the beginning if it exists.
     */
    private fun appendToOrgs(output: String) {
        if (output.startsWith("---")) {
            File(organizationsFile).appendBytes(output.replaceFirst("---", "").toByteArray())
        } else {
            File(organizationsFile).appendBytes(output.toByteArray())
        }
    }
}