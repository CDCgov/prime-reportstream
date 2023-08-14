package fhirengine.engine

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import ca.uhn.hl7v2.model.v251.datatype.DTM
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.TranslationFunctions
import org.hl7.fhir.r4.model.BaseDateTimeType
import java.time.ZoneId

class CustomTranslationFunctions : TranslationFunctions {
    /**
     * Convert a FHIR [dateTime] to the format required by HL7
     *
     * @return the converted HL7 DTM
     */
    override fun convertDateTimeToHL7(
        dateTime: BaseDateTimeType,
        appContext: CustomContext?
    ): String {
        /**
         * Set the timezone for an [hl7DateTime] if a timezone was specified.
         *
         * @return the updated [hl7DateTime] object
         */
        fun setTimezone(hl7DateTime: DTM): DTM {
            dateTime.timeZone?.let {
                // This is strange way to set the timezone offset, but it is an integer with the leftmost
                // two digits as the hour
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
            TemporalPrecisionEnum.DAY ->
                "%d%02d%02d".format(dateTime.year, dateTime.month + 1, dateTime.day)

            // Note hour precision is not supported by the FHIR data type

            TemporalPrecisionEnum.MINUTE -> {
                hl7DateTime.setDateMinutePrecision(
                    dateTime.year, dateTime.month + 1, dateTime.day, dateTime.hour, dateTime.minute
                )
                setTimezone(hl7DateTime).toString()
            }
            TemporalPrecisionEnum.SECOND -> {
                hl7DateTime.setDateSecondPrecision(
                    dateTime.year, dateTime.month + 1, dateTime.day, dateTime.hour, dateTime.minute,
                    dateTime.second.toFloat()
                )
                setTimezone(hl7DateTime).toString()
            }
            else -> {
                appContext?.let { convertDateTime(dateTime, it) } ?: ""
            }
        }
    }

    /**
     * This optional function allows ReportStream UP to convert dateTime to highPrecision format as in
     * Covid-19 pipline. [dateTime] - dateTime value convert [appContext] - specific application
     * context which contain receiver translate setting.
     */
    fun convertDateTime(dateTime: BaseDateTimeType, appContext: CustomContext): String? {
//        check(appContext.config is Receiver)
//        check(appContext.config.translation is Hl7Configuration)
        val dateTimeFormat = if (appContext.config is Receiver)
            appContext.config.dateTimeFormat else null
        val convertPositiveDateTimeOffsetToNegative =
            if (appContext.config != null)
                if ((appContext.config as Receiver).translation is Hl7Configuration)
                    (appContext.config.translation as Hl7Configuration).convertPositiveDateTimeOffsetToNegative
                else null
            else null
        val useHighPrecisionHeaderDateTimeFormat =
            if (appContext.config != null)
                if ((appContext.config as Receiver).translation is Hl7Configuration)
                    (appContext.config.translation as Hl7Configuration).useHighPrecisionHeaderDateTimeFormat
                else null
            else null

        val tz =
            if (dateTime.timeZone?.id != null) {
                ZoneId.of(dateTime.timeZone?.id)
            } else DateUtilities.utcZone

        return DateUtilities.formatDateForReceiver(
            DateUtilities.parseDate(dateTime.asStringValue()),
            tz,
            dateTimeFormat ?: DateUtilities.DateTimeFormat.OFFSET,
            convertPositiveDateTimeOffsetToNegative ?: false,
            useHighPrecisionHeaderDateTimeFormat ?: false
        )
    }
}