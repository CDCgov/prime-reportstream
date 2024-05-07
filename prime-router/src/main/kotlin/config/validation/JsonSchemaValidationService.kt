package gov.cdc.prime.router.config.validation

import com.networknt.schema.ValidationMessage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import java.io.File
import java.io.InputStream

class JsonSchemaValidationService {

    fun validateYAMLStructure(
        configType: ConfigurationType,
        file: File,
    ): Set<ValidationMessage> {
        return validateYAMLStructure(configType, file.inputStream())
    }

    fun validateYAMLStructure(
        configType: ConfigurationType,
        yamlString: String,
    ): Set<ValidationMessage> {
        return validateYAMLStructure(configType, yamlString.byteInputStream())
    }

    fun validateYAMLStructure(
        configType: ConfigurationType,
        inputStream: InputStream,
    ): Set<ValidationMessage> {
        val parsed = JacksonMapperUtilities.yamlMapper.readTree(inputStream)
        return configType.jsonSchema.validate(parsed)
    }
}