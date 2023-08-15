package fhirengine.engine

import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.TranslationFunctions
import org.hl7.fhir.r4.model.BaseDateTimeType
import java.time.ZoneId

class CustomTranslationFunctions : TranslationFunctions {
    /**
     * Converts a FHIR [dateTime] to the format specified in [appContext] - specific application
     * context which contains receiver translation settings.
     *
     * @return the converted HL7 DTM
     */
    override fun convertDateTimeToHL7(
        dateTime: BaseDateTimeType,
        appContext: CustomContext?
    ): String {
        if (appContext?.config is Receiver && appContext.config.translation is Hl7Configuration) {
            val receiver = appContext.config
            val config = appContext.config.translation

            val tz =
                if (dateTime.timeZone?.id != null) {
                    ZoneId.of(dateTime.timeZone?.id)
                } else DateUtilities.utcZone

            return DateUtilities.formatDateForReceiver(
                DateUtilities.parseDate(dateTime.asStringValue()),
                tz,
                receiver.dateTimeFormat ?: DateUtilities.DateTimeFormat.OFFSET,
                config.convertPositiveDateTimeOffsetToNegative ?: false,
                config.useHighPrecisionHeaderDateTimeFormat ?: false
            )
        } else {
            return super.convertDateTimeToHL7(dateTime, appContext)
        }
    }
}