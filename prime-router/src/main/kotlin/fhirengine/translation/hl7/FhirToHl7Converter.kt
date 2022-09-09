package gov.cdc.prime.router.fhirengine.translation.hl7

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
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
    internal fun processSchema(
        schema: ConfigSchema,
        focusResource: Base,
        context: CustomContext = CustomContext(bundle)
    ) {
        logger.debug("Processing schema: ${schema.name} with ${schema.elements.size} elements")
        // Add any schema level constants to the context
        // We need to create a new context, so constants exist only within their specific schema tree
        val schemaContext = CustomContext.addConstants(schema.constants, context)
        schema.elements.forEach { element ->
            processElement(element, focusResource, schemaContext)
        }
    }

    /**
     * Generate HL7 data for an [element] starting at the [focusResource] in the bundle.
     */
    internal fun processElement(element: ConfigSchemaElement, focusResource: Base, context: CustomContext) {
        logger.trace("Started processing of element ${element.name}...")
        // Add any element level constants to the context
        val elementContext = CustomContext.addConstants(element.constants, context)
        var debugMsg = "Processed element name: ${element.name}, required: ${element.required}, "

        // First we need to resolve a resource value if available.
        val focusResources = getFocusResources(element, focusResource, elementContext)
        if (focusResources.isEmpty() && element.required == true) {
            // There are no sources to parse, but the element was required
            throw RequiredElementException(element)
        } else if (focusResources.isEmpty()) debugMsg += "resource: NONE"

        focusResources.forEachIndexed { index, singleFocusResource ->
            if (canEvaluate(element, singleFocusResource, elementContext)) {
                when {
                    // If this is a schema then process it.
                    element.schemaRef != null -> {
                        // Schema references can have new index references
                        val indexContext = if (element.resourceIndex.isNullOrBlank()) elementContext
                        else CustomContext.addConstant(element.resourceIndex!!, index.toString(), elementContext)
                        processSchema(element.schemaRef!!, singleFocusResource, indexContext)
                    }

                    // A value
                    element.value.isNotEmpty() && element.hl7Spec.isNotEmpty() -> {
                        val value = getValue(element, singleFocusResource, elementContext)
                        setHl7Value(element, value, context)
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
        }
        // Only log for elements that require values
        if (element.schemaRef == null) logger.debug(debugMsg)
        logger.trace("End processing of element ${element.name}.")
    }

    /**
     * Get the first valid string from the list of values specified in the schema for a given [element] starting
     * at the [focusResource].
     * @return the value for the the element or an empty string if no value found
     */
    internal fun getValue(element: ConfigSchemaElement, focusResource: Base, context: CustomContext): String {
        var retVal = ""
        run findValue@{
            element.value.forEach {
                val value = if (it.isBlank()) ""
                else FhirPathUtils.evaluateString(context, focusResource, bundle, it)
                if (value.isNotBlank()) {
                    retVal = value
                    return@findValue
                }
            }
        }
        return retVal
    }

    /**
     * Determine the focus resource for an [element] using the [previousFocusResource].
     * @return a list of focus resources containing at least one resource.  Multiple resources are returned for collections
     */
    internal fun getFocusResources(
        element: ConfigSchemaElement,
        previousFocusResource: Base,
        context: CustomContext
    ): List<Base> {
        val resourceList = if (element.resource == null) {
            listOf(previousFocusResource)
        } else {
            val evaluatedResource = FhirPathUtils
                .evaluate(context, previousFocusResource, bundle, element.resource!!)
            evaluatedResource
        }

        return resourceList
    }

    /**
     * Test if an [element] can be evaluated based on the [element]'s condition.  Use the [focusResource] to evaluate the
     * condition expression.
     * @return true if the condition expression evaluates to a boolean or if the condition expression is empty, false otherwise
     */
    internal fun canEvaluate(
        element: ConfigSchemaElement,
        focusResource: Base,
        context: CustomContext
    ): Boolean {
        return element.condition?.let {
            try {
                FhirPathUtils.evaluateCondition(context, focusResource, bundle, it)
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
    internal fun setHl7Value(element: ConfigSchemaElement, value: String, context: CustomContext) {
        if (value.isBlank() && element.required == true) {
            // The value is empty, but the element was required
            throw RequiredElementException(element)
        }
        element.hl7Spec.forEach { rawHl7Spec ->
            val resolvedHl7Spec = ConstantSubstitutor.replace(rawHl7Spec, context)
            try {
                terser!!.set(resolvedHl7Spec, value)
                logger.debug("Set HL7 $resolvedHl7Spec = $value")
            } catch (e: HL7Exception) {
                val msg = "Could not set HL7 value for spec $resolvedHl7Spec for element ${element.name}"
                if (strict) {
                    logger.error(msg, e)
                    throw HL7ConversionException(msg, e)
                } else logger.warn(msg, e)
            } catch (e: IllegalArgumentException) {
                val msg = "Invalid Hl7 spec $resolvedHl7Spec specified in schema for element ${element.name}"
                if (strict) {
                    logger.error(msg, e)
                    throw SchemaException(msg, e)
                } else logger.warn(msg, e)
            } catch (e: Exception) {
                if (strict) {
                    logger.error(e)
                    throw HL7ConversionException(e.message ?: "", e)
                } else logger.warn(e.message ?: "", e)
            }
        }
    }
}