package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Topic
import kotlin.test.Test

class HL7MessageHelpersTests {
    @Test
    fun `test batch file generation`() {
        var receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )
        var result = HL7MessageHelpers.batchMessages(emptyList(), receiver)
        result.split(HL7MessageHelpers.hl7SegmentDelimiter).forEachIndexed { index, s ->
            val regex = when (index) {
                0 -> """^FHS\|[^|]{4}\|[^|]+\|[^|]+\|\|\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
                1 -> """^BHS\|[^|]{4}\|[^|]+\|[^|]+\|\|\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
                2 -> """^BTS\|0""".toRegex()
                3 -> """^FTS\|1""".toRegex()
                else -> null
            }
            if (s.isNotEmpty()) {
                assertThat(regex).isNotNull()
                assertThat(regex!!.matches(s), s).isTrue()
            }
        }

        // With receiver information
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                receivingApplicationName = "appName", receivingApplicationOID = null,
                receivingFacilityName = "facName", receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )
        result = HL7MessageHelpers.batchMessages(emptyList(), receiver)
        result.split(HL7MessageHelpers.hl7SegmentDelimiter).forEachIndexed { index, s ->
            val regex = when (index) {
                0 -> """^FHS\|[^|]{4}\|[^|]+\|[^|]+\|appName\|facName\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
                1 -> """^BHS\|[^|]{4}\|[^|]+\|[^|]+\|appName\|facName\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
                2 -> """^BTS\|0""".toRegex()
                3 -> """^FTS\|1""".toRegex()
                else -> {
                    assertThat(s).isEmpty()
                    null
                }
            }
            if (regex != null) {
                assertThat(regex.matches(s), s).isTrue()
            }
        }

        val sampleHl7 = """MSH|^~\&#|hl7sendingApp||hl7recApp|hl7recFac|||ORU^R01^ORU_R01|||2.5.1""".trimIndent()
        var messages = listOf("message1", "message2", sampleHl7)
        result = HL7MessageHelpers.batchMessages(messages, receiver)
        result.split(HL7MessageHelpers.hl7SegmentDelimiter).forEachIndexed { index, s ->
            when (index) {
                0 -> assertThat(s).startsWith("FHS")
                1 -> assertThat(s).startsWith("BHS")
                2 -> assertThat(s).isEqualTo(messages[0])
                3 -> assertThat(s).isEqualTo(messages[1])
                4 -> {
                    assertThat(s).startsWith("MSH")
                }

                5 -> assertThat(s).isEqualTo("BTS|${messages.size}")
                6 -> assertThat(s).isEqualTo("FTS|1")
            }
        }

        // Now the sample HL7 is first, so we can grab data from it
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )
        messages = listOf(sampleHl7, "message1", "message2")
        result = HL7MessageHelpers.batchMessages(messages, receiver)
        result.split(HL7MessageHelpers.hl7SegmentDelimiter).forEachIndexed { index, s ->
            when (index) {
                0 -> {
                    val regex = """^FHS\|[^|]{4}\|hl7sendingApp.*\|hl7sendingApp.*|hl7recApp\|hl7recFac\|.*"""
                        .toRegex()
                    assertThat(regex.matches(s)).isTrue()
                }

                1 -> {
                    val regex = """^BHS\|[^|]{4}\|hl7sendingApp.*\|hl7sendingApp.*|hl7recApp\|hl7recFac\|.*"""
                        .toRegex()
                    assertThat(regex.matches(s)).isTrue()
                }

                2 -> assertThat(s).isEqualTo(sampleHl7)
                3 -> assertThat(s).isEqualTo(messages[1])
            }
        }
    }

    @Test
    fun `test decoding generated batch HL7 file`() {
        // Make sure the HAPI HL7 v2 library can decode the batch file
        val receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                receivingApplicationName = "appName", receivingApplicationOID = null,
                receivingFacilityName = "facName", receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )
        val hl7Message = """
MSH|^~\&||Any facility USA^31D2827476^CLIA|0.0.0.0.1|0.0.0.0.1|20200331082902||ORU^R01^ORU_R01|63774|T|2.5.1||||||||||
PID|1|1731-TEST734|5000104^^^^MR||TEST^RESULTS||19950615|M||U||||||S|||111-11-1111
PV1|1|O|||||||||||||||||1731-T734-40923
OBR|6|||40923^TONSILLITIS -neg strep screen|||19980601184619|||||||||51789||OV
OBX|1|ST|MLI-4000.15^TEMPERATURE||97.7|deg f|||||R|||19980601184619
        """.trimIndent()

        val batchFile = HL7MessageHelpers.batchMessages(listOf(hl7Message, hl7Message), receiver)
        val messages = HL7Reader(ActionLogger()).getMessages(batchFile)
        assertThat(messages).isNotEmpty()
        assertThat(messages.size).isEqualTo(2)
        val a = Terser(messages[0])
        print(a)
        assertThat(Terser(messages[0]).get("/PATIENT_RESULT/PATIENT/PID-2")).isEqualTo("1731-TEST734")
        assertThat(Terser(messages[1]).get("/PATIENT_RESULT/PATIENT/PID-2")).isEqualTo("1731-TEST734")
    }
}