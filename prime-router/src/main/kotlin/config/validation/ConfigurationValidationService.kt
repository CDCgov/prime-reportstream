package gov.cdc.prime.router.config.validation

import java.io.File
import java.io.InputStream

/**
 * Service that combines both the JSON schema and
 * value validation into a single service call
 */
interface ConfigurationValidationService {

    /**
     * Validates YAML file structure and values
     */
    fun <T> validateYAML(
        configType: ConfigurationType<T>,
        file: File,
    ): ConfigurationValidationResult<T>

    /**
     * Validates YAML string structure and values
     */
    fun <T> validateYAML(
        configType: ConfigurationType<T>,
        yamlString: String,
    ): ConfigurationValidationResult<T>

    /**
     * Validates YAML stream structure and values
     */
    fun <T> validateYAML(
        configType: ConfigurationType<T>,
        inputStream: InputStream,
    ): ConfigurationValidationResult<T>
}

class ConfigurationValidationServiceImpl(
    private val jsonSchemaValidationService: JsonSchemaValidationService = JsonSchemaValidationServiceImpl(),
    private val configurationValueValidationService: ConfigurationValueValidationService =
    ConfigurationValueValidationServiceImpl(),
) : ConfigurationValidationService {

    override fun <T> validateYAML(configType: ConfigurationType<T>, file: File): ConfigurationValidationResult<T> =
        validateYAML(configType, file.inputStream())

    override fun <T> validateYAML(
        configType: ConfigurationType<T>,
        yamlString: String,
    ): ConfigurationValidationResult<T> = validateYAML(configType, yamlString.byteInputStream())

    override fun <T> validateYAML(
        configType: ConfigurationType<T>,
        inputStream: InputStream,
    ): ConfigurationValidationResult<T> = when (
            val jsonSchemaValidation = jsonSchemaValidationService.validateYAMLStructure(configType, inputStream)
        ) {
            is ConfigurationValidationSuccess -> {
                configurationValueValidationService.validate(configType, jsonSchemaValidation.parsed)
            }
            is ConfigurationValidationFailure -> jsonSchemaValidation
        }
}