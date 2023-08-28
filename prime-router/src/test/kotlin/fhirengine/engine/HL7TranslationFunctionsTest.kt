package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.Hl7TranslationFunctions
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.TranslationFunctions
import kotlin.test.Test

class HL7TranslationFunctionsTest {

    @Test
    fun testHL7Truncation() {
        val translationFunctions: TranslationFunctions = Hl7TranslationFunctions()
        val emptyTerser = Terser(ORU_R01())

        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ T",
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.truncateHL7Field(
                input,
                "MSH-4-1",
                emptyTerser,
                truncateHDNamespaceIds = false,
                truncateHl7Fields = listOf("MSH-4-1", "MSH-3-1")
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}