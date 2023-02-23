package gov.cdc.prime.router.fhirengine.translation.hl7.utils

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
    private val stringCompatibleTypes = listOf(
        "base64Binary",
        "canonical",
        "code",
        "date",
        "dateTime",
        "id",
        "instant",
        "markdown",
        "oid",
        "string",
        "time",
        "url",
        "uri",
        "uuid"
    )

    fun convertFhirType(value: Base, sourceType: String, targetType: String): Base {
        return if (sourceType == targetType) {
            value
        } else if (stringCompatibleTypes.contains(sourceType)) {
            when (targetType) {
                "base64Binary" -> Base64BinaryType(value.primitiveValue())
                "canonical" -> CanonicalType(value.primitiveValue())
                "code" -> CodeType(value.primitiveValue())
                "date" -> DateType(value.primitiveValue())
                "dateTime" -> DateTimeType(value.primitiveValue())
                "id" -> IdType(value.primitiveValue())
                "instant" -> InstantType(value.primitiveValue())
                "markdown" -> MarkdownType(value.primitiveValue())
                "oid" -> OidType(value.primitiveValue())
                "string" -> StringType(value.primitiveValue())
                "time" -> TimeType(value.primitiveValue())
                "url" -> UrlType(value.primitiveValue())
                "uri" -> UriType(value.primitiveValue())
                "uuid" -> UuidType(value.primitiveValue())
                else -> {
                    logger.error("Conversion between $sourceType and $targetType not yet implemented.")
                    value
                }
            }
        } else {
            logger.error("Conversion between $sourceType and $targetType not yet implemented.")
            value
        }
    }
}