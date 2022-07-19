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
 * The FHIR to HL7 converter.
 * @property bundle the source FHIR bundle
 * @property schema the name of the schema to use for the conversion
 * @property schemaFolder the location of the schema
 * @property terser the terser to use for building the HL7 message (use for dependency injection)
 */
class FhirToHl7Converter(
    private val bundle: Bundle,
    private val schema: String,
    private val schemaFolder: String,
    private var terser: Terser? = null
) : Logging {

    /**
     * Convert the given bundle to an HL7 message.
     * @return the HL7 message
     */
    fun convert(): Message {
        val schemaRef = ConfigSchemaReader.fromFile(schema, schemaFolder)

        // Sanity check, but the schema is assumed good to go here
        check(!schemaRef.hl7Type.isNullOrBlank())
        check(!schemaRef.hl7Version.isNullOrBlank())
        val message = HL7Utils.SupportedMessages.getMessageInstance(schemaRef.hl7Type!!, schemaRef.hl7Version!!)

        // Sanity check, but at this point we know we have a good schema
        check(message != null)
        terser = Terser(message)
        processSchema(bundle, schemaRef)
        return message
    }

    /**
     * Generate HL7 data for the elements for the given [schema] starting at the [focusResource] in the bundle.
     */
    internal fun processSchema(focusResource: Base, schema: ConfigSchema) {
        logger.debug("Processing schema: ${schema.name} with ${schema.elements.size} elements")
        schema.elements.forEach { element ->
            processElement(focusResource, element)
        }
    }

    /**
     * Generate HL7 data for an [element] starting at the [focusResource] in the bundle.
     */
    internal fun processElement(focusResource: Base, element: ConfigSchemaElement) {
        // First we need to resolve a resource value if available.
        getFocusResources(element, focusResource).forEach { singleFocusResource ->
            var debugMsg = "Processing element name: ${element.name}, required: ${element.required}, "
            if (canEvaluate(element, singleFocusResource)) {
                when {
                    // If this is a schema then process it.
                    element.schemaRef != null -> {
                        processSchema(singleFocusResource, element.schemaRef!!)
                    }

                    // A value
                    element.valueExpression != null && element.hl7Spec.isNotEmpty() -> {
                        val value = FhirPathUtils.evaluateString("", focusResource, bundle, element.valueExpression!!)
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
     * Determine the focus resource for an [element] using the [previousFocusResource].
     * @return a list of focus resources containing at least one resource.  Multiple resources are returned for collections
     */
    internal fun getFocusResources(element: ConfigSchemaElement, previousFocusResource: Base): List<Base> {
        val resource = if (element.resourceExpression == null) {
            listOf(previousFocusResource)
        } else {
            val evaluatedResource = FhirPathUtils
                .evaluate("", previousFocusResource, bundle, element.resourceExpression!!)
            evaluatedResource ?: emptyList<Base>()
        }
        // Sanity check, we will always have a resource even if it is the bundle.
        check(resource.isNotEmpty())
        return resource
    }

    /**
     * Test if an [element] can be evaluated based on the [element]'s condition.  Use the [focusResource] to evaluate the
     * condition expression.
     * @return true if the condition expression evaluates to a boolean or if the condition expression is empty, false otherwise
     */
    internal fun canEvaluate(element: ConfigSchemaElement, focusResource: Base): Boolean {
        return element.conditionExpression?.let {
            FhirPathUtils.evaluateCondition("", focusResource, bundle, it)
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
                val msg = "Error while setting HL7 value for element ${element.name}"
                logger.error(msg, e)
                throw HL7ConversionException(msg, e)
            } catch (e: java.lang.IllegalArgumentException) {
                val msg = "Invalid Hl7 spec specified in schema for element ${element.name}"
                logger.error(msg, e)
                throw SchemaException(msg, e)
            }
        }
    }
}