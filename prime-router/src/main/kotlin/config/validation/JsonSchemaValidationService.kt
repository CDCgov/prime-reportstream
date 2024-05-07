package gov.cdc.prime.router.config.validation

import com.networknt.schema.ValidationMessage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import java.io.File
import java.io.InputStream

/**
 * Service used to validate YAML files against a JSON schema
 */
class JsonSchemaValidationService {

    /**
     * Validate the YAML structure of a file
     */
    fun validateYAMLStructure(
        configType: ConfigurationType,
        file: File,
    ): Set<ValidationMessage> {
        return validateYAMLStructure(configType, file.inputStream())
    }

    /**
     * Validate the YAML structure of a string
     */
    fun validateYAMLStructure(
        configType: ConfigurationType,
        yamlString: String,
    ): Set<ValidationMessage> {
        return validateYAMLStructure(configType, yamlString.byteInputStream())
    }

    /**
     * Validate the YAML structure of an input stream
     */
    fun validateYAMLStructure(
        configType: ConfigurationType,
        inputStream: InputStream,
    ): Set<ValidationMessage> {
        val parsed = JacksonMapperUtilities.yamlMapper.readTree(inputStream)
        return configType.jsonSchema.validate(parsed)
    }
}