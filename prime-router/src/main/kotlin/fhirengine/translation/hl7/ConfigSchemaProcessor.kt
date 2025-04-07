package gov.cdc.prime.router.fhirengine.translation.hl7

import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.StringType

abstract class ConfigSchemaProcessor<
    Original,
    Converted,
    Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
    SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
    >(
    val schema: Schema,
    val errors: MutableList<String>,
    val warnings: MutableList<String>,
) : Logging {

    /**
     * Validates the schema the processor will use is valid given a sample input and output
     *
     * @property input the value to be converted
     * @property expectedOutput the expected output of the conversion
     * @returns Whether applying this processor against the [input] exactly matches the [expectedOutput]
     */
    fun validate(input: Original, expectedOutput: Converted): Boolean {
        val converted = process(input)
        return checkForEquality(converted, expectedOutput)
    }

    abstract fun checkForEquality(converted: Converted, expectedOutput: Converted): Boolean

    /**
     *
     * Accepts an input value and applies the schema to it returning the converted value
     *
     * @property input the value to apply the schema to
     * @return The value after applying the schema to [input]
     */
    abstract fun process(
        input: Original,
    ): Converted

    /**
     * Get the first valid value from the list of values specified in the schema for a given [element] using
     * [bundle] and [context] starting at the [focusResource].
     * @return the value for the element or null if no value found
     */
    internal fun getValue(
        element: SchemaElement,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext,
    ): Base? {
        var retVal: Base? = null
        run findValue@{
            element.value?.forEach {
                val value = if (it.isBlank()) {
                    emptyList()
                } else {
                    FhirPathUtils.evaluate(context, focusResource, bundle, it)
                }
                logger.trace("Evaluated value expression '$it' to '$value'")
                if (value.isNotEmpty()) {
                    retVal = value[0]
                    if (retVal != null && retVal!!.isPrimitive) {
                        retVal = retVal!!.copy()
                    }
                    return@findValue
                }
            }
        }

        // when valueSet is available, return mapped value or null if match isn't found
        if (retVal != null && element.valueSet != null) {
            val valStr = element.valueSet!!.getMappedValue(retVal?.primitiveValue() ?: "")
            retVal = if (valStr != null) {
                StringType(valStr)
            } else {
                null
            }
        }
        return retVal
    }

    /**
     * Determine the focus resource from [resourceStr] using [bundle] and the [previousFocusResource].
     * @return a list of focus resources containing at least one resource.  Multiple resources are returned for collections
     */
    internal fun getFocusResources(
        resourceStr: String?,
        bundle: Bundle,
        previousFocusResource: Base,
        context: CustomContext,
    ): List<Base> {
        val resourceList = if (resourceStr == null || resourceStr == "") {
            listOf(previousFocusResource)
        } else {
            val evaluatedResource = FhirPathUtils
                .evaluate(context, previousFocusResource, bundle, resourceStr)
            evaluatedResource
        }

        return resourceList
    }

    /**
     * Test if an [element] can be evaluated based on the [element]'s condition.  Use the [bundle], [schemaResource] and [focusResource] * to evaluate the condition expression.
     * @return true if the condition expression evaluates to a boolean or if the condition expression is empty, false otherwise
     */
    internal fun canEvaluate(
        element: SchemaElement,
        bundle: Bundle,
        focusResource: Base,
        schemaResource: Base,
        context: CustomContext,
    ): Boolean = element.condition?.let {
            try {
                FhirPathUtils.evaluateCondition(context, focusResource, schemaResource, bundle, it)
            } catch (e: SchemaException) {
                logger.warn(
                    "Condition for element ${element.name} did not evaluate to a boolean type, " +
                        "so the condition failed. ${e.message}"
                )
                false
            }
        } ?: true
}