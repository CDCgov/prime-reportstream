package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.Event
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.UUID
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
                0 -> """^FHS\|[^|]{4}\|[^|]+\|\|\|\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
                1 -> """^BHS\|[^|]{4}\|[^|]+\|\|\|\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
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
                0 -> """^FHS\|[^|]{4}\|[^|]+\|\|appName\|facName\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
                1 -> """^BHS\|[^|]{4}\|[^|]+\|\|appName\|facName\|\d{14}\.\d{0,4}[-+]\d{4}.*""".toRegex()
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
                    val regex = """^FHS\|[^|]{4}\|hl7sendingApp\|\|hl7recApp\|hl7recFac\|.*"""
                        .toRegex()
                    assertThat(regex.matches(s)).isTrue()
                }
                1 -> {
                    val regex = """^BHS\|[^|]{4}\|hl7sendingApp\|\|hl7recApp\|hl7recFac\|.*"""
                        .toRegex()
                    assertThat(regex.matches(s)).isTrue()
                }
                2 -> assertThat(s).isEqualTo(sampleHl7)
                3 -> assertThat(s).isEqualTo(messages[1])
            }
        }
    }

    @Test
    fun `test generate report for hl7`() {
        val mockMetadata = mockk<Metadata>() {
            every { fileNameTemplates } returns emptyMap()
        }
        val mockActionHistory = mockk<ActionHistory>() {
            every { trackCreatedReport(any(), any(), any()) } returns Unit
        }
        val hl7MockData = UUID.randomUUID().toString().toByteArray() // Just some data
        val receiver = Receiver(
            "name", "org", Topic.FULL_ELR.json_val,
            translation = Hl7Configuration(
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )

        // First test error conditions
        assertThat {
            HL7MessageHelpers.takeHL7GetReport(
                Event.EventAction.PROCESS, hl7MockData, emptyList(), receiver, mockMetadata, mockActionHistory
            )
        }.isFailure()

        assertThat {
            HL7MessageHelpers.takeHL7GetReport(
                Event.EventAction.PROCESS, "".toByteArray(), listOf(ReportId.randomUUID()), receiver,
                mockMetadata, mockActionHistory
            )
        }.isFailure()

        // Now test single report
        mockkObject(BlobAccess)
        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.HL7, hl7MockData, any(), any(), Event.EventAction.PROCESS
            )
        } returns
            BlobAccess.BlobInfo(Report.Format.HL7, "someurl", "digest".toByteArray())

        var reportIds = listOf(ReportId.randomUUID())
        val (report, event, blobInfo) = HL7MessageHelpers.takeHL7GetReport(
            Event.EventAction.PROCESS, hl7MockData, reportIds, receiver, mockMetadata, mockActionHistory
        )
        unmockkObject(BlobAccess)

        assertThat(report.bodyFormat).isEqualTo(Report.Format.HL7)
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.destination).isNotNull()
        assertThat(report.destination!!.name).isEqualTo(receiver.name)
        assertThat(report.itemLineages).isNotNull()
        assertThat(report.itemLineages!!.size).isEqualTo(1)
        assertThat(event.eventAction).isEqualTo(Event.EventAction.PROCESS)
        assertThat(blobInfo.blobUrl).isEqualTo("someurl")

        // Multiple reports
        reportIds = listOf(ReportId.randomUUID(), ReportId.randomUUID(), ReportId.randomUUID())
        mockkObject(BlobAccess.Companion)
        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.HL7_BATCH, hl7MockData, any(), any(), Event.EventAction.SEND
            )
        } returns
            BlobAccess.BlobInfo(Report.Format.HL7_BATCH, "someurl", "digest".toByteArray())
        val (report2, event2, _) = HL7MessageHelpers.takeHL7GetReport(
            Event.EventAction.SEND, hl7MockData, reportIds, receiver, mockMetadata, mockActionHistory
        )
        unmockkObject(BlobAccess.Companion)
        assertThat(report2.bodyFormat).isEqualTo(Report.Format.HL7_BATCH)
        assertThat(report2.itemCount).isEqualTo(3)
        assertThat(report2.itemLineages).isNotNull()
        assertThat(report2.itemLineages!!.size).isEqualTo(3)
        assertThat(event2.eventAction).isEqualTo(Event.EventAction.SEND)
    }
}