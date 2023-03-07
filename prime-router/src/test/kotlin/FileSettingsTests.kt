package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
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
                  customerStatus: active
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
                    topic: covid-19
                    customerStatus: active
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
                    topic: covid-19
                    customerStatus: active
                    schemaName: one
                    format: CSV
    """.trimIndent()

    @Test
    fun `test loading a receiver`() {
        val settings = FileSettings().also { it.loadOrganizations(ByteArrayInputStream(receiversYaml.toByteArray())) }
        val result = settings.findReceiver("phd1.elr")
        assertThat(result?.jurisdictionalFilter).isNotNull().hasSize(1)
    }

    @Test
    fun `test loading a sender and receiver`() {
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(sendersAndReceiversYaml.toByteArray()))
        }
        val result = settings.findSender("phd1.sender")
        assertThat(result?.name).isEqualTo("sender")
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
                    Receiver("elr", "single", Topic.COVID_19, CustomerStatus.INACTIVE, "schema")
                )
            )
        )
        val result = settings.findReceiver("single.elr") ?: fail("Expected to find service")
        assertThat(result.name).isEqualTo("elr")
    }

    @Test
    fun `test loading local settings`() {
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        assertThat(settings).isNotNull()
    }

    @Test
    fun `test nextBatchTime`() {
        val timing = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            24,
            "04:05",
            USTimeZone.ARIZONA
        ) // AZ is -7:00 from UTC
        assertThat(timing.isValid()).isTrue()
        // The result should be in the AZ timezone
        val now1 = ZonedDateTime.of(2020, 10, 2, 0, 0, 0, 999, ZoneId.of("UTC")).toOffsetDateTime()
        val expected1 = ZonedDateTime.of(2020, 10, 1, 17, 5, 0, 0, ZoneId.of("US/Arizona")).toOffsetDateTime()
        val actual1 = timing.nextTime(now1)
        assertThat(actual1).isEqualTo(expected1)

        // Test that the minDuration comes into play
        val now2 = ZonedDateTime.of(2020, 10, 1, 0, 5, 0, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual2 = timing.nextTime(now2)
        val expected2 = ZonedDateTime.of(2020, 9, 30, 18, 5, 0, 0, ZoneId.of("US/Arizona")).toOffsetDateTime()
        assertThat(actual2).isEqualTo(expected2)
    }

    @Test
    fun `test find sender`() {
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val sender = settings.findSender("ignore")
        assertThat(sender).isNotNull()
        val sender2 = settings.findSender("ignore.default")
        assertThat(sender2).isNotNull()
    }

    @Test
    fun `test find receiver`() {
        val org1 = DeepOrganization(
            "test", "test", Organization.Jurisdiction.FEDERAL, null, null,
            receivers = listOf(
                Receiver("service1", "test", Topic.COVID_19, CustomerStatus.INACTIVE, "schema1"),
            )
        )
        val settings = FileSettings().also {
            it.loadOrganizationList(listOf(org1))
        }
        assertThat(settings.findOrganization("test")?.name).isNotNull().isEqualTo(org1.name)
        assertThat(settings.findReceiver(fullName = "test.service1")?.name)
            .isNotNull().isEqualTo(org1.receivers[0].name)
        assertThat(settings.findReceiver(fullName = "service1")).isNull()
    }

    @Test
    fun `test duplicate receiver name`() {
        val settings = FileSettings()
        val org1 = DeepOrganization(
            "test", "test", Organization.Jurisdiction.FEDERAL, null, null,
            receivers = listOf(
                Receiver("service1", "test", Topic.COVID_19, CustomerStatus.INACTIVE, "schema1"),
                Receiver("service1", "test", Topic.COVID_19, CustomerStatus.INACTIVE, "schema1")
            )
        )
        assertThat {
            settings.loadOrganizationList(listOf(org1))
        }.isFailure()
    }
}