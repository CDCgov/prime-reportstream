package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import org.apache.logging.log4j.kotlin.KotlinLogger
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Base64BinaryType
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.MarkdownType
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.UriType
import org.hl7.fhir.r4.model.UrlType
import org.hl7.fhir.r4.model.UuidType

object FhirBundleUtils : Logging {
    private enum class StringCompatibleType(val typeAsString: kotlin.String) {
        Base64Binary("base64Binary"),
        Canonical("canonical"),
        Code("code"),
        Date("date"),
        DateTime("dateTime"),
        Id("id"),
        Instant("instant"),
        Integer("integer"),
        Markdown("markdown"),
        Oid("oid"),
        String("string"),
        Time("time"),
        Uri("uri"),
        Url("url"),
        Uuid("uuid"),
    }

    /**
     * Converts a [value] of type [sourceType] into a compatible Base of type [targetType]. Returns the original value
     * and logs an error if the conversion is not supported.
     */
    fun convertFhirType(
        value: Base,
        sourceType: String,
        targetType: String,
        logger: KotlinLogger = this.logger,
    ): Base {
        return if (sourceType == targetType || targetType == "*") {
            value
        } else if (targetType.contains("|")) {
            val targetTypes = targetType.split("|")
            targetTypes.forEach {
                if (sourceType == it || it == "*") {
                    return value
                }
                val attemptedValue = getValue(it, value, logger, sourceType, false)
                if (attemptedValue != null) {
                    return attemptedValue
                }
            }
            value
        } else if (StringCompatibleType.values().any { it.typeAsString == sourceType }) {
            getValue(targetType, value, logger, sourceType, true) ?: value
        } else {
            logger.debug("Conversion between $sourceType and $targetType not yet implemented.")
            value
        }
    }

    /**
    Determines if the [targetType] matches any of the options and, if it does, returns the [value] as that type. If not, uses the [logger] to log the [sourceType] and [targetType]. Will also log this error if there is an issue with compatibility and the [logIncompatibilityErrors] is true.
     */
    private fun getValue(
        targetType: String,
        value: Base,
        logger: KotlinLogger,
        sourceType: String,
        logIncompatibilityErrors: Boolean,
    ) = try {
        when (targetType) {
            StringCompatibleType.Base64Binary.typeAsString -> Base64BinaryType(value.primitiveValue())
            StringCompatibleType.Canonical.typeAsString -> CanonicalType(value.primitiveValue())
            StringCompatibleType.Code.typeAsString -> CodeType(value.primitiveValue())
            StringCompatibleType.Date.typeAsString -> DateType(value.primitiveValue())
            StringCompatibleType.DateTime.typeAsString -> DateTimeType(value.primitiveValue())
            StringCompatibleType.Id.typeAsString -> IdType(value.primitiveValue())
            StringCompatibleType.Instant.typeAsString -> InstantType(value.primitiveValue())
            StringCompatibleType.Markdown.typeAsString -> MarkdownType(value.primitiveValue())
            StringCompatibleType.Oid.typeAsString -> OidType(value.primitiveValue())
            StringCompatibleType.String.typeAsString -> StringType(value.primitiveValue())
            StringCompatibleType.Time.typeAsString -> TimeType(value.primitiveValue())
            StringCompatibleType.Uri.typeAsString -> UriType(value.primitiveValue())
            StringCompatibleType.Url.typeAsString -> UrlType(value.primitiveValue())
            StringCompatibleType.Uuid.typeAsString -> UuidType(value.primitiveValue())
            else -> {
                logger.debug("Conversion between $sourceType and $targetType not supported.")
                null
            }
        }
    } catch (e: Exception) {
        if (logIncompatibilityErrors) {
            logger.debug("Conversion between $sourceType and $targetType not supported.")
        }
        null
    }
}