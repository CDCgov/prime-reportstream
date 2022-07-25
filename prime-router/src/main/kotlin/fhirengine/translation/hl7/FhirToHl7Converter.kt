package gov.cdc.prime.router.fhirengine.translation.hl7

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle

/**
 * Convert a FHIR [bundle] to an HL7 message using the [schemaRef] to perform the conversion.
 * The converter will error out if [strict] is set to true and there is an error during the conversion.  if [strict]
 * is set to false (the default) then any conversion errors are logged as a warning.  Note [strict] does not affect
 * the schema validation process.
 * @property terser the terser to use for building the HL7 message (use for dependency injection)
 */
class FhirToHl7Converter(
    private val bundle: Bundle,
    private val schemaRef: ConfigSchema,
    private val strict: Boolean = false,
    private var terser: Terser? = null
) : Logging {
    /**
     * Convert a FHIR [bundle] to an HL7 message using the [schema] in the [schemaFolder] location to perform the conversion.
     * The converter will error out if [strict] is set to true and there is an error during the conversion.  if [strict]
     * is set to false (the default) then any conversion errors are logged as a warning.  Note [strict] does not affect
     * the schema validation process.
     * @property terser the terser to use for building the HL7 message (use for dependency injection)
     */
    constructor(
        bundle: Bundle,
        schema: String,
        schemaFolder: String,
        strict: Boolean = false,
        terser: Terser? = null
    ) : this(bundle, ConfigSchemaReader.fromFile(schema, schemaFolder), strict, terser)

    /**
     * Convert the given bundle to an HL7 message.
     * @return the HL7 message
     */
    fun convert(): Message {
        // Sanity check, but the schema is assumed good to go here
        check(!schemaRef.hl7Type.isNullOrBlank())
        check(!schemaRef.hl7Version.isNullOrBlank())
        val message = HL7Utils.SupportedMessages.getMessageInstance(schemaRef.hl7Type!!, schemaRef.hl7Version!!)

        // Sanity check, but at this point we know we have a good schema
        check(message != null)
        terser = Terser(message)
        processSchema(schemaRef, bundle)
        return message
    }

    /**
     * Generate HL7 data for the elements for the given [schema] starting at the [focusResource] in the bundle.
     */
    internal fun processSchema(schema: ConfigSchema, focusResource: Base) {
        logger.debug("Processing schema: ${schema.name} with ${schema.elements.size} elements")
        schema.elements.forEach { element ->
            processElement(element, focusResource)
        }
    }

    /**
     * Generate HL7 data for an [element] starting at the [focusResource] in the bundle.
     */
    internal fun processElement(element: ConfigSchemaElement, focusResource: Base) {
        // First we need to resolve a resource value if available. This always returns at least one resource
        getFocusResources(element, focusResource).forEach { singleFocusResource ->
            // TODO NEED TO HANDLE REPEATING SEGMENTS AS THEY WILL NEED AN INDEX REFERENCE

            var debugMsg = "Processing element name: ${element.name}, required: ${element.required}, "
            if (canEvaluate(element, singleFocusResource)) {
                when {
                    // If this is a schema then process it.
                    element.schemaRef != null -> {
                        processSchema(element.schemaRef!!, singleFocusResource)
                    }

                    // A value
                    element.valueExpressions.isNotEmpty() && element.hl7Spec.isNotEmpty() -> {
                        // TODO NEED TO PROVIDE AN APPCONTEXT WITH CUSTOM SET OF VARIABLES INSTEAD OF ""
                        val value = getValue(element, singleFocusResource)
                        setHl7Value(element, value)
                        debugMsg += "condition: true, resourceType: ${singleFocusResource.fhirType()}, " +
                            "value: $value, hl7Spec: ${element.hl7Spec}"
                    }

                    // This should never happen as the schema was validated prior to getting here
                    else -> throw IllegalStateException()
                }
            } else if (element.required == true) {
                // The condition was not met, but the element was required
                throw RequiredElementException(element)
            } else {
                debugMsg += "condition: false, resourceType: ${singleFocusResource.fhirType()}"
            }
            // Only log for elements that require values
            if (element.schemaRef == null) logger.debug(debugMsg)
        }
    }

    /**
     * Get the first valid string from the list of values specified in the schema for a given [element] starting
     * at the [focusResource].
     * @return the value for the the element or an empty string if no value found
     */
    internal fun getValue(element: ConfigSchemaElement, focusResource: Base): String {
        var retVal = ""
        element.valueExpressions.forEach {
            val value = FhirPathUtils.evaluateString("", focusResource, bundle, it)
            if (value.isNotBlank()) {
                retVal = value
                return@forEach
            }
        }
        return retVal
    }

    /**
     * Determine the focus resource for an [element] using the [previousFocusResource].
     * @return a list of focus resources containing at least one resource.  Multiple resources are returned for collections
     */
    internal fun getFocusResources(element: ConfigSchemaElement, previousFocusResource: Base): List<Base> {
        val resourceList = if (element.resourceExpression == null) {
            listOf(previousFocusResource)
        } else {
            val evaluatedResource = FhirPathUtils
                .evaluate("", previousFocusResource, bundle, element.resourceExpression!!)
            evaluatedResource ?: emptyList()
        }

        // This must be resources
        resourceList.forEach {
            if (it.isPrimitive)
                throw SchemaException("Invalid FHIR path ${element.resource}: must evaluate to a FHIR resource.")
        }
        return resourceList
    }

    /**
     * Test if an [element] can be evaluated based on the [element]'s condition.  Use the [focusResource] to evaluate the
     * condition expression.
     * @return true if the condition expression evaluates to a boolean or if the condition expression is empty, false otherwise
     */
    internal fun canEvaluate(element: ConfigSchemaElement, focusResource: Base): Boolean {
        return element.conditionExpression?.let {
            try {
                FhirPathUtils.evaluateCondition("", focusResource, bundle, it)
            } catch (e: SchemaException) {
                logger.warn(
                    "Condition for element ${element.name} did not evaluate to a boolean type, " +
                        "so the condition failed."
                )
                false
            }
        } ?: true
    }

    /**
     * Set the [value] an [element]'s HL7 spec.
     */
    internal fun setHl7Value(element: ConfigSchemaElement, value: String) {
        if (value.isBlank() && element.required == true) {
            // The value is empty, but the element was required
            throw RequiredElementException(element)
        }
        element.hl7Spec.forEach {
            try {
                terser!!.set(it, value)
            } catch (e: HL7Exception) {
                val msg = "Could not set HL7 value for spec $it for element ${element.name}"
                if (strict) {
                    logger.error(msg, e)
                    throw HL7ConversionException(msg, e)
                } else logger.warn(msg, e)
            } catch (e: IllegalArgumentException) {
                val msg = "Invalid Hl7 spec $it specified in schema for element ${element.name}"
                if (strict) {
                    logger.error(msg, e)
                    throw SchemaException(msg, e)
                } else logger.warn(msg, e)
            }
        }
    }
}