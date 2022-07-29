package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
        var toReturn = ""
        if (evaluated.isEmpty()) {
            // Return toReturn as is. An empty string is the appropriate representation.
        } else if (evaluated.size > 1) {
            val msg = "Could not evaluate multiple results: $evaluated for path: $expressionNode"
            logger.error(msg)
            throw IllegalStateException(msg)
        } else if (!evaluated[0].isPrimitive) {
            val msg = "Could not evaluate path: $expressionNode to primitive. ${evaluated[0]} is not a primitive."
            logger.error(msg)
            throw HL7ConversionException(msg)
        } else {
            toReturn = when (val primitive = evaluated[0]) {
                is InstantType, is DateTimeType -> convertDateTimeToHL7(primitive)
                is DateType -> convertDateToHL7(primitive)
                is TimeType -> convertTimeToHL7(primitive)
                else -> defaultPathEngine.convertToString(primitive)
            }
        }
        return toReturn
    }

    /**
     * Convert a FHIR [dateTime] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateTimeToHL7(dateTime: Base): String {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMddHHmmss.SSSSxxxx")
        val zonedDateTime: ZonedDateTime = ZonedDateTime.parse(dateTime.dateTimeValue().valueAsString)

        return zonedDateTime.format(formatter)
    }

    /**
     * Convert a FHIR [timeType] to the format required by HL7
     * @return the converted HL7 TM
     */
    fun convertTimeToHL7(timeType: TimeType): String {
        // FHIR TimeType does not include a time zone.
        // HL7 TM can include a timezone but none will be generated given the FHIR restriction.
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss.SSSS")
        val localTime: LocalTime = LocalTime.parse(timeType.valueAsString)

        return localTime.format(formatter)
    }

    /**
     * Convert a FHIR [dateType] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateToHL7(dateType: DateType): String {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd")
        val localDate: LocalDate = LocalDate.parse(dateType.valueAsString)

        return localDate.format(formatter)
    }
}