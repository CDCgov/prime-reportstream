package fhirengine.engine

import gov.cdc.prime.router.CovidHL7Configuration
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.Hl7TranslationFunctions
import org.hl7.fhir.r4.model.BaseDateTimeType
import java.time.ZoneId

class CustomTranslationFunctions : Hl7TranslationFunctions() {
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
        if (appContext?.config is CovidHL7Configuration) {
            val receiver = appContext.config.receiver
            val config = appContext.config.hl7Configuration

            val tz =
                if (dateTime.timeZone?.id != null) {
                    ZoneId.of(dateTime.timeZone?.id)
                } else DateUtilities.utcZone

            return DateUtilities.formatDateForReceiver(
                DateUtilities.parseDate(dateTime.asStringValue()),
                tz,
                receiver?.dateTimeFormat ?: DateUtilities.DateTimeFormat.OFFSET,
                config.convertPositiveDateTimeOffsetToNegative ?: false,
                config.useHighPrecisionHeaderDateTimeFormat ?: false
            )
        } else {
            return super.convertDateTimeToHL7(dateTime, appContext)
        }
    }
}