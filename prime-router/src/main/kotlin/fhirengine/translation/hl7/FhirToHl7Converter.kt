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

class FhirToHl7Converter(
    private val bundle: Bundle,
    private val schema: String,
    private val folder: String,
    private var terser: Terser? = null
) : Logging {

    fun convert(): Message {
        val schemaRef = ConfigSchemaReader.fromFile(schema, folder)

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

    internal fun processSchema(focusResource: Base, schema: ConfigSchema) {
        logger.debug("Processing schema: ${schema.name} with ${schema.elements.size} elements")
        schema.elements.forEach { element ->
            processElement(focusResource, element)
        }
    }

    internal fun processElement(focusResource: Base, element: ConfigSchemaElement) {
        // First we need to resolve a resource value if available.
        var traceMsg = "Processing element name: ${element.name}"
        val resourceValue = if (element.resourceExpression == null) {
            focusResource
        } else {
            val evaluatedResource = FhirPathUtils
                .evaluate("", focusResource, bundle, element.resourceExpression!!)
            // If a resource was not found then we cannot resolve this element further.  If we try to use
            // bundle or some other resource then we could get unexpected values, so the preference is to not process it.
            when {
                evaluatedResource == null || evaluatedResource.isEmpty() -> null
                evaluatedResource.size == 1 -> evaluatedResource[0]
                else -> {
                    // TODO handle collections and their index}
                    evaluatedResource.forEach {
                        println(it)
                    }
                    TODO()
                }
            }
        }

        if (resourceValue != null) {
            // Check the condition to see if we need to evaluate this element
            val needToEvaluate = element.conditionExpression?.let {
                FhirPathUtils.evaluateCondition("", resourceValue, bundle, it)
            } ?: true

            traceMsg += ", required: ${element.required}, " +
                "condition: $needToEvaluate, resourceType: ${resourceValue.fhirType()}"
            if (needToEvaluate) {
                when {
                    // If this is a schema then process it.
                    element.schemaRef != null -> {
                        processSchema(resourceValue, element.schemaRef!!)
                    }

                    // A value
                    element.valueExpression != null && element.hl7Spec.isNotEmpty() -> {
                        val value = FhirPathUtils.evaluateString("", resourceValue, bundle, element.valueExpression!!)
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
                        traceMsg += ", value: $value, hl7Spec: ${element.hl7Spec}"
                    }

                    // This should never happen as the schema was validated prior to getting here
                    else -> throw IllegalStateException()
                }
            } else if (element.required == true) {
                // The condition was not met, but the element was required
                throw RequiredElementException(element)
            }
        } else {
            // There was no resource found and one was specified
            traceMsg += ", resource: NOT FOUND - Will not process element"
        }
        logger.debug(traceMsg)
    }
}