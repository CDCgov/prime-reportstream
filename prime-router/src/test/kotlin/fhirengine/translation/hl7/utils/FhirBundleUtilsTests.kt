package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.StringType
import kotlin.test.Test

class FhirBundleUtilsTests {
    @Test
    fun `test convert fhir type`() {
        var convertedValue = FhirBundleUtils.convertFhirType(StringType("testing"), "string", "id")
        assertThat(convertedValue).isInstanceOf(IdType::class)

        convertedValue = FhirBundleUtils.convertFhirType(BooleanType("true"), "boolean", "boolean")
        assertThat(convertedValue).isInstanceOf(BooleanType::class)
        if (convertedValue is BooleanType)
            assertThat(convertedValue.booleanValue()).isTrue()

        convertedValue = FhirBundleUtils.convertFhirType(BooleanType("true"), "boolean", "id")
        assertThat(convertedValue).isInstanceOf(BooleanType::class)
    }
}