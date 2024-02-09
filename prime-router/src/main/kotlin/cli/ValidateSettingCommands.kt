package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import com.networknt.schema.SpecVersion.VersionFlag
import com.networknt.schema.ValidationMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import java.io.File

/**
 * Converts a file into a different output format
 */
class ValidateSettingCommands(
    private val metadataInstance: Metadata? = null,
) : CliktCommand(
    name = "validate-setting",
    help = "Validate settings e.g. 'organizations.yml' with schema, e.g. organizations.schema.json"
) {
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )

    val jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    init {
        yamlMapper.registerModule(JavaTimeModule())
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

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

    fun getSetting(settingFile: File): String {
        return readInput(settingFile)
    }

    private fun readInput(inputFile: File): String {
        val input = String(inputFile.readBytes())
        if (input.isBlank()) abort("Blank content")
        return input
    }

    fun validate(): Set<ValidationMessage> {
        val settingStr = getSetting(inputFile)
        val schemaStr = getSchema(schemaFile)
        val config: SchemaValidatorsConfig = SchemaValidatorsConfig()
        println(config.isCustomMessageSupported)
        val schema = jsonSchemaFactory.getSchema(schemaStr)
        return schema.validate(yamlMapper.readTree(settingStr))
    }
    override fun run() {
        val invalidMessages = validate()
        echo("validation completed: validation messages count: ${invalidMessages.size}")
        invalidMessages.forEach {
            println(it.toString())
        }
    }
}