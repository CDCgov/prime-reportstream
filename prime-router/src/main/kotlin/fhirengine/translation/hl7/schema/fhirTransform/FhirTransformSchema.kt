package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetCollection
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import java.util.SortedMap

/**
 * A FHIR transform schema.
 * @property name the schema name
 * @property elements the elements for the schema
 * @property constants schema level constants
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
class FhirTransformSchema(
    elements: MutableList<FhirTransformSchemaElement> = mutableListOf(),
    constants: SortedMap<String, String> = sortedMapOf(),
    extends: String? = null
) : ConfigSchema<FhirTransformSchemaElement>(elements = elements, constants = constants, extends = extends) {
    override fun merge(childSchema: ConfigSchema<FhirTransformSchemaElement>):
        ConfigSchema<FhirTransformSchemaElement> =
        apply {
            check(childSchema is FhirTransformSchema) { "Child schema ${childSchema.name} not a FHIRTransformSchema." }
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
 * @property resourceIndex the variable name to store a FHIR collection's index number
 * @property constants element level constants
 * @property valueSet a collection of key-value pairs used to convert the value property
 * @property debug log debug information for the element
 * @property bundleProperty a FHIR path denoting where to store the value
 */
@JsonIgnoreProperties
class FhirTransformSchemaElement(
    name: String? = null,
    condition: String? = null,
    required: Boolean? = null,
    schema: String? = null,
    schemaRef: FhirTransformSchema? = null,
    resource: String? = null,
    value: List<String>? = null,
    resourceIndex: String? = null,
    constants: SortedMap<String, String> = sortedMapOf(),
    valueSet: ValueSetCollection? = null,
    debug: Boolean = false,
    var bundleProperty: String? = null,
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
            !schema.isNullOrBlank() && bundleProperty != null ->
                addError("Schema property cannot be used with the bundleProperty property")
            schema.isNullOrBlank() && bundleProperty == null ->
                addError("BundleProperty property is required when not using a schema")
        }

        return super.validate()
    }

    override fun merge(overwritingElement: ConfigSchemaElement): ConfigSchemaElement = apply {
        check(overwritingElement is FhirTransformSchemaElement) {
            "Overwriting element ${overwritingElement.name} was not a FHIRTransformSchemaElement."
        }
        overwritingElement.bundleProperty?.let { this.bundleProperty = overwritingElement.bundleProperty }
        super.merge(overwritingElement)
    }
}