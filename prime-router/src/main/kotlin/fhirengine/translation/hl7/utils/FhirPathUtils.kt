package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ExpressionNode
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * Utilities to handle FHIR Path parsing.
 */
object FhirPathUtils : Logging {
    /**
     * The FHIR path engine.
     */
    private val defaultPathEngine = FHIRPathEngine(SimpleWorkerContext())

    init {
        defaultPathEngine.hostServices = FhirPathCustomResolver()
    }

    /**
     * Parse a FHIR path from a [fhirPath] string.  This will also provide some format validation.
     * @return the validated FHIR path
     * @throws Exception if the path is invalid
     */
    fun parsePath(fhirPath: String?): ExpressionNode? {
        return if (fhirPath == null) null
        else defaultPathEngine.parse(fhirPath)
    }

    /**
     * Gets a FHIR base resource from the given [expression] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     */
    fun evaluate(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expression: String
    ): List<Base> {
        return try {
            val resources = defaultPathEngine.evaluate(appContext, focusResource, bundle, bundle, parsePath(expression))
            resources.map {
                if (it.hasType("Reference") && (it as Reference).resource != null) {
                    it.resource as Base
                } else it
            }
        } catch (e: Exception) {
            // This is due to a bug in at least the extension() function
            logger.debug(
                "Unknown error while evaluating FHIR expression $expression. " +
                    "Returning empty resource list.",
                e
            )
            emptyList()
        }
    }

    /**
     * Gets a boolean result from the given [expression] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     * Note that if the [expression] does not evaluate to a boolean then the result is false.
     * @return true if the expression evaluates to true, otherwise false
     * @throws SchemaException if the FHIR path does not evaluate to a boolean type
     */
    fun evaluateCondition(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expression: String
    ): Boolean {
        return try {
            val value = defaultPathEngine.evaluate(appContext, focusResource, bundle, bundle, parsePath(expression))
            if (value.size == 1 && value[0].isBooleanPrimitive) (value[0] as BooleanType).value
            else {
                throw SchemaException("Condition did not evaluate to a boolean type")
            }
        } catch (e: Exception) {
            // This is due to a bug in at least the extension() function
            logger.debug(
                "Unknown error while evaluating FHIR expression $expression for condition. " +
                    "Setting value of condition to false.",
                e
            )
            false
        }
    }

    /**
     * Gets a string result from the given [expression] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     * Note that if the [expression] does not evaluate to a value that can be converted to a string then the return
     * value will be an empty string.
     * @return a string with the value from the expression, or an empty string
     */
    fun evaluateString(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expression: String
    ): String {
        return defaultPathEngine.evaluateToString(appContext, focusResource, bundle, bundle, parsePath(expression))
    }
}