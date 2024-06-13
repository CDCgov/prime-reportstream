package gov.cdc.prime.router.config.validation

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import java.io.File

/**
 * A configuration type containing all necessary data to parse and validate a YAML file
 */
sealed class ConfigurationType<T> {

    // references to how a type is validated
    abstract val jsonSchema: JsonSchema
    abstract val konformValidation: KonformValidation<T>

    // implementations for using Jackson to parse to a type
    abstract fun convert(node: JsonNode): T

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

        override val konformValidation: KonformValidation<List<DeepOrganization>> = OrganizationValidation

        override fun convert(node: JsonNode): List<DeepOrganization> {
            return mapper.convertValue(node, Array<DeepOrganization>::class.java).toList()
        }
    }

    /**
     * FHIR to FHIR transform configuration
     */
    data object FhirToFhirTransform : ConfigurationType<FhirTransformSchema>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/fhir/fhir-to-fhir-transform.json")
        }

        override val konformValidation: KonformValidation<FhirTransformSchema> = FhirToFhirTransformValidation

        override fun convert(node: JsonNode): FhirTransformSchema {
            return mapper.convertValue(node, FhirTransformSchema::class.java)
        }
    }

    data object FhirToHL7Mapping : ConfigurationType<HL7ConverterSchema>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/fhir/fhir-to-hl7-mapping.json")
        }

        override val konformValidation: KonformValidation<HL7ConverterSchema> = FhirToHL7MappingValidation

        override fun convert(node: JsonNode): HL7ConverterSchema {
            return mapper.convertValue(node, HL7ConverterSchema::class.java)
        }
    }
}