package gov.cdc.prime.router.config.validation

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.config.validation.models.HL7ToFHIRMappingMessageTemplate
import gov.cdc.prime.router.config.validation.models.HL7ToFHIRMappingResourceTemplate
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import io.github.linuxforhealth.api.Condition
import io.github.linuxforhealth.hl7.expression.ExpressionAttributes
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

        override fun convert(node: JsonNode): List<DeepOrganization> =
        mapper.convertValue(node, Array<DeepOrganization>::class.java).toList()
    }

    /**
     * FHIR to FHIR transform configuration
     */
    data object FhirToFhirTransform : ConfigurationType<FhirTransformSchema>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/fhir/fhir-to-fhir-transform.json")
        }

        override val konformValidation: KonformValidation<FhirTransformSchema> = FhirToFhirTransformValidation

        override fun convert(node: JsonNode): FhirTransformSchema =
            mapper.convertValue(node, FhirTransformSchema::class.java)
    }

    data object FhirToHL7Mapping : ConfigurationType<HL7ConverterSchema>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/fhir/fhir-to-hl7-mapping.json")
        }

        override val konformValidation: KonformValidation<HL7ConverterSchema> = FhirToHL7MappingValidation

        override fun convert(node: JsonNode): HL7ConverterSchema =
            mapper.convertValue(node, HL7ConverterSchema::class.java)
    }

    data object HL7ToFhirMappingMessageTemplate : ConfigurationType<HL7ToFHIRMappingMessageTemplate>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/fhir/hl7-to-fhir-mapping-message-template.json")
        }

        override val konformValidation: KonformValidation<HL7ToFHIRMappingMessageTemplate> =
            HL7ToFHIRMappingMessageTemplateValidation

        override fun convert(node: JsonNode): HL7ToFHIRMappingMessageTemplate =
            mapper.convertValue(node, HL7ToFHIRMappingMessageTemplate::class.java)
    }

    data object HL7ToFhirMappingResourceTemplate : ConfigurationType<HL7ToFHIRMappingResourceTemplate>() {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/fhir/hl7-to-fhir-mapping-resource-template.json")
        }

        override val konformValidation: KonformValidation<HL7ToFHIRMappingResourceTemplate> =
            HL7ToFHIRMappingResourceTemplateValidation

        override fun convert(node: JsonNode): HL7ToFHIRMappingResourceTemplate {
            // grab optional top-level resource type
            val maybeResourceType = node.get("resourceType")?.textValue()

            // parse each expression that is not the resourceType
            val expressions = node
                .fields()
                .asSequence()
                .associate { it.key to it.value }
                .filter { it.key != "resourceType" }
                .mapValues { mapper.convertValue(it.value, ExpressionAttributes::class.java) }

            // extract all parsed conditions into one place
            val flatConditions = expressions.flatMap { flattenConditions(it.value) }

            return HL7ToFHIRMappingResourceTemplate(maybeResourceType, expressions, flatConditions)
        }

        /**
         * Conditions are a part of each expression that we want to validate, so we need
         * to extract them all into one place
         */
        private fun flattenConditions(
            expression: ExpressionAttributes,
        ): List<Condition> {
            val conditions = mutableListOf<Condition>()

            fun flattenConditionsHelper(currentExpression: ExpressionAttributes) {
                if (currentExpression.filter != null) {
                    conditions.add(currentExpression.filter)
                }
                currentExpression.expressions?.forEach {
                    flattenConditionsHelper(it)
                }
                currentExpression.expressionsMap?.values?.forEach {
                    flattenConditionsHelper(it)
                }
            }

            flattenConditionsHelper(expression)
            return conditions
        }
    }
}