package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import ca.uhn.hl7v2.model.v251.datatype.DT
import ca.uhn.hl7v2.model.v251.datatype.DTM
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.ExpressionNode
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.utils.FHIRLexer.FHIRLexerException
import org.hl7.fhir.r4.utils.FHIRPathEngine
import java.time.DateTimeException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utilities to handle FHIR Path parsing.
 */
object FhirPathUtils : Logging {
    /**
     * The FHIR path engine.
     */
    val pathEngine = FHIRPathEngine(SimpleWorkerContext())

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
     * @throws Exception if the path is invalid
     */
    fun parsePath(fhirPath: String?): ExpressionNode? {
        return if (fhirPath.isNullOrBlank()) null
        else pathEngine.parse(fhirPath)
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
        val retVal = try {
            pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
            val expressionNode = parsePath(expression)
            if (expressionNode == null) emptyList()
            else pathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
        } catch (e: Exception) {
            // This is due to a bug in at least the extension() function
            logger.error(
                "Unknown error while evaluating FHIR expression $expression. " +
                    "Returning empty resource list.",
                e
            )
            emptyList()
        }
        logger.trace("Evaluated '$expression' to '$retVal'")
        return retVal
    }

    /**
     * Gets a boolean result from the given [expression] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     * Note that if the [expression] does not evaluate to a boolean then the result is false.
     * @return true if the expression evaluates to true, otherwise false
     * @throws SchemaException if the FHIR path does not evaluate to a boolean type or fails to evaluate
     */
    fun evaluateCondition(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expression: String
    ): Boolean {
        val retVal = try {
            pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
            val expressionNode = parsePath(expression)
            val value = if (expressionNode == null) emptyList()
            else pathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
            if (value.size == 1 && value[0].isBooleanPrimitive) (value[0] as BooleanType).value
            else {
                throw SchemaException("FHIR Path expression did not evaluate to a boolean type: $expression")
            }
        } catch (e: Exception) {
            // This is due to a bug in at least the extension() function
            val msg = when (e) {
                is FHIRLexerException -> "Syntax error in FHIR Path expression $expression"
                is SchemaException -> throw e
                else ->
                    "Unknown error while evaluating FHIR Path expression $expression for condition. " +
                        "Setting value of condition to false."
            }
            logger.error(msg, e)
            throw SchemaException(msg)
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
        expression: String
    ): String {
        pathEngine.hostServices = FhirPathCustomResolver(appContext?.customFhirFunctions)
        val expressionNode = parsePath(expression)
        val evaluated = if (expressionNode == null) emptyList()
        else pathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
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
                convertDateTimeToHL7(evaluated[0] as BaseDateTimeType)
            }
            evaluated[0] is DateType -> convertDateToHL7(evaluated[0] as DateType)

            evaluated[0] is TimeType -> convertTimeToHL7(evaluated[0] as TimeType)

            // Use the string representation of the value for any other types.
            else -> pathEngine.convertToString(evaluated[0])
        }
    }

    /**
     * Convert a FHIR [dateTime] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateTimeToHL7(dateTime: BaseDateTimeType): String {
        /**
         * Set the timezone for an [hl7DateTime] if a timezone was specified.
         * @return the updated [hl7DateTime] object
         */
        fun setTimezone(hl7DateTime: DTM): DTM {
            dateTime.timeZone?.let {
                // This is strange way to set the timezone offset, but it is an integer with the leftmost two digits as the hour
                // and the rightmost two digits as minutes (e.g. -0400)
                var offset = dateTime.timeZone.rawOffset
                if (dateTime.timeZone.useDaylightTime()) {
                    offset = dateTime.timeZone.rawOffset + dateTime.timeZone.dstSavings
                }
                val hour = offset / 1000 / 60 / 60
                val min = offset / 1000 / 60 % 60
                hl7DateTime.setOffset(hour * 100 + min)
            }
            return hl7DateTime
        }

        val hl7DateTime = DTM(null)

        return when (dateTime.precision) {
            TemporalPrecisionEnum.YEAR -> "%d".format(dateTime.year)

            TemporalPrecisionEnum.MONTH -> "%d%02d".format(dateTime.year, dateTime.month + 1)

            TemporalPrecisionEnum.DAY -> "%d%02d%02d".format(dateTime.year, dateTime.month + 1, dateTime.day)

            // Note hour precision is not supported by the FHIR data type

            TemporalPrecisionEnum.MINUTE -> {
                hl7DateTime.setDateMinutePrecision(
                    dateTime.year, dateTime.month + 1, dateTime.day,
                    dateTime.hour, dateTime.minute
                )
                setTimezone(hl7DateTime).toString()
            }

            else -> {
                var secs = dateTime.second.toFloat()
//                TODO: There's no way to turn this off at the moment.
//                 Need to add support to configure Date precision.
//                 Ticket: https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/8694

//                if (dateTime.nanos != null) secs += dateTime.nanos.toFloat() / 1000000000
                hl7DateTime.setDateSecondPrecision(
                    dateTime.year, dateTime.month + 1, dateTime.day, dateTime.hour, dateTime.minute,
                    secs
                )
                setTimezone(hl7DateTime).toString()
            }
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
}