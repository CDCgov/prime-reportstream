package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.inputStream
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
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.azure.SenderAPI
import gov.cdc.prime.router.common.Environment
import org.apache.http.HttpStatus
import java.io.InputStream

private const val apiPath = "/api/settings"
private const val dummyAccessToken = "dummy"
private const val jsonMimeType = "application/json"

/**
 * Base class to handle common stuff: authentication, calling, inputs and outputs
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

    protected val inStreamOption = option(
        "-i", "--input",
        help = "Input from file",
        metavar = "<file>"
    ).inputStream()

    protected val nameOption = option(
        "-n", "--name",
        metavar = "<name>",
        help = "The full name of the setting"
    ).required()

    protected val jsonOption = option(
        "--json",
        help = "Use the JSON format instead of YAML"
    ).flag(default = false)

    open val inStream: InputStream? = null

    enum class Operation { LIST, GET, PUT, DELETE }
    enum class SettingType { ORG, SENDER, RECEIVER }

    val jsonMapper = jacksonObjectMapper()
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
        // Format OffsetDateTime as an ISO string
        jsonMapper.registerModule(JavaTimeModule())
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        yamlMapper.registerModule(JavaTimeModule())
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * The environment specified by the command line parameters
     */
    val cliEnvironment: Environment by lazy {
        Environment.get(env)
    }

    /**
     * The accessToken left by a previous login command as specified by the command line parameters
     */
    val cliAccessToken: String by lazy {
        if (cliEnvironment.oktaApp == null) {
            dummyAccessToken
        } else {
            OktaCommand.fetchAccessToken(cliEnvironment.oktaApp)
                ?: abort("Invalid access token. Run ./prime login to fetch/refresh your access token.")
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
                        val version = result.value.obj().getInt("version")
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
    fun get(environment: Environment, accessToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.GET, settingType, settingName)
        verbose("GET $path")
        val (_, response, result) = SettingsUtilities.get(path, accessToken)
        return when (result) {
            is Result.Failure ->
                abort("Error getting $settingName: ${response.responseMessage} ${String(response.data)}")
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
            .responseJson()
        return when (result) {
            is Result.Failure -> handleHttpFailure(settingName, response, result)
            is Result.Success -> {
                val resultObjs = result.value.array()
                val names = if (settingType == SettingType.ORG) {
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

    /**
     * Echo a difference table for [settingName] to [payload]. [settingType] is needed for serialization
     */
    fun diff(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String,
        payload: String
    ): Boolean {
        val base = get(environment, accessToken, settingType, settingName)
        val diffList = CommandUtilities.diffJson(base, payload)
            .filter { !it.name.startsWith("meta") } // Remove the meta differences
        val isDifferent = diffList.isNotEmpty()
        if (isDifferent) {
            val diffTable = CommandUtilities.renderDiffTable(diffList)
            echo(diffTable)
        }
        return isDifferent
    }

    fun readInput(): String {
        if (inStream == null) abort("Missing input file")
        val input = String(inStream!!.readAllBytes())
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
            SettingType.ORG -> {
                val organization = mapper.readValue(input, OrganizationAPI::class.java)
                Pair(organization.name, jsonMapper.writeValueAsString(organization))
            }
            SettingType.SENDER -> {
                val sender = mapper.readValue(input, SenderAPI::class.java)
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
            SettingType.ORG -> {
                val organization = jsonMapper.readValue(output, OrganizationAPI::class.java)
                yamlMapper.writeValueAsString(organization)
            }
            SettingType.SENDER -> {
                val sender = jsonMapper.readValue(output, SenderAPI::class.java)
                return yamlMapper.writeValueAsString(sender)
            }
            SettingType.RECEIVER -> {
                val receiver = jsonMapper.readValue(output, ReceiverAPI::class.java)
                return yamlMapper.writeValueAsString(receiver)
            }
        }
    }

    fun echo(message: String) {
        if (!silent) TermUi.echo(message)
    }

    fun verbose(message: String) {
        if (verbose) TermUi.echo(message)
    }

    fun abort(message: String): Nothing {
        if (silent)
            throw ProgramResult(statusCode = 1)
        else
            throw PrintMessage(message, error = true)
    }

    fun confirm(message: String, abortMessage: String = "") {
        if (!silent && TermUi.confirm(message) == false) {
            abort(abortMessage)
        }
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
                    SettingType.ORG -> "/organizations"
                    SettingType.SENDER -> "/organizations/$settingName/senders"
                    SettingType.RECEIVER -> "/organizations/$settingName/receivers"
                }
            } else {
                when (settingType) {
                    SettingType.ORG -> "/organizations/$settingName"
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
        if (settingType != SettingType.ORG && settingName.isBlank())
            abort("Missing organization name argument")
        val output = listNames(cliEnvironment, cliAccessToken, settingType, settingName)
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
        val output = get(cliEnvironment, cliAccessToken, settingType, settingName)
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
        delete(cliEnvironment, cliAccessToken, settingType, settingName)
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
    override val inStream by inStreamOption
    private val useJson: Boolean by jsonOption

    override fun run() {
        val (name, payload) = if (useJson)
            fromJson(readInput(), settingType)
        else
            fromYaml(readInput(), settingType)
        if (silent) {
            put(cliEnvironment, cliAccessToken, settingType, name, payload)
        } else {
            val isDifferent = diff(cliEnvironment, cliAccessToken, settingType, name, payload)
            if (isDifferent) {
                confirm("Make the above changes", "no change applied")
                val output = put(cliEnvironment, cliAccessToken, settingType, name, payload)
                writeOutput(output)
            }
        }
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
    override val inStream by inStreamOption
    private val useJson: Boolean by jsonOption

    override fun run() {
        val (name, payload) = if (useJson)
            fromJson(readInput(), settingType)
        else
            fromYaml(readInput(), settingType)
        diff(cliEnvironment, cliAccessToken, settingType, name, payload)
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
        )
    }

    override fun run() {
        // Does not run at this level
    }
}

class ListOrganizationSetting : SettingCommand(
    name = "list",
    help = "List the names of all organizations"
) {
    override fun run() {
        val output = listNames(cliEnvironment, cliAccessToken, SettingType.ORG, "")
        writeOutput(output.joinToString("\n"))
    }
}

class GetOrganizationSetting : GetSettingCommand(
    name = "get",
    help = "Fetch a organization",
    settingType = SettingType.ORG,
)

class PutOrganizationSetting : PutSettingCommand(
    name = "set",
    help = "Update a organization",
    settingType = SettingType.ORG
)

class DeleteOrganizationSetting : DeleteSettingCommand(
    name = "remove",
    help = "Remove a organization",
    settingType = SettingType.ORG,
)

class DiffOrganizationSetting : DiffSettingCommand(
    name = "diff",
    help = "Compare to the current organization",
    settingType = SettingType.ORG
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
            TokenUrl(),
            AddPublicKey(),
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
    help = "Fetch a sender",
    settingType = SettingType.SENDER,
)

class PutSenderSetting : PutSettingCommand(
    name = "set",
    help = "Update a sender",
    settingType = SettingType.SENDER,
)

class DeleteSenderSetting : DeleteSettingCommand(
    name = "remove",
    help = "Remove a sender",
    settingType = SettingType.SENDER,
)

class DiffSenderSetting : DiffSettingCommand(
    name = "diff",
    help = "Compare to the current sender",
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
    help = "Fetch receiver names for an organization",
    settingType = SettingType.RECEIVER,
)

class GetReceiverSetting : GetSettingCommand(
    name = "get",
    help = "Fetch a receiver",
    settingType = SettingType.RECEIVER,
)

class PutReceiverSetting : PutSettingCommand(
    name = "set",
    help = "Update a receiver",
    settingType = SettingType.RECEIVER,
)

class DeleteReceiverSetting : DeleteSettingCommand(
    name = "remove",
    help = "Remove a receiver",
    settingType = SettingType.RECEIVER,
)

class DiffReceiverSetting : DiffSettingCommand(
    name = "diff",
    help = "Compare to the current receiver",
    settingType = SettingType.RECEIVER,
)

/**
 * Update multiple settings
 */
class MultipleSettings : CliktCommand(
    name = "multiple-settings",
    help = "Fetch and update multiple settings"
) {
    init { subcommands(PutMultipleSettings(), GetMultipleSettings()) }

    override fun run() {
        // Does not run at this level
    }
}

class PutMultipleSettings : SettingCommand(
    name = "set",
    help = "set all settings from a 'organizations.yml' file"
) {

    override val inStream by inStreamOption

    /**
     * Number of connection retries.
     */
    private val connRetries by option("-r", "--retries", help = "Number of seconds to retry waiting for the API")
        .int().default(30)

    override fun run() {
        // First wait for the API to come online
        echo("Waiting for the API at ${cliEnvironment.url} to be available...")
        CommandUtilities.waitForApi(cliEnvironment, connRetries)

        val results = putAll()
        val output = "${results.joinToString("\n")}\n"
        writeOutput(output)
    }

    private fun putAll(): List<String> {
        val deepOrganizations = readYaml()
        val results = mutableListOf<String>()
        // Put organizations
        deepOrganizations.forEach { deepOrg ->
            val org = Organization(deepOrg)
            val payload = jsonMapper.writeValueAsString(org)
            results += put(cliEnvironment, cliAccessToken, SettingType.ORG, deepOrg.name, payload)
        }
        // Put senders
        deepOrganizations.flatMap { it.senders }.forEach { sender ->
            val payload = jsonMapper.writeValueAsString(sender)
            results += put(cliEnvironment, cliAccessToken, SettingType.SENDER, sender.fullName, payload)
        }
        // Put receivers
        deepOrganizations.flatMap { it.receivers }.forEach { receiver ->
            val payload = jsonMapper.writeValueAsString(receiver)
            results += put(cliEnvironment, cliAccessToken, SettingType.RECEIVER, receiver.fullName, payload)
        }
        return results
    }

    private fun readYaml(): List<DeepOrganization> {
        val input = readInput()
        return yamlMapper.readValue(input)
    }
}

class GetMultipleSettings : SettingCommand(
    name = "get",
    help = "get all settings from an environment in yaml format"
) {
    val filter by option(
        "-f", "--filter",
        help = "filter the organizations, only returning those with names that start with <filter>",
        metavar = "<filter>"
    )

    override fun run() {
        val output = getAll(cliEnvironment, cliAccessToken)
        writeOutput(output)
    }

    private fun getAll(environment: Environment, accessToken: String): String {
        // get organizations
        val organizationJson = getMany(environment, accessToken, SettingType.ORG, settingName = "")
        var organizations = jsonMapper.readValue(organizationJson, Array<OrganizationAPI>::class.java)
        if (filter != null) {
            organizations = organizations.filter { it.name.startsWith(filter!!, ignoreCase = true) }.toTypedArray()
        }

        // get senders and receivers per org
        val deepOrganizations = organizations.map { org ->
            val sendersJson = getMany(environment, accessToken, SettingType.SENDER, org.name)
            val orgSenders = jsonMapper.readValue(sendersJson, Array<SenderAPI>::class.java).map { Sender(it) }
            val receiversJson = getMany(environment, accessToken, SettingType.RECEIVER, org.name)
            val orgReceivers = jsonMapper.readValue(receiversJson, Array<ReceiverAPI>::class.java).map { Receiver(it) }
            DeepOrganization(org, orgSenders, orgReceivers)
        }
        return yamlMapper.writeValueAsString(deepOrganizations)
    }
}