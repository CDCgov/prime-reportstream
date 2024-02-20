package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import gov.cdc.prime.router.common.JacksonMapperUtilities
import java.io.File

/**
 * Validate a yaml file with a json schema, validation passes if the objects in the yaml file
 * match the schema, otherwise errors will indicate the location and reason for the invalid
 */
class ValidateSettingCommands(
    private val metadataInstance: Metadata? = null,
) : CliktCommand(
    name = "validate-setting",
    help = "Validate settings e.g. 'settings/organizations.yml' with schema, " +
            "e.g. 'src/main/resources/settings/schemas/settings.json"
) {

    private val yamlMapper = JacksonMapperUtilities.yamlMapperNoNilNoEmpty
    private val jsonSchemaFactory: JsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)

    private val inputOption = option(
        "-i", "--input",
        help = "Setting file (e.g. organizations.yml)",
        metavar = "<setting-file>"
    ).file(mustBeReadable = true).required()

    private val schemaFileOption = option(
        "-s", "--schema",
        help = "Schema to validate setting with",
        metavar = "<schema-file>"
    ).file(mustBeReadable = true).required()

    private val inputFile by inputOption

    private val schemaFile by schemaFileOption

    fun getSchema(schemaFile: File): String {
        return readInput(schemaFile)
    }

    private fun getSetting(settingFile: File): String {
        return readInput(settingFile)
    }

    private fun readInput(inputFile: File): String {
        val input = String(inputFile.readBytes())
        if (input.isBlank()) abort("Blank content")
        return input
    }

    fun validate(): Set<ValidationMessage> {
        val settingStr = getSetting(inputFile)
        val schema = jsonSchemaFactory.getSchema(schemaFile.toURI())
        val settings = yamlMapper.readTree(settingStr)
        return schema.validate(settings)
    }
    override fun run() {
        val invalidMessages = validate()
        echo("validation completed: validation messages count: ${invalidMessages.size}")
        invalidMessages.forEach {
            println("error: $it")
            println("schema path: ${it.evaluationPath}")
        }
    }
}