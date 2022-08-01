package gov.cdc.prime.router.fhirengine.translation.hl7.utils

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
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.utils.FHIRPathEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utilities to handle FHIR Path parsing.
 */
object FhirPathUtils : Logging {
    /**
     * The FHIR path engine.
     */
    private val defaultPathEngine = FHIRPathEngine(SimpleWorkerContext())

    /**
     * The HL7 DTM format
     */
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMddHHmmss.SSSSxxxx")

    /**
     * The HL7 TM format. We are converting from a FHIR TimeType which does not include a time zone.
     * HL7 TM can include a timezone but none will be generated given the FHIR restriction.
     */
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss.SSSS")

    /**
     * The HL7 DTM format without time.
     */
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd")

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
     * Gets a FHIR base resource from the given [expressionNode] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     */
    fun evaluate(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expressionNode: ExpressionNode
    ): List<Base> {
        val resources = defaultPathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
        return resources.map {
            if (it.hasType("Reference") && (it as Reference).resource != null) {
                it.resource as Base
            } else it
        }
    }

    /**
     * Gets a boolean result from the given [expressionNode] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     * Note that if the [expressionNode] does not evaluate to a boolean then the result is false.
     * @return true if the expression evaluates to true, otherwise false
     * @throws SchemaException if the FHIR path does not evaluate to a boolean type
     */
    fun evaluateCondition(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expressionNode: ExpressionNode
    ): Boolean {
        val value = defaultPathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
        return if (value.size == 1 && value[0].isBooleanPrimitive) (value[0] as BooleanType).value
        else {
            throw SchemaException("Condition did not evaluate to a boolean type")
        }
    }

    /**
     * Gets a string result from the given [expressionNode] using [bundle] and starting from a specific [focusResource].
     * [focusResource] can be the same as [bundle] when starting from the root.
     * [appContext] provides custom context (e.g. variables) used for the evaluation.
     * Note that if the [expressionNode] does not evaluate to a value that can be converted to a string then the return
     * value will be an empty string.
     * @return a string with the value from the expression, or an empty string
     */
    fun evaluateString(
        appContext: CustomContext?,
        focusResource: Base,
        bundle: Bundle,
        expressionNode: ExpressionNode
    ): String {
        val evaluated = defaultPathEngine.evaluate(appContext, focusResource, bundle, bundle, expressionNode)
        return when {
            // If we couldn't evaluate the path we should return an empty string
            evaluated.isEmpty() -> ""

            evaluated.size > 1 -> {
                val msg = "Could not evaluate multiple results: $evaluated for path: $expressionNode"
                logger.error(msg)
                throw SchemaException(msg)
            }
            !evaluated[0].isPrimitive -> {
                val msg = "Could not evaluate path: $expressionNode to primitive. ${evaluated[0]} is not a primitive."
                logger.error(msg)
                throw HL7ConversionException(msg)
            }
            // InstantType and DateTimeType are both subclasses of BaseDateTime and can use the same helper
            evaluated[0] is InstantType || evaluated[0] is DateTimeType -> {
                convertDateTimeToHL7(evaluated[0] as BaseDateTimeType)
            }
            evaluated[0] is DateType -> convertDateToHL7(evaluated[0] as DateType)

            evaluated[0] is TimeType -> convertTimeToHL7(evaluated[0] as TimeType)

            else -> {
                try {
                    defaultPathEngine.convertToString(evaluated[0])
                } catch (e: Exception) {
                    val msg = "Could not parse ${evaluated[0]} to string."
                    logger.error(msg, e)
                    throw HL7ConversionException(msg, e)
                }
            }
        }
    }

    /**
     * Convert a FHIR [dateTime] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateTimeToHL7(dateTime: BaseDateTimeType): String {
        val offsetDateTime: OffsetDateTime = OffsetDateTime.ofInstant(
            dateTime.value.toInstant(), dateTime.timeZone.toZoneId()
        )

        return offsetDateTime.format(dateTimeFormatter)
    }

    /**
     * Convert a FHIR [timeType] to the format required by HL7
     * @return the converted HL7 TM
     */
    fun convertTimeToHL7(timeType: TimeType): String {
        try {
            val localTime: LocalTime = LocalTime.parse(timeType.valueAsString)
            return localTime.format(timeFormatter)
        } catch (e: Exception) {
            val msg = "Could not parse time: $timeType to LocalTime."
            logger.error(msg, e)
            throw HL7ConversionException(msg, e)
        }
    }

    /**
     * Convert a FHIR [dateType] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateToHL7(dateType: DateType): String {
        // Fake Zone ID will be truncated in the format
        val localDate: LocalDate = LocalDate.ofInstant(dateType.value.toInstant(), ZoneId.of("GMT"))

        return localDate.format(dateFormatter)
    }
}