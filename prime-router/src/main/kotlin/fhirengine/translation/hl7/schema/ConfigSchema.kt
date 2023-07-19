package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetCollection
import java.util.SortedMap

/**
 * A schema.
 * @property name the schema name
 * @property elements the elements for the schema
 * @property constants schema level constants
 * @property extends the name of a schema that this schema extends
 */
@JsonIgnoreProperties
abstract class ConfigSchema<T : ConfigSchemaElement>(
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

    val duplicateElements: Map<String?, Int>
        get() = (
            elements.filter { it.name != null } +
                elements.flatMap { it.schemaRef?.elements ?: emptyList() }
            ).groupingBy { it.name }.eachCount().filter { it.value > 1 }

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
        this.constants.putAll(childSchema.constants)
        this.name = childSchema.name
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
 */
@JsonIgnoreProperties
abstract class ConfigSchemaElement(
    var name: String? = null,
    var condition: String? = null,
    var required: Boolean? = null,
    var schema: String? = null,
    var schemaRef: ConfigSchema<*>? = null,
    var resource: String? = null,
    var value: List<String>? = null,
    var resourceIndex: String? = null,
    var constants: SortedMap<String, String> = sortedMapOf(),
    var valueSet: ValueSetCollection? = null,
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
            !schema.isNullOrBlank() && !value.isNullOrEmpty() ->
                addError("Schema property cannot be used with the value property")
            !schema.isNullOrBlank() && (valueSet != null) ->
                addError("Schema property cannot be used with the valueSet property")
            schema.isNullOrBlank() && value.isNullOrEmpty() ->
                addError("Value property is required when not using a schema")
        }

        // value sets need a value to be...set
        if (valueSet != null && value.isNullOrEmpty()) {
            addError("Value property is required when using a value set")
        }

        // value set keys and values cannot be null
        if (valueSet?.toSortedMap()?.keys?.any { it == null } == true ||
            valueSet?.toSortedMap()?.values?.any { it == null } == true
        ) {
            addError("Value sets cannot contain null values")
        }

        if (!schema.isNullOrBlank() && schemaRef == null) {
            addError("Missing schema reference for '$schema'")
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
        overwritingElement.value?.let { this.value = overwritingElement.value }
        overwritingElement.valueSet?.let { this.valueSet = overwritingElement.valueSet }
        if (overwritingElement.debug) this.debug = overwritingElement.debug
        if (overwritingElement.constants.isNotEmpty()) this.constants = overwritingElement.constants
    }
}