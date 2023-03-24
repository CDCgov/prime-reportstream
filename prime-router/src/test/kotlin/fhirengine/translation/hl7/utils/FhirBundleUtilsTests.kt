package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import org.hl7.fhir.r4.model.Base64BinaryType
import org.hl7.fhir.r4.model.BooleanType
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
import kotlin.test.Test

class FhirBundleUtilsTests {
    @Test
    fun `test convert fhir type`() {
        var convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "base64Binary")
        assertThat(convertedValue).isInstanceOf(Base64BinaryType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "canonical")
        assertThat(convertedValue).isInstanceOf(CanonicalType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "code")
        assertThat(convertedValue).isInstanceOf(CodeType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("1905-08-23"), "string", "date")
        assertThat(convertedValue).isInstanceOf(DateType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("2015-02-07T13:28:17-05:00"), "string", "dateTime")
        assertThat(convertedValue).isInstanceOf(DateTimeType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "id")
        assertThat(convertedValue).isInstanceOf(IdType::class)
        convertedValue =
            FhirBundleUtils.convertFhirType(StringType("2015-02-07T13:28:17.239+02:00"), "string", "instant")
        assertThat(convertedValue).isInstanceOf(InstantType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "markdown")
        assertThat(convertedValue).isInstanceOf(MarkdownType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "oid")
        assertThat(convertedValue).isInstanceOf(OidType::class)
        convertedValue = FhirBundleUtils.convertFhirType(IdType("123456"), "id", "string")
        assertThat(convertedValue).isInstanceOf(StringType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "time")
        assertThat(convertedValue).isInstanceOf(TimeType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "uri")
        assertThat(convertedValue).isInstanceOf(UriType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "url")
        assertThat(convertedValue).isInstanceOf(UrlType::class)
        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "uuid")
        assertThat(convertedValue).isInstanceOf(UuidType::class)

        convertedValue = FhirBundleUtils.convertFhirType(BooleanType("true"), "boolean", "boolean")
        assertThat(convertedValue).isInstanceOf(BooleanType::class)
        if (convertedValue is BooleanType)
            assertThat(convertedValue.booleanValue()).isTrue()
    }

    @Test
    fun `test convert to datetime`() {
        var convertedValue =
            FhirBundleUtils.convertFhirType(StringType("2015-02-07T13:28:17-05:00"), "string", "dateTime")
        assertThat(convertedValue).isInstanceOf(DateTimeType::class)
        convertedValue =
            FhirBundleUtils.convertFhirType(InstantType("2015-02-07T13:28:17.239+02:00"), "instant", "dateTime")
        assertThat(convertedValue).isInstanceOf(DateTimeType::class)
        convertedValue =
            FhirBundleUtils.convertFhirType(DateTimeType("2015-02-07T13:28:17.239+02:00"), "dateTime", "dateTime")
        assertThat(convertedValue).isInstanceOf(DateTimeType::class)
        convertedValue =
            FhirBundleUtils.convertFhirType(DateType("2015-02-07"), "date", "dateTime")
        assertThat(convertedValue).isInstanceOf(DateTimeType::class)

        // Incompatible type
        assertThat { FhirBundleUtils.convertFhirType(DateType("13:28:17"), "time", "dateTime") }.isFailure()
    }

    @Test
    fun `test convert incompatible fhir types`() {
        var convertedValue = FhirBundleUtils.convertFhirType(BooleanType("true"), "boolean", "id")
        assertThat(convertedValue).isInstanceOf(BooleanType::class)

        convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "boolean")
        assertThat(convertedValue).isInstanceOf(StringType::class)
    }

    @Test
    fun `test convert invalid values`() {
        assertThat {
            FhirBundleUtils.convertFhirType(
                StringType("testing"),
                "string",
                "date"
            )
        }.isFailure()

        assertThat {
            FhirBundleUtils.convertFhirType(
                StringType("testing"),
                "string",
                "dateTime"
            )
        }.isFailure()

        assertThat {
            FhirBundleUtils.convertFhirType(
                StringType("testing"),
                "string",
                "instant"
            )
        }.isFailure()
    }
}