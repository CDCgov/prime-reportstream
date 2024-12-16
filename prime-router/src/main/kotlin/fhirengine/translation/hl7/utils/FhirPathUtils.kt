package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import ca.uhn.hl7v2.model.v251.datatype.DT
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.fhirpath.ExpressionNode
import org.hl7.fhir.r4.fhirpath.FHIRLexer
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.TimeType
import java.time.DateTimeException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utilities to handle FHIR Path parsing.
 */
object FhirPathUtils : Logging {

    private val fhirContext = FhirContext.forR4()

    /**
     * The FHIR path engine
     *
     * For each call to the functions in this object, the path engine evaluation context is being
     * set to support additional constants outside the base specification.
     *
     * Be very careful that you set this to the appropriate value for your use case!
     *
     * ex: pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
     *
     * TODO: Think about changing this pattern to avoid future bugs if a new function was written incorrectly
     */
    val pathEngine = FHIRPathEngine(HapiWorkerContext(fhirContext, fhirContext.validationSupport))

    /**
     * The HL7 time format. We are converting from a FHIR TimeType which does not include a time zone.
     * Note that HL7 TM can include a timezone, but timezone is not used in FHIR.
     */
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss.SSSS")

    init {
        pathEngine.hostServices = FhirPathCustomResolver()
    }

    /**
     * Parse a FHIR path from a [fhirPath] string.  This will also provide some format validation.
     * @return the validated FHIR path
     * @throws FHIRLexerException if the path is invalid
     */
    fun parsePath(fhirPath: String?): ExpressionNode? {
        return if (fhirPath.isNullOrBlank()) {
            null
        } else {
            pathEngine.parse(fhirPath)
        }
    }

    /**
     * Is the provided path a valid FHIR path given the evaluation context?
     */
    fun validatePath(path: String, evaluationContext: FHIRPathEngine.IEvaluationContext): Boolean {
        return withEvaluationContext(evaluationContext) {
            runCatching { parsePath(path) }.isSuccess
        }
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
        expression: String,
    ): List<Base> {
        val retVal = try {
            pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
            val expressionNode = parsePath(expression)
            if (expressionNode == null) {
                emptyList()
            } else {
                pathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
            }
        } catch (e: FHIRLexer.FHIRLexerException) {
            logger.error("${e.message}: Syntax error in FHIR Path $expression.")
            emptyList()
        } catch (e: IndexOutOfBoundsException) {
            // This happens when a non-string value is given to an extension field.
            logger.error("${e.javaClass.name}: FHIR path could not find a specified field in $expression.")
            emptyList()
        }
        logger.trace("Evaluated '$expression' to '$retVal'")
        return retVal
    }

    /**
     * Gets a boolean result from the given [expression] using [rootResource], [contextResource] (which in most cases is
     * the resource the schema is being evaluated against) and starting from a specific [focusResource].
     * [focusResource] can be the same as [rootResource] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     * Note that if the [expression] does not evaluate to a boolean then the result is false.
     * @return true if the expression evaluates to true, otherwise false
     * @throws SchemaException if the FHIR path does not evaluate to a boolean type or fails to evaluate
     */
    fun evaluateCondition(
        appContext: CustomContext?,
        focusResource: Base,
        contextResource: Base,
        rootResource: Bundle,
        expression: String,
    ): Boolean {
        val retVal = try {
            pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
            val expressionNode = parsePath(expression)
            val value = if (expressionNode == null) {
                emptyList()
            } else {
                pathEngine.evaluate(appContext, focusResource, rootResource, contextResource, expressionNode)
            }
            if (value.size == 1 && value[0].isBooleanPrimitive) {
                (value[0] as BooleanType).value
            } else if (value.isEmpty()) {
                // The FHIR utilities that test for booleans only return one if the resource exists
                // if the resource does not exist, they return []
                // for the purposes of the evaluating a schema condition that is the same as being false
                false
            } else {
                throw SchemaException("FHIR Path expression did not evaluate to a boolean type: $expression")
            }
        } catch (e: Exception) {
            val msg = when (e) {
                is FHIRLexer.FHIRLexerException -> "Syntax error in FHIR Path expression $expression"
                is SchemaException -> e.message.toString()
                else ->
                    "Unknown error while evaluating FHIR Path expression $expression for condition. " +
                        "Setting value of condition to false."
            }
            logger.error(msg, e)
            throw SchemaException(msg, e)
        }
        logger.trace("Evaluated condition '$expression' to '$retVal'")
        return retVal
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
        expression: String,
        element: ConverterSchemaElement? = null,
        constantSubstitutor: ConstantSubstitutor? = null,
    ): String {
        pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
        val expressionNode = parsePath(expression)
        val evaluated = if (expressionNode == null) {
            emptyList()
        } else {
            pathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
        }
        return when {
            // If we couldn't evaluate the path we should return an empty string
            evaluated.isEmpty() -> ""

            evaluated.size > 1 -> {
                val msg = "Could convert to string multiple FHIR path results for path: $expression"
                logger.error(msg)
                throw SchemaException(msg)
            }

            // Must be a primitive to get a value
            !evaluated[0].isPrimitive -> {
                val msg = "Could not evaluate path $expression to string as it is not a primitive."
                logger.error(msg)
                throw SchemaException(msg)
            }

            // InstantType and DateTimeType are both subclasses of BaseDateTime and can use the same helper
            evaluated[0] is InstantType || evaluated[0] is DateTimeType -> {
                // There are two translation functions to handle datetime formatting.
                // If there is a custom translation function, we will call to the function.
                // Otherwise, we use our old HL7TranslationFunctions to handle the dataTime formatting.
                if (appContext?.config is HL7TranslationConfig && appContext.translationFunctions != null) {
                    appContext.translationFunctions.convertDateTimeToHL7(
                        evaluated[0] as BaseDateTimeType,
                        appContext,
                        element,
                        constantSubstitutor
                    )
                } else {
                    Hl7TranslationFunctions().convertDateTimeToHL7(
                        evaluated[0] as BaseDateTimeType,
                        appContext,
                        element,
                        constantSubstitutor
                    )
                }
            }

            evaluated[0] is DateType -> convertDateToHL7(evaluated[0] as DateType)

            evaluated[0] is TimeType -> convertTimeToHL7(evaluated[0] as TimeType)

            // Use the string representation of the value for any other types.
            else -> pathEngine.convertToString(evaluated[0])
        }
    }

    /**
     * Convert a FHIR [time] to the format required by HL7
     * @return the converted HL7 TM
     */
    fun convertTimeToHL7(time: TimeType): String {
        try {
            val localTime: LocalTime = LocalTime.parse(time.valueAsString)
            return localTime.format(timeFormatter)
        } catch (e: DateTimeParseException) {
            val msg = "Could not parse time $time to LocalTime."
            logger.error(msg, e)
            throw HL7ConversionException(msg, e)
        } catch (e: DateTimeException) {
            val msg = "Could not format time $time to LocalTime."
            logger.error(msg, e)
            throw HL7ConversionException(msg, e)
        }
    }

    /**
     * Convert a FHIR [date] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateToHL7(date: DateType): String {
        val hl7Date = DT(null)
        when (date.precision) {
            TemporalPrecisionEnum.YEAR -> hl7Date.setYearPrecision(date.year)
            TemporalPrecisionEnum.MONTH -> hl7Date.setYearMonthPrecision(date.year, date.month + 1)
            else -> hl7Date.setYearMonthDayPrecision(date.year, date.month + 1, date.day)
        }
        return hl7Date.toString()
    }

    /**
     * Stores the previous evaluation context in a temporary variable and then
     * runs the lambda with the new evaluation context.
     *
     * After executing the lambda, it will set the evaluation context back to the initial value.
     */
    private fun <T> withEvaluationContext(
        evaluationContext: FHIRPathEngine.IEvaluationContext,
        block: () -> T,
    ): T {
        val previousEvaluationContext = pathEngine.hostServices
        pathEngine.hostServices = evaluationContext
        return try {
            block()
        } finally {
            pathEngine.hostServices = previousEvaluationContext
        }
    }
}