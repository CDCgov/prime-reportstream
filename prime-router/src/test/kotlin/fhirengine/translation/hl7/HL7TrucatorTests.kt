package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ca.uhn.hl7v2.model.v251.message.ADT_A01
import ca.uhn.hl7v2.model.v251.message.OML_O21
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.config.TruncationConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class HL7TrucatorTests {

    private val emptyTerser = Terser(ORU_R01())
    private val covidHL7Truncator = CovidPipelineHL7Truncator()
    private val upHL7Truncator = UniversalPipelineHL7Truncator()

    @Test
    fun `test setTruncationLimitWithEncoding`() {
        val testValueWithSpecialChars = "Test & Value ~ Text ^ String"
        val testValueNoSpecialChars = "Test Value Text String"
        val testLimit = 20
        val newLimitWithSpecialChars = covidHL7Truncator
            .getTruncationLimitWithEncoding(testValueWithSpecialChars, testLimit)
        val newLimitNoSpecialChars = covidHL7Truncator
            .getTruncationLimitWithEncoding(testValueNoSpecialChars, testLimit)

        assertEquals(newLimitWithSpecialChars, 16)
        assertEquals(newLimitNoSpecialChars, testLimit)
    }

    @Test
    fun `test getHl7MaxLength`() {
        // Test the ordering provider id has the right length
        assertThat(covidHL7Truncator.getHl7MaxLength("ORC-12-1", emptyTerser)).isEqualTo(15)
        assertThat(covidHL7Truncator.getHl7MaxLength("OBR-16-1", emptyTerser)).isEqualTo(15)
        // Test that MSH returns reasonable values
        assertThat(covidHL7Truncator.getHl7MaxLength("MSH-7", emptyTerser)).isEqualTo(26)
        assertThat(covidHL7Truncator.getHl7MaxLength("MSH-4-1", emptyTerser)).isEqualTo(20)
        assertThat(covidHL7Truncator.getHl7MaxLength("MSH-3-1", emptyTerser)).isEqualTo(20)
        assertThat(covidHL7Truncator.getHl7MaxLength("MSH-4-2", emptyTerser)).isEqualTo(199)
        assertThat(covidHL7Truncator.getHl7MaxLength("MSH-1", emptyTerser)).isEqualTo(1)
        // Test that OBX returns reasonable values
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-2", emptyTerser)).isEqualTo(2)
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-5", emptyTerser)).isEqualTo(99999)
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-11", emptyTerser)).isEqualTo(1)
        // This component limit is smaller than the enclosing field. This inconsistency was fixed by v2.9
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-18", emptyTerser)).isEqualTo(22)
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-18-1", emptyTerser)).isEqualTo(199)
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-19", emptyTerser)).isEqualTo(26)
        assertThat(covidHL7Truncator.getHl7MaxLength("OBX-23-1", emptyTerser)).isEqualTo(50)
        // Test that a subcomponent returns null
        assertThat(covidHL7Truncator.getHl7MaxLength("OBR-16-1-2", emptyTerser)).isNull()
    }

    @Test
    fun `test UP getHl7MaxLength for ORU_R01`() {
        // Test the ordering provider id has the right length
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-12-1", emptyTerser)
        ).isEqualTo(15)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-16-1", emptyTerser)
        ).isEqualTo(15)
        // Test that MSH returns reasonable values
        assertThat(
            upHL7Truncator.getHl7MaxLength("MSH-7", emptyTerser)
        ).isEqualTo(26)
        assertThat(
            upHL7Truncator.getHl7MaxLength("MSH-4-1", emptyTerser)
        ).isEqualTo(20)
        assertThat(
            upHL7Truncator.getHl7MaxLength("MSH-3-1", emptyTerser)
        ).isEqualTo(20)
        assertThat(
            upHL7Truncator.getHl7MaxLength("MSH-4-2", emptyTerser)
        ).isEqualTo(199)
        assertThat(
            upHL7Truncator.getHl7MaxLength("MSH-1", emptyTerser)
        ).isEqualTo(1)
        // Test that OBX returns reasonable values
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-2", emptyTerser)
        ).isEqualTo(2)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-5", emptyTerser)
        ).isEqualTo(99999)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-11", emptyTerser)
        ).isEqualTo(1)
        // This component limit is smaller than the enclosing field. This inconsistency was fixed by v2.9
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-18", emptyTerser)
        ).isEqualTo(22)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-18-1", emptyTerser)
        ).isEqualTo(199)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-19", emptyTerser)
        ).isEqualTo(26)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/OBX-23-1", emptyTerser)
        ).isEqualTo(50)
        // Test that a subcomponent returns null
        assertThat(
            upHL7Truncator.getHl7MaxLength("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-16-1-2", emptyTerser)
        ).isNull()
    }

    @Test
    fun `test UP getHl7MaxLength for OML_O21`() {
        val terser = Terser(OML_O21())

        // Test the ordering provider id has the right length
        assertThat(upHL7Truncator.getHl7MaxLength("/ORDER/ORC-12-1", terser)).isEqualTo(15)
        assertThat(upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBR-16-1", terser)).isEqualTo(15)
        // Test that MSH returns reasonable values
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-7", terser)).isEqualTo(26)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-4-1", terser)).isEqualTo(20)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-3-1", terser)).isEqualTo(20)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-4-2", terser)).isEqualTo(199)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-1", terser)).isEqualTo(1)
        // Test that OBX returns reasonable values
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-2", terser)
        ).isEqualTo(2)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-5", terser)
        ).isEqualTo(99999)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-11", terser)
        ).isEqualTo(1)
        // This component limit is smaller than the enclosing field. This inconsistency was fixed by v2.9
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-18", terser)
        ).isEqualTo(22)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-18-1", terser)
        ).isEqualTo(199)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-19", terser)
        ).isEqualTo(26)
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-23-1", terser)
        ).isEqualTo(50)
        // Test that a subcomponent returns null
        assertThat(
            upHL7Truncator.getHl7MaxLength("/ORDER/OBSERVATION_REQUEST/OBR-16-1-2", terser)
        ).isNull()
    }

    @Test
    fun `test UP getHl7MaxLength for ADT_A01`() {
        val terser = Terser(ADT_A01())

        // Test that MSH returns reasonable values
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-7", terser)).isEqualTo(26)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-4-1", terser)).isEqualTo(20)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-3-1", terser)).isEqualTo(20)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-4-2", terser)).isEqualTo(199)
        assertThat(upHL7Truncator.getHl7MaxLength("MSH-1", terser)).isEqualTo(1)
        // Test that OBX returns reasonable values
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-2", terser)).isEqualTo(2)
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-5", terser)).isEqualTo(99999)
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-11", terser)).isEqualTo(1)
        // This component limit is smaller than the enclosing field. This inconsistency was fixed by v2.9
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-18", terser)).isEqualTo(22)
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-18-1", terser)).isEqualTo(199)
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-19", terser)).isEqualTo(26)
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-23-1", terser)).isEqualTo(50)
        // Test that a subcomponent returns null
        assertThat(upHL7Truncator.getHl7MaxLength("OBX-23-1-2", terser)).isNull()
    }

    @Test
    fun `test trimAndTruncateValue with truncated HD`() {
        val config = TruncationConfig(
            truncateHDNamespaceIds = true,
            emptyList()
        )

        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ T",
            "Test Value Text String" to "Test Value Text Stri"
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = covidHL7Truncator.trimAndTruncateValue(
                input,
                "MSH-4-1",
                emptyTerser,
                config
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test trimAndTruncateValue with HD`() {
        val config = TruncationConfig(
            truncateHDNamespaceIds = false,
            truncateHl7Fields = listOf("MSH-4-1", "MSH-3-1")
        )

        // The truncation with encoding will subtract 2 from the length for every occurrence of a
        // special characters [&^~]. This is done because the HL7 parser escapes them by replacing them with a three
        // character string. For example, & will get replaced with \T\. This adds 2 to the length of the value.
        // Because of this, after getting the length truncated to 20 (HD truncation), the string value gets checked
        // one more time to accommodate for any especial characters.
        // "Test & Value ~ Text" truncates to "Test & Value ~ T" because the final string value will be
        // converted to "Test \T\ Value \R\ Test",
        // so the final string value with 20 characters will be equals to "Test \T\ Value \R\ T"
        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ T",
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = covidHL7Truncator.trimAndTruncateValue(
                input,
                "MSH-4-1",
                emptyTerser,
                config
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test trimAndTruncateValue with custom length map`() {
        val config = TruncationConfig(
            truncateHDNamespaceIds = false,
            truncateHl7Fields = listOf("MSH-3-1"),
            customLengthHl7Fields = mapOf("MSH-4-1" to 10)
        )

        // see comment on "test trimAndTruncateValue with HD" to understand why truncation ends up having a length of 8
        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & V",
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = covidHL7Truncator.trimAndTruncateValue(
                input,
                "MSH-4-1",
                emptyTerser,
                config
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test trimAndTruncateValue when not truncating`() {
        val config = TruncationConfig(
            truncateHDNamespaceIds = false,
            truncateHl7Fields = emptyList()
        )

        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ Text ^ String",
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = covidHL7Truncator.trimAndTruncateValue(
                input,
                "MSH-4-1",
                emptyTerser,
                config
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}