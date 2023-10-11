package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.Hl7TranslationFunctions
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.mockk
import kotlin.test.Test

class HL7TranslationFunctionsTest {

    @Test
    fun `test hl7 truncation passthrough`() {
        val translationFunctions = Hl7TranslationFunctions()
        val mockTerser = mockk<Terser>()
        val customContext = UnitTestUtils.createCustomContext()

        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ Text ^ String",
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT/PATIENT/MSH-4-1",
                mockTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}