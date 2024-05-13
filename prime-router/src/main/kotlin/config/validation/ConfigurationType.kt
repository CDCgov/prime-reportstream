package gov.cdc.prime.router.config.validation

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.common.JacksonMapperUtilities
import io.konform.validation.Validation
import java.io.File
import java.io.InputStream

/**
 * A configration type containing all necessary data to parse and validate a YAML file
 */
sealed class ConfigurationType<T> {

    // references to how a type is validated
    abstract val jsonSchema: JsonSchema
    abstract val valueValidation: Validation<T>

    // implementations for using Jackson to parse to a type
    abstract fun convert(node: JsonNode): T
    abstract fun parse(inputStream: InputStream): T

    // helper function for loading a json schema file
    protected fun getSchema(path: String): JsonSchema {
        val rawSchema = File(path).inputStream()
        return JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(rawSchema)
    }

    // Jackson yaml mapper reference
    protected val mapper = JacksonMapperUtilities.yamlMapper

    /**
     * Organizations configuration
     */
    data object Organizations : ConfigurationType<List<DeepOrganization>>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/organizations/organizations.json")
        }
        override val valueValidation: Validation<List<DeepOrganization>> = OrganizationValidation.validation
        override fun convert(node: JsonNode): List<DeepOrganization> {
            return mapper.convertValue(node, Array<DeepOrganization>::class.java).toList()
        }
        override fun parse(inputStream: InputStream): List<DeepOrganization> {
            return mapper.readValue(inputStream, Array<DeepOrganization>::class.java).toList()
        }
    }

//    data object FhirTransforms : ConfigurationType<...>() {

//    }
//
//    data object FhirMappings : ConfigurationType<...>() {

//    }
}