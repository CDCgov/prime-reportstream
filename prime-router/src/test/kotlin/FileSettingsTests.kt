package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class FileSettingsTests {
    private val receiversYaml = """
            ---
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                jurisdiction: STATE
                stateCode: AZ
                receivers: 
                - name: elr
                  organizationName: phd1
                  topic: test
                  jurisdictionalFilter: [ "matches(a, 1)"]
                  deidentify: false
                  translation:
                    type: CUSTOM
                    schemaName: one
                    format: CSV
                 
    """.trimIndent()

    private val sendersAndReceiversYaml = """
            ---
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                jurisdiction: STATE
                stateCode: AZ
                receivers: 
                  - name: elr
                    organizationName: phd1
                    topic: test
                    jurisdictionalFilter: [ "matches(a, 1)"]
                    deidentify: false
                    timing:
                      operation: MERGE
                      numberPerDay: 24
                      initialTime: 00:00
                      timeZone: ARIZONA
                    translation:
                      type: CUSTOM
                      schemaName: one
                      format: CSV
                senders:
                  - name: sender
                    organizationName: phd1
                    topic: topic
                    schemaName: one
                    format: CSV
    """.trimIndent()

    @Test
    fun `test loading a receiver`() {
        val settings = FileSettings()
        settings.loadOrganizations(ByteArrayInputStream(receiversYaml.toByteArray()))
        val result = settings.findReceiver("phd1.elr")

        assertEquals(1, result?.jurisdictionalFilter?.size)
    }

    @Test
    fun `test loading a sender and receiver`() {
        val settings = FileSettings()
        settings.loadOrganizations(ByteArrayInputStream(sendersAndReceiversYaml.toByteArray()))

        val result = settings.findSender("phd1.sender")

        assertEquals("sender", result?.name)
    }

    @Test
    fun `test loading a single organization`() {
        val settings = FileSettings().loadOrganizations(
            DeepOrganization(
                name = "single",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                stateCode = null,
                countyName = null,
                senders = listOf(),
                receivers = listOf(
                    Receiver("elr", "single", "topic", "schema")
                )
            )
        )
        val result = settings.findReceiver("single.elr") ?: fail("Expected to find service")
        assertEquals("elr", result.name)
    }

    @Test
    fun `test loading local settings`() {
        val settings = FileSettings(FileSettings.defaultSettingsDirectory, "-local")
        assertNotNull(settings)
    }

    @Test
    fun `test loading test settings`() {
        val settings = FileSettings(FileSettings.defaultSettingsDirectory, "-test")
        assertNotNull(settings)
    }

    @Test
    fun `test loading prod settings`() {
        val settings = FileSettings(FileSettings.defaultSettingsDirectory, "-prod")
        assertNotNull(settings)
    }

    @Test
    fun `test nextBatchTime`() {
        val timing = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            24,
            "04:05",
            USTimeZone.ARIZONA
        ) // AZ is -7:00 from UTC
        assertTrue(timing.isValid())

        // The result should be in the AZ timezone
        val now1 = ZonedDateTime.of(2020, 10, 2, 0, 0, 0, 999, ZoneId.of("UTC")).toOffsetDateTime()
        val expected1 = ZonedDateTime.of(2020, 10, 1, 17, 5, 0, 0, ZoneId.of("US/Arizona")).toOffsetDateTime()
        val actual1 = timing.nextTime(now1)
        assertEquals(expected1, actual1)

        // Test that the minDuration comes into play
        val now2 = ZonedDateTime.of(2020, 10, 1, 0, 5, 0, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual2 = timing.nextTime(now2)
        val expected2 = ZonedDateTime.of(2020, 9, 30, 18, 5, 0, 0, ZoneId.of("US/Arizona")).toOffsetDateTime()
        assertEquals(expected2, actual2)
    }

    @Test
    fun `test find sender`() {
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val sender = settings.findSender("simple_report")
        assertNotNull(sender)
        val sender2 = settings.findSender("simple_report.default")
        assertNotNull(sender2)
    }

    @Test
    fun `test find receiver`() {
        val settings = FileSettings()
        val org1 = DeepOrganization(
            "test", "test", Organization.Jurisdiction.FEDERAL, null, null,
            receivers = listOf(
                Receiver("service1", "test", "topic1", "schema1"),
            )
        )
        settings.loadOrganizationList((listOf(org1)))
        assertEquals(org1.name, settings.findOrganization("test")?.name)
        assertEquals(org1.receivers[0].name, settings.findReceiver(fullName = "test.service1")?.name)
        assertNull(settings.findReceiver(fullName = "service1"))
    }

    @Test
    fun `test duplicate receiver name`() {
        val settings = FileSettings()
        val org1 = DeepOrganization(
            "test", "test", Organization.Jurisdiction.FEDERAL, null, null,
            receivers = listOf(
                Receiver("service1", "test", "topic1", "schema1"),
                Receiver("service1", "test", "topic1", "schema1")
            )
        )
        assertFails { settings.loadOrganizationList(listOf(org1)) }
    }
}