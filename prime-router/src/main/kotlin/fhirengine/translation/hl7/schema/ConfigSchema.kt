package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import java.util.SortedMap

/**
 * A schema.
 * @property name the schema name
 * @property hl7Type the HL7 message type for the output.  Only allowed at the top level schema
 * @property hl7Version the HL7 message version for the output.  Only allowed at the top level schema
 * @property elements the elements for the schema
 * @property constants schema level constants
 */
@JsonIgnoreProperties
data class ConfigSchema(
    var hl7Type: String? = null,
    var hl7Version: String? = null,
    var elements: MutableList<ConfigSchemaElement> = mutableListOf(),
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
     * @return a list of validation errors, or an empty list if no errors
     */
    internal fun validate(isChildSchema: Boolean = false): List<String> {
        val validationErrors = mutableListOf<String>()

        /**
         * Add an error [msg] to the list of errors.
         */
        fun addError(msg: String) {
            validationErrors.add("Schema $name: $msg")
        }

        // hl7Type or hl7Version is only allowed at the top level.
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

            // Do we support the provided HL7 type and version?
            if (hl7Version != null && hl7Type != null) {
                if (!HL7Utils.SupportedMessages.supports(hl7Type!!, hl7Version!!)) {
                    addError(
                        "Schema unsupported hl7 type and version. Must be one of: " +
                            HL7Utils.SupportedMessages.getSupportedListAsString()
                    )
                }
            }
        }

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
    fun merge(childSchema: ConfigSchema) = apply {
        childSchema.hl7Version?.let { this.hl7Version = childSchema.hl7Version }
        childSchema.hl7Type?.let { this.hl7Type = childSchema.hl7Type }
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
        var elementsInSchema = elements.filter { elementName == it.name }

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
 * @property debug log debug information for the element
 */
@JsonIgnoreProperties
data class ConfigSchemaElement(
    var name: String? = null,
    var condition: String? = null,
    var required: Boolean? = null,
    var schema: String? = null,
    var schemaRef: ConfigSchema? = null,
    var resource: String? = null,
    var value: List<String> = emptyList(),
    var hl7Spec: List<String> = emptyList(),
    var resourceIndex: String? = null,
    var constants: SortedMap<String, String> = sortedMapOf(),
    var valueSet: SortedMap<String, String> = sortedMapOf(),
    var debug: Boolean = false
) {
    /**
     * Validate the element.
     * @return a list of validation errors, or an empty list if no errors
     */
    internal fun validate(): List<String> {
        val validationErrors = mutableListOf<String>()

        /**
         * Add an error [msg] to the list of errors.
         */
        fun addError(msg: String) {
            validationErrors.add("[$name]: $msg")
        }

        when {
            resource.isNullOrBlank() && !resourceIndex.isNullOrBlank() ->
                addError("Resource property is required to use the resourceIndex property")
            schema.isNullOrBlank() && !resourceIndex.isNullOrBlank() ->
                addError("Schema property is required to use the resourceIndex property")
        }

        // Hl7spec, value and valueSet cannot be used with schema.
        when {
            !schema.isNullOrBlank() && (hl7Spec.isNotEmpty() || value.isNotEmpty() || valueSet.isNotEmpty()) ->
                addError("Schema property cannot be used with hl7Spec, value or valueSet properties")
            schema.isNullOrBlank() && hl7Spec.isEmpty() ->
                addError("Hl7Spec property is required when not using a schema")
            schema.isNullOrBlank() && value.isEmpty() ->
                addError("Value property is required when not using a schema")
        }

        // value sets need a value to be...set
        when {
            valueSet.isNotEmpty() && value.isEmpty() ->
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
        return validationErrors
    }

    /**
     * Merge an [overwritingElement] into this element, overwriting only those properties that have values.
     * @return the reference to the element
     */
    fun merge(overwritingElement: ConfigSchemaElement) = apply {
        overwritingElement.condition?.let { this.condition = overwritingElement.condition }
        overwritingElement.required?.let { this.required = overwritingElement.required }
        overwritingElement.schema?.let { this.schema = overwritingElement.schema }
        overwritingElement.schemaRef?.let { this.schemaRef = overwritingElement.schemaRef }
        overwritingElement.resource?.let { this.resource = overwritingElement.resource }
        overwritingElement.resourceIndex?.let { this.resourceIndex = overwritingElement.resourceIndex }
        if (overwritingElement.value.isNotEmpty()) this.value = overwritingElement.value
        if (overwritingElement.constants.isNotEmpty()) this.constants = overwritingElement.constants
        if (overwritingElement.hl7Spec.isNotEmpty()) this.hl7Spec = overwritingElement.hl7Spec
    }
}