package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import java.util.SortedMap

/**
 * A schema.
 * @property name the schema name
 * @property elements the elements for the schema
 * @property constants schema level constants
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
sealed class ConfigSchema<T : ConfigSchemaElement>(
    var elements: MutableList<T> = mutableListOf(),
    var constants: SortedMap<String, String> = sortedMapOf(),
    var extends: String? = null
) {
    /**
     * Name used to identify this schema.
     * Can be set from outside this class to make it useful in context.
     * e.g. The schema's filename
     */
    var name: String? = null

    /**
     * List of the names of schemas that have led to this schema being loaded.
     */
    var ancestry: List<String> = listOf()

    /**
     * Has this schema been validated? Only used on the top level schema.
     */
    private var hasBeenValidated = false

    /**
     * Validation errors.
     */
    var errors = emptyList<String>()
        private set

    /**
     * Private property used to build list of validation errors
     */
    private var validationErrors: MutableSet<String> = mutableSetOf()

    /**
     * Add an error [msg] to the list of errors.
     */
    protected fun addError(msg: String) {
        validationErrors.add("Schema $name: $msg")
    }

    /**
     * Test if the schema and its elements (including other schema) is valid.  See [errors] property for validation
     * error messages.
     * @return true if the schema is valid, false otherwise
     */
    fun isValid(): Boolean {
        if (!hasBeenValidated) {
            errors = validate()
        }
        return errors.isEmpty()
    }

    /**
     * Validate the top level schema if [isChildSchema] is false, or a child schema if [isChildSchema] is true.
     * [validationErrors] can optionally be specified to start with a list of errors, but defaults to an empty list.
     * @return a list of validation errors, or an empty list if no errors
     */
    internal open fun validate(isChildSchema: Boolean = false): List<String> {
        // Check that all constants have a string
        constants.filterValues { it.isNullOrBlank() }.forEach { (key, _) ->
            addError("Constant '$key' does not have a value")
        }

        // Validate the schema elements.
        if (elements.isEmpty()) {
            addError("Schema elements cannot be empty")
        }
        elements.forEach { element ->
            element.validate().forEach { addError(it) }
        }

        hasBeenValidated = true
        return validationErrors.toList()
    }

    /**
     * Merge a [childSchema] into this one.
     * @return the reference to the schema
     */
    open fun merge(childSchema: ConfigSchema<T>) = apply {
        childSchema.elements.forEach { childElement ->
            // If we find the element in the schema then replace it, otherwise add it.
            if (childElement.name.isNullOrBlank()) {
                throw SchemaException("Child schema ${childSchema.name} found with element with no name.")
            }
            val elementInSchema = findElement(childElement.name!!)
            if (elementInSchema != null) {
                elementInSchema.merge(childElement)
            } else {
                this.elements.add(childElement)
            }
        }
    }

    /**
     * Find an [elementName] in this schema. This function recursively traverses the entire schema tree to find the
     * element.
     * @return the element found or null if not found
     */
    internal fun findElement(elementName: String): ConfigSchemaElement? {
        // First try to find the element at this level in the schema.
        var elementsInSchema: List<ConfigSchemaElement> = elements.filter { elementName == it.name }

        // If the element was not found in this schema level, then traverse any elements that reference a schema
        if (elementsInSchema.isEmpty()) {
            // Stay with me here: first get all the elements that are schema references, then for each of those schemas
            // then find the element in there.  Note that this is recursive.
            // Why the distinct? A schema can make references to the same schema multiple times, so you could get
            // a list of elements that are identical, so we make sure to get only those that at different.
            elementsInSchema = elements.filter { it.schemaRef != null }.mapNotNull {
                it.schemaRef?.findElement(elementName)
            }.distinct()
        }
        // Sanity check
        check(elementsInSchema.size <= 1)
        return if (elementsInSchema.isEmpty()) null else elementsInSchema[0]
    }
}

/**
 * A converter schema.
 * @property name the schema name
 * @property hl7Type the HL7 message type for the output.  Only allowed at the top level schema
 * @property hl7Version the HL7 message version for the output.  Only allowed at the top level schema
 * @property elements the elements for the schema
 * @property constants schema level constants
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
class ConverterSchema(
    var hl7Type: String? = null,
    var hl7Version: String? = null,
    elements: MutableList<ConverterSchemaElement> = mutableListOf(),
    constants: SortedMap<String, String> = sortedMapOf(),
    extends: String? = null
) : ConfigSchema<ConverterSchemaElement>(elements = elements, constants = constants, extends = extends) {
    override fun validate(isChildSchema: Boolean): List<String> {
        if (isChildSchema) {
            if (!hl7Type.isNullOrBlank()) {
                addError("Schema hl7Type can only be specified in top level schema")
            }
            if (!hl7Version.isNullOrBlank()) {
                addError("Schema hl7Version can only be specified in top level schema")
            }
        } else {
            if (hl7Type.isNullOrBlank()) {
                addError("Schema hl7Type cannot be blank")
            }
            if (hl7Version.isNullOrBlank()) {
                addError("Schema hl7Version cannot be blank")
            }
        }

        // Do we support the provided HL7 type and version?
        if (hl7Version != null && hl7Type != null) {
            if (!HL7Utils.SupportedMessages.supports(hl7Type!!, hl7Version!!)) {
                addError(
                    "Schema unsupported hl7 type and version. Must be one of: " +
                        HL7Utils.SupportedMessages.getSupportedListAsString()
                )
            }
        }

        return super.validate(isChildSchema)
    }

    override fun merge(childSchema: ConfigSchema<ConverterSchemaElement>): ConfigSchema<ConverterSchemaElement> =
        apply {
            check(childSchema is ConverterSchema) { "Child schema ${childSchema.name} not a ConverterSchema." }
            childSchema.hl7Version?.let { this.hl7Version = childSchema.hl7Version }
            childSchema.hl7Type?.let { this.hl7Type = childSchema.hl7Type }
            super.merge(childSchema)
        }
}

/**
 * A FHIR transform schema.
 * @property name the schema name
 * @property elements the elements for the schema
 * @property constants schema level constants
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
class FHIRTransformSchema(
    elements: MutableList<FHIRTransformSchemaElement> = mutableListOf(),
    constants: SortedMap<String, String> = sortedMapOf(),
    extends: String? = null
) : ConfigSchema<FHIRTransformSchemaElement>(elements = elements, constants = constants, extends = extends) {
    override fun merge(childSchema: ConfigSchema<FHIRTransformSchemaElement>):
        ConfigSchema<FHIRTransformSchemaElement> =
        apply {
            check(childSchema is FHIRTransformSchema) { "Child schema ${childSchema.name} not a FHIRTransformSchema." }
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
 * @property valueSet a list of key-value pairs used to convert the value property
 * @property debug log debug information for the element
 */
@JsonIgnoreProperties
sealed class ConfigSchemaElement(
    var name: String? = null,
    var condition: String? = null,
    var required: Boolean? = null,
    var schema: String? = null,
    var schemaRef: ConfigSchema<*>? = null,
    var resource: String? = null,
    var value: List<String> = emptyList(),
    var resourceIndex: String? = null,
    var constants: SortedMap<String, String> = sortedMapOf(),
    var valueSet: SortedMap<String, String> = sortedMapOf(),
    var debug: Boolean = false
) {
    private var validationErrors: MutableSet<String> = mutableSetOf()

    /**
     * Add an error [msg] to the list of errors.
     */
    protected fun addError(msg: String) {
        validationErrors.add("[$name]: $msg")
    }

    /**
     * Validate the element. If specified [validationErrors] will be a starting list of errors.
     * @return a list of validation errors, or an empty list if no errors
     */
    internal open fun validate(): List<String> {
        if (!resourceIndex.isNullOrBlank()) {
            when {
                resource.isNullOrBlank() ->
                    addError("Resource property is required to use the resourceIndex property")
                schema.isNullOrBlank() ->
                    addError("Schema property is required to use the resourceIndex property")
            }
        }

        when {
            !schema.isNullOrBlank() && value.isNotEmpty() ->
                addError("Schema property cannot be used with the value property")
            !schema.isNullOrBlank() && valueSet.isNotEmpty() ->
                addError("Schema property cannot be used with the valueSet property")
            schema.isNullOrBlank() && value.isEmpty() ->
                addError("Value property is required when not using a schema")
        }

        // value sets need a value to be...set
        if (valueSet.isNotEmpty() && value.isEmpty()) {
            addError("Value property is required when using a value set")
        }

        if (!schema.isNullOrBlank() && schemaRef == null) {
            addError("Missing schema reference for '$schema'")
        }

        // Check that all constants have a string
        constants.filterValues { it.isNullOrBlank() }.forEach { (key, _) ->
            addError("Constant '$key' does not have a value")
        }

        schemaRef?.let {
            validationErrors.addAll(it.validate(true))
        }
        return validationErrors.toList()
    }

    /**
     * Merge an [overwritingElement] into this element, overwriting only those properties that have values.
     * @return the reference to the element
     */
    open fun merge(overwritingElement: ConfigSchemaElement) = apply {
        overwritingElement.condition?.let { this.condition = overwritingElement.condition }
        overwritingElement.required?.let { this.required = overwritingElement.required }
        overwritingElement.schema?.let { this.schema = overwritingElement.schema }
        overwritingElement.schemaRef?.let { this.schemaRef = overwritingElement.schemaRef }
        overwritingElement.resource?.let { this.resource = overwritingElement.resource }
        overwritingElement.resourceIndex?.let { this.resourceIndex = overwritingElement.resourceIndex }
        if (overwritingElement.value.isNotEmpty()) this.value = overwritingElement.value
        if (overwritingElement.constants.isNotEmpty()) this.constants = overwritingElement.constants
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
 * @property valueSet a list of key-value pairs used to convert the value property
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
    value: List<String> = emptyList(),
    var hl7Spec: List<String> = emptyList(),
    resourceIndex: String? = null,
    constants: SortedMap<String, String> = sortedMapOf(),
    valueSet: SortedMap<String, String> = sortedMapOf(),
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
 * @property valueSet a list of key-value pairs used to convert the value property
 * @property debug log debug information for the element
 * @property bundleProperty a FHIR path denoting where to store the value
 */
@JsonIgnoreProperties
class FHIRTransformSchemaElement(
    name: String? = null,
    condition: String? = null,
    required: Boolean? = null,
    schema: String? = null,
    schemaRef: FHIRTransformSchema? = null,
    resource: String? = null,
    value: List<String> = emptyList(),
    resourceIndex: String? = null,
    constants: SortedMap<String, String> = sortedMapOf(),
    valueSet: SortedMap<String, String> = sortedMapOf(),
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
        check(overwritingElement is FHIRTransformSchemaElement) {
            "Overwriting element ${overwritingElement.name} was not a FHIRTransformSchemaElement."
        }
        overwritingElement.bundleProperty?.let { this.bundleProperty = overwritingElement.bundleProperty }
        super.merge(overwritingElement)
    }
}