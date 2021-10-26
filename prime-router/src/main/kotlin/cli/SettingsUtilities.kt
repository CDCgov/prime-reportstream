package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.azure.SenderAPI
import org.apache.http.HttpStatus
import java.io.InputStream
import java.io.OutputStream

private const val jsonMimeType = "application/json"
private const val apiPath = "/api/settings"

class SettingsUtilities {

    data class Environment(
        val name: String,
        val baseUrl: String,
        val useHttp: Boolean = false,
        val oktaApp: OktaCommand.OktaApp? = null
    )

    enum class Operation { LIST, GET, PUT, DELETE }
    enum class SettingType { ORG, SENDER, RECEIVER }

    companion object {

        private val outStream: OutputStream? = System.out
        private val jsonMapper = jacksonObjectMapper()
        private val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        init {
            // Format OffsetDateTime as an ISO string
            jsonMapper.registerModule(JavaTimeModule())
            jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            yamlMapper.registerModule(JavaTimeModule())
            yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        fun abort(message: String): Nothing {
            throw PrintMessage(message, error = true)
        }

        fun readInput(inputStream: InputStream): String {
            if (inputStream == null) abort("Missing input file")
            val input = String(inputStream!!.readAllBytes())
            if (input.isBlank()) abort("Blank input")
            return input
        }

        fun writeOutput(output: String) {
            outStream!!.write(output.toByteArray())
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

        fun formPath(
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

        fun put(
            path: String,
            accessToken: String,
            settingName: String,
            payload: String
        ): String {
            val (_, response, result) = Fuel
                .put(path)
                .authentication()
                .bearer(accessToken)
                .header(CONTENT_TYPE to jsonMimeType)
                .jsonBody(payload)
                .responseJson()
            return when (result) {
                is Result.Failure -> {
                    "Error on put of $settingName: ${response.responseMessage} ${String(response.data)}"
                }
                is Result.Success ->
                    when (response.statusCode) {
                        HttpStatus.SC_OK -> {
                            val version = result.value.obj().getInt("version")
                            "Success. Setting $settingName at version $version"
                        }
                        HttpStatus.SC_CREATED -> "Success. Created $settingName"
                        else -> error("Unexpected successful status code")
                    }
            }
        }

        fun get(
            path: String,
            accessToken: String,
            settingName: String
        ): String {
            val (_, response, result) = Fuel
                .get(path)
                .authentication()
                .bearer(accessToken)
                .header(CONTENT_TYPE to jsonMimeType)
                .responseString()
            return when (result) {
                is Result.Failure ->
                    "Error getting $settingName: ${response.responseMessage} ${String(response.data)}"
                is Result.Success -> result.value
            }
        }

        fun delete(
            path: String,
            accessToken: String,
            settingName: String
        ): String {
            val (_, response, result) = Fuel
                .delete(path)
                .authentication()
                .bearer(accessToken)
                .header(CONTENT_TYPE to jsonMimeType)
                .responseString()
            return when (result) {
                is Result.Failure ->
                    "Error on delete of $settingName: ${response.responseMessage} ${String(response.data)}"
                is Result.Success ->
                    "Success $settingName: ${result.value}"
            }
        }
    }
}