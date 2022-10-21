package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Topic
import kotlin.test.Test

class HL7MessageHelpersTests {
    @Test
    fun `test batch file generation`() {
        var receiver = Receiver(
            "name", "org", Topic.FULL_ELR.json_val,
            translation = Hl7Configuration(
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )
        var result = HL7MessageHelpers.batchMessages(emptyList(), receiver)
        result.split(HL7MessageHelpers.hl7SegmentDelimiter).forEachIndexed { index, s ->
            val regex = when (index) {
                0 -> """^FHS\|[^|]{4}\|[^|]+\|\|\|\|\d{14}\.\d{0,4}-\d{4}.*""".toRegex()
                1 -> """^BHS\|[^|]{4}\|[^|]+\|\|\|\|\d{14}\.\d{0,4}-\d{4}.*""".toRegex()
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
            "name", "org", Topic.FULL_ELR.json_val,
            translation = Hl7Configuration(
                receivingApplicationName = "appName", receivingApplicationOID = null,
                receivingFacilityName = "facName", receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )
        result = HL7MessageHelpers.batchMessages(emptyList(), receiver)
        result.split(HL7MessageHelpers.hl7SegmentDelimiter).forEachIndexed { index, s ->
            val regex = when (index) {
                0 -> """^FHS\|[^|]{4}\|[^|]+\|\|appName\|facName\|\d{14}\.\d{0,4}-\d{4}.*""".toRegex()
                1 -> """^BHS\|[^|]{4}\|[^|]+\|\|appName\|facName\|\d{14}\.\d{0,4}-\d{4}.*""".toRegex()
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
            "name", "org", Topic.FULL_ELR.json_val,
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
                    val regex = """^FHS\|[^|]{4}\|hl7sendingApp\|\|hl7recApp\|hl7recFac\|\d{14}\.\d{0,4}-\d{4}.*"""
                        .toRegex()
                    assertThat(regex.matches(s)).isTrue()
                }
                1 -> {
                    val regex = """^BHS\|[^|]{4}\|hl7sendingApp\|\|hl7recApp\|hl7recFac\|\d{14}\.\d{0,4}-\d{4}.*"""
                        .toRegex()
                    assertThat(regex.matches(s)).isTrue()
                }
                2 -> assertThat(s).isEqualTo(sampleHl7)
                3 -> assertThat(s).isEqualTo(messages[1])
            }
        }
    }
}