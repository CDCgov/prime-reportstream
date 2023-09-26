package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetCollection
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import java.util.SortedMap

/**
 * A FHIR transform template (schema) to be processed by the FHIR transformer
 * @property name the schema name, used by child schemas to extend this schema
 * @property elements list of [FhirTransformSchemaElement] representing schema elements
 * @property constants schema level constants that can be used by any element in the schema or child schemas
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
class FhirTransformSchema(
    elements: MutableList<FhirTransformSchemaElement> = mutableListOf(),
    constants: SortedMap<String, String> = sortedMapOf(),
    extends: String? = null
) : ConfigSchema<FhirTransformSchemaElement>(elements = elements, constants = constants, extends = extends) {
    override fun override(overrideSchema: ConfigSchema<FhirTransformSchemaElement>):
        ConfigSchema<FhirTransformSchemaElement> =
        apply {
            check(overrideSchema is FhirTransformSchema) {
                "Child schema ${overrideSchema.name} not a FHIRTransformSchema."
            }
            super.override(overrideSchema)
        }
}

/**
 * A FHIR transform template (schema) element, the basic building block of a schema that describes a particular
 * transform for a particular resource or set of resources. [schema] and [value] are mutually exclusive, that is an
 * element can either point to a schema that eventually points to a schema with the value property set, or it can
 * specify the value to set the resource to directly.
 * @property name the name of the element
 * @property condition a FHIR path condition to evaluate. If false then the element is ignored.
 * @property required true if the element must have a value
 * @property schema relative path to template .yml file that will apply each resource to.
 *  When referencing another schema, during schema loading, the schemas will be MERGED, and an error will be thrown
 *  if the schemas contain elements with identical names. This property is mutually exclusive with
 *  [value] and [bundleProperty] parameters.
 * @property schemaRef the reference to the loaded child schema
 * @property resource a FHIR path that evaluates to a FHIR resource
 * @property value The value to set the bundleProperty to value is a yml list type, but only the first value
 *  in the list will get used. See `ConfigSchemaReader.getValue`. Mutually exclusive with [schemaRef] parameter.
 * @property resourceIndex To be used alongside [schema], an arbitrary variable name to be used in the FHIR path
 *  expressions of element properties for elements in the referenced schema. This is helpful when the [resource]
 *  expression is expected to return multiple resources. Each resource that is processed by the referenced schema
 *  will automatically have access to the defined variable and the variable will increment to match the
 *  index of the resource in the resource list.
 * @property constants element level constants
 * @property valueSet a collection of key-value pairs used to convert the value property
 * @property debug log debug information for the element
 * @property bundleProperty The FHIR path to the property of the resource returned by the [resource] parameter.
 *  Used alongside the [value] property and mutually exclusive with schema.
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