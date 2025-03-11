package gov.cdc.prime.router.config.validation

import gov.cdc.prime.router.common.JacksonMapperUtilities
import java.io.File
import java.io.InputStream

/**
 * Service used to validate YAML files against a JSON schema
 */
interface JsonSchemaValidationService {
    /**
     * Validate the YAML structure of a file
     */
    fun <T> validateYAMLStructure(
        configType: ConfigurationType<T>,
        file: File,
    ): ConfigurationValidationResult<T>

    /**
     * Validate the YAML structure of a string
     */
    fun <T> validateYAMLStructure(
        configType: ConfigurationType<T>,
        yamlString: String,
    ): ConfigurationValidationResult<T>

    /**
     * Validate the YAML structure of an input stream
     */
    fun <T> validateYAMLStructure(
        configType: ConfigurationType<T>,
        inputStream: InputStream,
    ): ConfigurationValidationResult<T>
}

class JsonSchemaValidationServiceImpl : JsonSchemaValidationService {

    override fun <T> validateYAMLStructure(
        configType: ConfigurationType<T>,
        file: File,
    ): ConfigurationValidationResult<T> = validateYAMLStructure(configType, file.inputStream())

    override fun <T> validateYAMLStructure(
        configType: ConfigurationType<T>,
        yamlString: String,
    ): ConfigurationValidationResult<T> = validateYAMLStructure(configType, yamlString.byteInputStream())

    override fun <T> validateYAMLStructure(
        configType: ConfigurationType<T>,
        inputStream: InputStream,
    ): ConfigurationValidationResult<T> {
        var schemaErrors: List<String> = emptyList()
        return try {
            val parsedJson = JacksonMapperUtilities.yamlMapper.readTree(inputStream)
            val schemaValidation = configType.jsonSchema.validate(parsedJson)
            schemaErrors = schemaValidation.map { it.message }
            if (schemaErrors.isEmpty()) {
                val parsed = configType.convert(parsedJson)
                ConfigurationValidationSuccess(parsed)
            } else {
                ConfigurationValidationFailure(schemaErrors)
            }
        } catch (ex: Exception) {
            ConfigurationValidationFailure(schemaErrors, ex)
        }
    }
}