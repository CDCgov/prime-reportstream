package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import ca.uhn.hl7v2.model.v251.datatype.DTM
import ca.uhn.hl7v2.util.Terser
import org.hl7.fhir.r4.model.BaseDateTimeType

/**
 * This interface contains the required method signatures required to implement FHIR translation
 * functions. It contains the default dateTime conversion function.
 */
interface TranslationFunctions {

    /**
     * Convert a FHIR [dateTime] to the format required by HL7
     * @return the converted HL7 DTM
     */
    fun convertDateTimeToHL7(dateTime: BaseDateTimeType, appContext: CustomContext? = null): String

    fun maybeTruncateHL7Field(
        value: String,
        hl7FieldPath: String,
        terser: Terser,
        appContext: CustomContext?
    ): String
}

/**
 * This is the default implementation of TranslationFunctions.
 *
 * It is able to overriden for more customized behavior
 */
open class Hl7TranslationFunctions : TranslationFunctions {

    override fun convertDateTimeToHL7(dateTime: BaseDateTimeType, appContext: CustomContext?): String {
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
                hl7DateTime.setDateSecondPrecision(
                    dateTime.year, dateTime.month + 1, dateTime.day, dateTime.hour, dateTime.minute,
                    secs
                )
                setTimezone(hl7DateTime).toString()
            }
        }
    }

    /**
     * Default behavior is to not truncate at all and just passthrough the value
     */
    override fun maybeTruncateHL7Field(
        value: String,
        hl7FieldPath: String,
        terser: Terser,
        appContext: CustomContext?
    ): String {
        return value
    }
}