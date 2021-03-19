package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.AUTHORIZATION
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.azure.SenderAPI
import org.apache.http.HttpStatus

private const val apiPath = "/api/settings"
private const val dummyAccessToken = "dummy"
private const val jsonMimeType = "application/json"

// Must be configured in the Okta applications for this to work
private const val oktaRedirectUrl = "http://localhost:9999/redirect"
private const val oktaBaseUrl = "https://hhs-prime.okta.com"
private const val oktaScope = "openid"

/**
 * Base class to handle common stuff: authentication, calling, inputs and outputs
 */
abstract class SettingCommand(
    name: String,
    help: String,
) : CliktCommand(name = name, help = help) {
    private val env by option("--env", help = "Environment to run against", envvar = "PRIME_ENVIRONMENT")
        .choice("local", "test", "staging", "prod")
        .default("local", "local")

    private val accessParam by option("--access", envvar = "PRIME_ACCESS_TOKEN")

    private val outStream by option("--output", help = "output file name", metavar = "file")
        .outputStream(createIfNotExist = true, truncateExisting = true)
        .default(System.out)

    private val inStream by option("--input", help = "input file name", metavar = "file")
        .inputStream()

    data class Environment(
        val name: String,
        val baseUrl: String,
        val useHttp: Boolean = false,
        val authWithOkta: Boolean = true,
        val oktaClientId: String = ""
    )

    enum class Operation { LIST, GET, PUT, }
    enum class SettingType { ORG, SENDER, RECEIVER }

    private val environments = listOf(
        Environment("local", "localhost:7071", useHttp = true, authWithOkta = false),
        Environment("test", "test.prime.cdc.gov", oktaClientId = "0oa6fm8j4G1xfrthd4h6"),
        Environment("staging", "staging.prime.cdc.gov", oktaClientId = "0oa6fm8j4G1xfrthd4h6"),
        Environment("prod", "prime.cdc.gov", oktaClientId = "0oa6kt4j3tOFz5SH84h6"),
    )

    val jsonMapper = jacksonObjectMapper()
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    init {
        // Format OffsetDateTime as an ISO string
        jsonMapper.registerModule(JavaTimeModule())
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        yamlMapper.registerModule(JavaTimeModule())
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun getEnvironment(): Environment {
        return environments.find { it.name == env } ?: abort("bad environment")
    }

    fun getAccessToken(environment: Environment): String {
        if (!environment.authWithOkta) return dummyAccessToken
        if (accessParam != null) return accessParam!!
        TODO()
    }

    fun put(
        environment: Environment,
        sessionToken: String,
        settingType: SettingType,
        settingName: String,
        payload: String
    ): String {
        val path = formPath(environment, Operation.PUT, settingType, settingName)
        val (_, response, result) = Fuel
            .put(path)
            .header(CONTENT_TYPE to jsonMimeType, AUTHORIZATION to "Bearer $sessionToken")
            .body(payload)
            .responseString()
        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success ->
                if (response.statusCode == HttpStatus.SC_OK)
                    "No change: $settingName"
                else
                    "Updated: $settingName"
        }
    }

    fun get(environment: Environment, sessionToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.GET, settingType, settingName)
        val (_, _, result) = Fuel
            .get(path)
            .header(CONTENT_TYPE to jsonMimeType, AUTHORIZATION to "Bearer $sessionToken")
            .responseString()
        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> result.value
        }
    }

    fun list(environment: Environment, sessionToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.LIST, settingType, settingName)
        val (_, _, result) = Fuel
            .get(path)
            .header(CONTENT_TYPE to jsonMimeType, AUTHORIZATION to "Bearer $sessionToken")
            .responseJson()
        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> "[${result.value.array().join(",\n")}]"
        }
    }

    private fun formPath(
        environment: Environment,
        operation: Operation,
        settingType: SettingType,
        settingName: String
    ): String {
        val protocol = if (environment.useHttp) "http" else "https"
        return "$protocol://${environment.baseUrl}$apiPath${settingPath(operation, settingType, settingName)}"
    }

    fun settingPath(operation: Operation, settingType: SettingType, settingName: String): String {
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

    fun readInput(): String {
        if (inStream == null) abort("Missing input file")
        val input = String(inStream!!.readAllBytes())
        if (input.isBlank()) abort("Blank input")
        return input
    }

    fun writeOutput(output: String) {
        outStream.write(output.toByteArray())
    }

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    fun fromYaml(input: String, settingType: SettingType): String {
        return when (settingType) {
            SettingType.ORG -> {
                val organization = yamlMapper.readValue(input, OrganizationAPI::class.java)
                jsonMapper.writeValueAsString(organization)
            }
            SettingType.SENDER -> {
                val sender = yamlMapper.readValue(input, SenderAPI::class.java)
                jsonMapper.writeValueAsString(sender)
            }
            SettingType.RECEIVER -> {
                val receiver = yamlMapper.readValue(input, ReceiverAPI::class.java)
                jsonMapper.writeValueAsString(receiver)
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
                val receiver = jsonMapper.readValue(output, SenderAPI::class.java)
                return yamlMapper.writeValueAsString(receiver)
            }
            SettingType.RECEIVER -> {
                val receiver = jsonMapper.readValue(output, SenderAPI::class.java)
                return yamlMapper.writeValueAsString(receiver)
            }
        }
    }

    fun toYamlList(output: String, settingType: SettingType): String {
        // DevNote: could be handled by inherited methods, but decided that keeping all these together was maintainable
        return when (settingType) {
            SettingType.ORG -> {
                val organization = jsonMapper.readValue(output, Array<OrganizationAPI>::class.java)
                yamlMapper.writeValueAsString(organization)
            }
            SettingType.SENDER -> {
                val receivers = jsonMapper.readValue(output, Array<SenderAPI>::class.java)
                return yamlMapper.writeValueAsString(receivers)
            }
            SettingType.RECEIVER -> {
                val receivers = jsonMapper.readValue(output, Array<SenderAPI>::class.java)
                return yamlMapper.writeValueAsString(receivers)
            }
        }
    }
}

/**
 * Handle a setting for a single entity. Includes the run method.
 */
abstract class SingleSettingCommand(
    val name: String,
    val help: String,
    val settingType: SettingType,
    val operation: Operation,
) : SettingCommand(name = name, help = help) {
    val settingName by argument("name", help = "Name of setting")
        .default("")

    private val useYaml by option(
        "--yaml", "--yml",
        help = "input and output in YAML format instead of JSON"
    ).flag(default = false)

    override fun run() {
        // Authenticate
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)

        // Operations
        when (operation) {
            Operation.LIST -> {
                val output = list(environment, accessToken, settingType, settingName)
                if (useYaml) writeOutput(toYamlList(output, settingType)) else writeOutput(output)
            }
            Operation.GET -> {
                val output = get(environment, accessToken, settingType, settingName)
                if (useYaml) writeOutput(toYaml(output, settingType)) else writeOutput(output)
            }
            Operation.PUT -> {
                val payload = if (useYaml) fromYaml(readInput(), settingType) else readInput()
                val output = put(environment, accessToken, settingType, settingName, payload)
                writeOutput(output)
            }
        }
    }
}

/**
 * Organization setting commands
 */
class OrganizationSettings : CliktCommand(
    name = "organization",
    help = "Fetch and update settings for an organization"
) {
    init { subcommands(ListOrganizationSetting(), GetOrganizationSetting(), PutOrganizationSetting()) }

    override fun run() {}
}

class ListOrganizationSetting : SingleSettingCommand(
    name = "list",
    help = "Fetch all organization settings",
    settingType = SettingType.ORG,
    operation = Operation.LIST
)

class GetOrganizationSetting : SingleSettingCommand(
    name = "get",
    help = "Fetch a organization",
    settingType = SettingType.ORG,
    operation = Operation.GET
)

class PutOrganizationSetting : SingleSettingCommand(
    name = "put",
    help = "Update a organization",
    settingType = SettingType.ORG,
    operation = Operation.PUT
)

/**
 * Sender setting commands
 */
class SenderSettings : CliktCommand(
    name = "sender",
    help = "Fetch and update settings for an sender"
) {
    init { subcommands(ListSenderSetting(), GetSenderSetting(), PutSenderSetting()) }

    override fun run() {}
}

class ListSenderSetting : SingleSettingCommand(
    name = "list",
    help = "Fetch all sender settings for an organization",
    settingType = SettingType.SENDER,
    operation = Operation.LIST
)

class GetSenderSetting : SingleSettingCommand(
    name = "get",
    help = "Fetch a sender",
    settingType = SettingType.SENDER,
    operation = Operation.GET
)

class PutSenderSetting : SingleSettingCommand(
    name = "put",
    help = "Update a sender",
    settingType = SettingType.SENDER,
    operation = Operation.PUT
)

/**
 * Receiver setting commands
 */
class ReceiverSettings : CliktCommand(
    name = "receiver",
    help = "Fetch and update settings for an receiver"
) {
    init { subcommands(ListReceiverSetting(), GetReceiverSetting(), PutReceiverSetting()) }

    override fun run() {}
}

class ListReceiverSetting : SingleSettingCommand(
    name = "list",
    help = "Fetch all receiver settings for an organization",
    settingType = SettingType.RECEIVER,
    operation = Operation.LIST
)

class GetReceiverSetting : SingleSettingCommand(
    name = "get",
    help = "Fetch a receiver",
    settingType = SettingType.RECEIVER,
    operation = Operation.GET
)

class PutReceiverSetting : SingleSettingCommand(
    name = "put",
    help = "Update a receiver",
    settingType = SettingType.RECEIVER,
    operation = Operation.PUT
)

/**
 * Update multiple settings
 */
class MultipleSettings : CliktCommand(
    name = "multiple-settings",
    help = "Retrieve and alter multiple settings using a 'organizations.yml' file."
) {
    init { subcommands(PutMultipleSettings(), GetMultipleSettings()) }

    override fun run() {}
}

class PutMultipleSettings : SettingCommand(
    name = "put",
    help = "set all settings from a 'organizations.yml' file"
) {
    override fun run() {
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)
        val results = putAll(environment, accessToken)
        val output = "${results.joinToString("\n")}\n"
        writeOutput(output)
    }

    fun putAll(environment: Environment, accessToken: String): List<String> {
        val deepOrgs = readYaml()
        val results = mutableListOf<String>()
        // Put orgs
        deepOrgs.forEach { deepOrg ->
            val org = Organization(deepOrg)
            val payload = jsonMapper.writeValueAsString(org)
            results += put(environment, accessToken, SettingType.ORG, deepOrg.name, payload)
        }
        // Put senders
        deepOrgs.flatMap { it.senders }.forEach { sender ->
            val payload = jsonMapper.writeValueAsString(sender)
            results += put(environment, accessToken, SettingType.SENDER, sender.fullName, payload)
        }
        // Put receivers
        deepOrgs.flatMap { it.receivers }.forEach { receiver ->
            val payload = jsonMapper.writeValueAsString(receiver)
            results += put(environment, accessToken, SettingType.RECEIVER, receiver.fullName, payload)
        }
        return results
    }

    fun readYaml(): List<DeepOrganization> {
        val input = readInput()
        return yamlMapper.readValue(input)
    }
}

class GetMultipleSettings : SettingCommand(
    name = "get",
    help = "get all settings from an environment in yaml format"
) {
    override fun run() {
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)
        val output = getAll(environment, accessToken)
        writeOutput(output)
    }

    fun getAll(environment: Environment, accessToken: String): String {
        // get orgs
        val orgsJson = list(environment, accessToken, SettingType.ORG, settingName = "")
        val orgs = jsonMapper.readValue(orgsJson, Array<OrganizationAPI>::class.java)

        // get senders and receivers per org
        val deepOrgs = orgs.map { org ->
            val sendersJson = list(environment, accessToken, SettingType.SENDER, org.name)
            val orgSenders = jsonMapper.readValue(sendersJson, Array<SenderAPI>::class.java).map { Sender(it) }
            val receiversJson = list(environment, accessToken, SettingType.RECEIVER, org.name)
            val orgReceivers = jsonMapper.readValue(receiversJson, Array<ReceiverAPI>::class.java).map { Receiver(it) }
            DeepOrganization(org, orgSenders, orgReceivers)
        }
        return yamlMapper.writeValueAsString(deepOrgs)
    }
}