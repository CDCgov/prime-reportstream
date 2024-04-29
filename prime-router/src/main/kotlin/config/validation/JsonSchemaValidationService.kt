package gov.cdc.prime.router.config.validation

import com.networknt.schema.ValidationMessage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import java.io.File
import java.io.InputStream

class JsonSchemaValidationService {

    fun validateYAMLFileStructure(
        configType: ConfigurationType,
        file: File,
    ): Set<ValidationMessage> {
        return validateYAMLFileStructure(configType, file.inputStream())
    }

    fun validateYAMLFileStructure(
        configType: ConfigurationType,
        yaml: String,
    ): Set<ValidationMessage> {
        return validateYAMLFileStructure(configType, yaml.byteInputStream())
    }

    fun validateYAMLFileStructure(
        configType: ConfigurationType,
        inputStream: InputStream,
    ): Set<ValidationMessage> {
        val parsed = JacksonMapperUtilities.yamlMapper.readTree(inputStream)
        return configType.jsonSchema.validate(parsed)
    }
}