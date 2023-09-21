package fhirengine.engine

import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7Truncator
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Constants
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.Hl7TranslationFunctions
import org.hl7.fhir.r4.model.BaseDateTimeType
import java.time.ZoneId

class CustomTranslationFunctions(
    private val hl7Truncator: HL7Truncator = HL7Truncator()
) : Hl7TranslationFunctions() {
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
        if (appContext?.config is HL7TranslationConfig) {
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

    /**
     * If the config is of type HL7TranslationConfig, attempt to truncate the field to spec length if configured
     * to do so, otherwise pass the value directly back unmodified
     */
    override fun maybeTruncateHL7Field(
        value: String,
        hl7FieldPath: String,
        terser: Terser,
        appContext: CustomContext?
    ): String {
        return if (appContext?.config is HL7TranslationConfig) {
            val config = appContext.config
            val truncationConfig = config.truncationConfig

            val hl7Field = hl7FieldPath.substringAfterLast("/")
            val cleanedHL7Field = HL7Utils.removeIndexFromHL7Field(hl7Field).trim()

            val shouldTruncateHDNamespaceIds = truncationConfig.truncateHDNamespaceIds &&
                HL7Constants.HD_FIELDS_LOCAL.contains(cleanedHL7Field)

            val shouldTruncateHl7Fields = truncationConfig.truncateHl7Fields.isNotEmpty() &&
                truncationConfig.truncateHl7Fields.contains(cleanedHL7Field)

            if (shouldTruncateHDNamespaceIds || shouldTruncateHl7Fields) {
                hl7Truncator.trimAndTruncateValue(
                    value,
                    cleanedHL7Field,
                    terser,
                    truncationConfig
                )
            } else value
        } else {
            super.maybeTruncateHL7Field(value, hl7FieldPath, terser, appContext)
        }
    }
}