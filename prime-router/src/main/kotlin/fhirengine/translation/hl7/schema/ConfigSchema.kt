package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import org.hl7.fhir.r4.model.ExpressionNode

/**
 * A schema.
 * @property name the schema name
 * @property hl7Type the HL7 message type for the output.  Only allowed at the top level schema
 * @property hl7Version the HL7 message version for the output.  Only allowed at the top level schema
 * @property elements the elements for the schema
 */
@JsonIgnoreProperties
data class ConfigSchema(
    var name: String? = null,
    var hl7Type: String? = null,
    var hl7Version: String? = null,
    var elements: List<ConfigSchemaElement> = emptyList()
) {
    /**
     * Has this schema been validated? Only used on the top level schema.
     */
    private var isValidated = false

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
        if (!isValidated) {
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

        if (name.isNullOrBlank())
            addError("Schema name cannot be blank")

        // hl7Type or hl7Version is only allowed at the top level.
        if (isChildSchema) {
            if (!hl7Type.isNullOrBlank())
                addError("Schema hl7Type can only be specified in top level schema")
            if (!hl7Version.isNullOrBlank())
                addError("Schema hl7Version can only be specified in top level schema")
        } else {
            if (hl7Type.isNullOrBlank())
                addError("Schema hl7Type cannot be blank")
            if (hl7Version.isNullOrBlank())
                addError("Schema hl7Version cannot be blank")

            // Do we support the provided HL7 type and version?
            if (hl7Version != null && hl7Type != null) {
                if (!HL7Utils.SupportedMessages.supports(hl7Type!!, hl7Version!!))
                    addError(
                        "Schema unsupported hl7 type and version. Must be one of: " +
                            HL7Utils.SupportedMessages.getSupportedListAsString()
                    )
            }
        }

        // Validate the schema elements.
        if (elements.isEmpty())
            addError("Schema elements cannot be empty")
        elements.forEach { element ->
            element.validate().forEach { addError(it) }
        }

        isValidated = true
        return validationErrors.toList()
    }
}

/**
 * An element within a Schema.
 * @property name the name of the element
 * @property condition a FHIR path condition to evaluate. If false then the element is ignored.
 * @property conditionExpression the validated FHIR path representation of the [condition] string
 * @property required true if the element must have a value
 * @property schema the name of a child schema
 * @property schemaRef the reference to the loaded child schema
 * @property resource a FHIR path that points to a FHIR resource
 * @property resourceExpression the validated FHIR path representation of the [resource] string
 * @property value a FHIR path that points to a FHIR primitive value
 * @property valueExpression the validated FHIR path representation of the [value] string
 * @property hl7Spec an array of hl7Specs that denote the field to place a value into
 */
@JsonIgnoreProperties
data class ConfigSchemaElement(
    var name: String? = null,
    var condition: String? = null,
    var conditionExpression: ExpressionNode? = null,
    var required: Boolean? = false,
    var schema: String? = null,
    var schemaRef: ConfigSchema? = null,
    var resource: String? = null,
    var resourceExpression: ExpressionNode? = null,
    var value: String? = null,
    var valueExpression: ExpressionNode? = null,
    var hl7Spec: List<String> = emptyList()
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

        /**
         * Validate and get a FHIR [path].
         * @return the validated FHIR path.
         */
        fun getFhirPath(path: String?): ExpressionNode? {
            return if (path == null) null
            else {
                try {
                    FhirPathUtils.parsePath(path)
                } catch (e: Exception) {
                    addError("Error parsing FHIR Path")
                    null
                }
            }
        }

        if (name.isNullOrBlank()) {
            addError("Schema name cannot be blank")
        }

        // Hl7spec and value cannot be used with schema.
        when {
            !schema.isNullOrBlank() && (hl7Spec.isNotEmpty() || !value.isNullOrBlank()) ->
                addError("Schema property cannot be used with hl7Spec or value properties")
            schema.isNullOrBlank() && hl7Spec.isEmpty() ->
                addError("Hl7Spec property is required when not using a schema")
            schema.isNullOrBlank() && value.isNullOrBlank() ->
                addError("Value property is required when not using a schema")
        }

        if (!schema.isNullOrBlank() && schemaRef == null) {
            addError("Missing schema reference $schema")
        }

        // Validate the FHIR paths.
        conditionExpression = getFhirPath(condition)
        valueExpression = getFhirPath(value)
        resourceExpression = getFhirPath(resource)

        schemaRef?.validate(true)
        return validationErrors
    }
}