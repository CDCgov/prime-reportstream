package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetCollection
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import java.util.SortedMap

/**
 * A converter schema.
 * @property name the schema name
 * @property hl7Class the HL7 message HAPI Hl7 v2 class name used for the output. Only allowed at the top level schema
 * @property elements the elements for the schema
 * @property constants schema level constants
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
class ConverterSchema(
    var hl7Class: String? = null,
    elements: MutableList<ConverterSchemaElement> = mutableListOf(),
    constants: SortedMap<String, String> = sortedMapOf(),
    extends: String? = null
) : ConfigSchema<ConverterSchemaElement>(elements = elements, constants = constants, extends = extends) {
    override fun validate(isChildSchema: Boolean): List<String> {
        if (isChildSchema) {
            if (!hl7Class.isNullOrBlank()) {
                addError("Schema hl7Class can only be specified in top level schema")
            }
        } else {
            if (hl7Class.isNullOrBlank()) {
                addError("Schema hl7Class cannot be blank")
            }
        }

        // Do we support the provided HL7 type and version?
        if (hl7Class != null) {
            if (!HL7Utils.supports(hl7Class!!)) {
                addError("Schema unsupported hl7 class of $hl7Class")
            }
        }

        return super.validate(isChildSchema)
    }

    override fun merge(childSchema: ConfigSchema<ConverterSchemaElement>): ConfigSchema<ConverterSchemaElement> =
        apply {
            check(childSchema is ConverterSchema) { "Child schema ${childSchema.name} not a ConverterSchema." }
            childSchema.hl7Class?.let { this.hl7Class = childSchema.hl7Class }
            super.merge(childSchema)
        }
}

/**
 * An element within a Schema.
 * @property name the name of the element
 * @property condition a FHIR path condition to evaluate. If false then the element is ignored.
 * @property required true if the element must have a value
 * @property schema the name of a child schema
 * @property schemaRef the reference to the loaded child schema
 * @property resource a FHIR path that points to a FHIR resource
 * @property value a list of FHIR paths each pointing to a FHIR primitive value
 * @property hl7Spec a list of hl7Specs that denote the field to place a value into
 * @property resourceIndex the variable name to store a FHIR collection's index number
 * @property constants element level constants
 * @property valueSet a collection of key-value pairs used to convert the value property
 * @property debug log debug information for the element
 */
@JsonIgnoreProperties
class ConverterSchemaElement(
    name: String? = null,
    condition: String? = null,
    required: Boolean? = null,
    schema: String? = null,
    schemaRef: ConfigSchema<ConverterSchemaElement>? = null,
    resource: String? = null,
    value: List<String>? = null,
    var hl7Spec: List<String> = emptyList(),
    resourceIndex: String? = null,
    constants: SortedMap<String, String> = sortedMapOf(),
    valueSet: ValueSetCollection? = null,
    debug: Boolean = false
) : ConfigSchemaElement(
    name = name,
    condition = condition,
    required = required,
    schema = schema,
    schemaRef = schemaRef,
    resource = resource,
    value = value,
    resourceIndex = resourceIndex,
    constants = constants,
    valueSet = valueSet,
    debug = debug
) {
    override fun validate(): List<String> {
        when {
            !schema.isNullOrBlank() && hl7Spec.isNotEmpty() ->
                addError("Schema property cannot be used with the hl7Spec property")
            schema.isNullOrBlank() && hl7Spec.isEmpty() ->
                addError("Hl7Spec property is required when not using a schema")
        }

        return super.validate()
    }

    override fun merge(overwritingElement: ConfigSchemaElement): ConfigSchemaElement = apply {
        check(overwritingElement is ConverterSchemaElement) {
            "Overwriting element ${overwritingElement.name} was not a ConverterSchemaElement."
        }
        if (overwritingElement.hl7Spec.isNotEmpty()) this.hl7Spec = overwritingElement.hl7Spec
        super.merge(overwritingElement)
    }
}