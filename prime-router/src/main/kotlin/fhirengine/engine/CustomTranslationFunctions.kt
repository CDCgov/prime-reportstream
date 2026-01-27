package fhirengine.engine

import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.fhirconverter.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.fhirconverter.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.fhirconverter.translation.hl7.utils.CustomContext
import gov.cdc.prime.fhirconverter.translation.hl7.utils.HL7Utils
import gov.cdc.prime.fhirconverter.translation.hl7.utils.Hl7TranslationFunctions
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.UniversalPipelineHL7Truncator
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Constants
import org.hl7.fhir.r4.model.BaseDateTimeType
import java.time.ZoneId

class CustomTranslationFunctions(
    private val hl7Truncator: UniversalPipelineHL7Truncator = UniversalPipelineHL7Truncator(),
) : Hl7TranslationFunctions() {
    /**
     * Converts a FHIR [dateTime] to the format specified in [appContext] - specific application
     * context which contains receiver translation settings.
     *
     * @return the converted HL7 DTM
     */
    override fun convertDateTimeToHL7(
        dateTime: BaseDateTimeType,
        appContext: CustomContext?,
        element: ConverterSchemaElement?,
        constantSubstitutor: ConstantSubstitutor?,
    ): String {
        check(appContext != null)
        check(appContext.config is HL7TranslationConfig)
        val receiver = appContext.config.receiver
        val config = appContext.config.hl7Configuration
        var dateTimeFormat = receiver?.dateTimeFormat

        if (config.convertTimestampToDateTime?.isNotEmpty() == true) {
            dateTimeFormat = getDateTimeFormat(
                config.convertTimestampToDateTime,
                element,
                constantSubstitutor,
                appContext,
                dateTimeFormat
            )
        }

        val tz =
            if (config.convertDateTimesToReceiverLocalTime == true && !receiver?.timeZone?.zoneId.isNullOrBlank()) {
                ZoneId.of(receiver?.timeZone?.zoneId)
            } else {
                DateUtilities.utcZone
            }

        return DateUtilities.formatDateForReceiver(
            DateUtilities.parseDate(dateTime.asStringValue()),
            tz,
            dateTimeFormat ?: DateUtilities.DateTimeFormat.OFFSET,
            config.convertPositiveDateTimeOffsetToNegative ?: false,
            config.useHighPrecisionHeaderDateTimeFormat ?: false
        )
    }

    /**
     * Checks if [element] needs to be converted to Datetime if the [element] is listed in the
     * [convertTimestampToDateTime] list and returns [DateUtilities.DateTimeFormat.LOCAL] format
     *
     */
    internal fun getDateTimeFormat(
        convertTimestampToDateTime: String,
        element: ConverterSchemaElement?,
        constantSubstitutor: ConstantSubstitutor?,
        appContext: CustomContext?,
        dateTimeFormat: DateUtilities.DateTimeFormat?,
    ): DateUtilities.DateTimeFormat? {
        var dateTimeFormat1 = dateTimeFormat
        val convertTimestampToDateTimeFields = convertTimestampToDateTime
            .split(",")
            .map { it.trim() }

        element?.hl7Spec?.forEach { rawHl7Spec ->
            if (constantSubstitutor != null) {
                val resolvedHl7Spec = constantSubstitutor.replace(rawHl7Spec, appContext)
                val hl7Field = resolvedHl7Spec.substringAfterLast("/")
                val cleanedHL7Field = HL7Utils.removeIndexFromHL7Field(hl7Field).trim()
                if (convertTimestampToDateTimeFields.contains(cleanedHL7Field)) {
                    dateTimeFormat1 = DateUtilities.DateTimeFormat.LOCAL
                }
            }
        }
        return dateTimeFormat1
    }

    /**
     * If the config is of type HL7TranslationConfig, attempt to truncate the field to spec length if configured
     * to do so, otherwise pass the value directly back unmodified
     */
    override fun maybeTruncateHL7Field(
        value: String,
        hl7FieldPath: String,
        terser: Terser,
        appContext: CustomContext?,
    ): String = if (appContext?.config is HL7TranslationConfig) {
            val config = appContext.config
            val truncationConfig = config.truncationConfig

            val hl7Field = hl7FieldPath.substringAfterLast("/")
            val path = hl7FieldPath.substringBeforeLast("/")
            val cleanedHL7Field = HL7Utils.removeIndexFromHL7Field(hl7Field).trim()
            val cleanedHL7FieldPath = "$path/$cleanedHL7Field"

            val shouldTruncateHDNamespaceIds = truncationConfig.truncateHDNamespaceIds &&
                HL7Constants.HD_FIELDS_LOCAL.contains(cleanedHL7Field)

            val shouldTruncateHl7Fields = truncationConfig.truncateHl7Fields.isNotEmpty() &&
                truncationConfig.truncateHl7Fields.contains(cleanedHL7Field)

            if (shouldTruncateHDNamespaceIds || shouldTruncateHl7Fields) {
                hl7Truncator.trimAndTruncateValue(
                    value,
                    cleanedHL7FieldPath,
                    terser,
                    truncationConfig
                )
            } else {
                value
            }
        } else {
            super.maybeTruncateHL7Field(value, hl7FieldPath, terser, appContext)
        }
}