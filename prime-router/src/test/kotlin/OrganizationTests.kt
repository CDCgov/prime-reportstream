package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class OrganizationTests {
    private val servicesYaml = """
            ---
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                services: 
                - name: elr
                  topic: test
                  schema: one
                  jurisdictionalFilter: [ "matches(a, 1)"]
                  transforms: {deidentify: false}
                  address: phd1
                  format: CSV
    """.trimIndent()

    private val clientsAndServicesYaml = """
            ---
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                services: 
                  - name: elr
                    topic: test
                    schema: one
                    jurisdictionalFilter: [ "matches(a, 1)"]
                    transforms: {deidentify: false}
                    batch:
                      operation: MERGE
                      numberPerDay: 24
                      initialBatch: 00:00
                      timeZone: ARIZONA
                    address: phd1
                    format: CSV
                clients:
                  - name: sender
                    topic: topic
                    schema: one
                    format: CSV
    """.trimIndent()

    @Test
    fun `test loading a service`() {
        val metadata = Metadata()
        metadata.loadOrganizations(ByteArrayInputStream(servicesYaml.toByteArray()))
        val result = metadata.findService("phd1.elr")

        assertEquals(1, result?.jurisdictionalFilter?.size)
    }

    @Test
    fun `test loading a client and service`() {
        val metadata = Metadata()
        metadata.loadOrganizations(ByteArrayInputStream(clientsAndServicesYaml.toByteArray()))

        val result = metadata.findClient("phd1.sender")

        assertEquals("sender", result?.name)
    }

    @Test
    fun `test loading a single organization`() {
        val metadata = Metadata().loadOrganizations(
            Organization(
                name = "single",
                description = "blah blah",
                clients = listOf(),
                services = listOf(
                    OrganizationService("elr", "topic", "schema")
                )
            )
        )
        val result = metadata.findService("single.elr") ?: fail("Expected to find service")
        assertEquals("elr", result.name)
    }



    @Test
    fun `test nextBatchTime`() {
        val batch = OrganizationService.Batch(
            OrganizationService.BatchOperation.NONE,
            24,
            "04:05",
            USTimeZone.ARIZONA
        ) // AZ is -7:00 from UTC
        assertTrue(batch.isValid())

        // The result should be in the AZ timezone
        val now1 = ZonedDateTime.of(2020, 10, 2, 0, 0, 0, 999, ZoneId.of("UTC")).toOffsetDateTime()
        val expected1 = ZonedDateTime.of(2020, 10, 1, 17, 5, 0, 0, ZoneId.of("US/Arizona")).toOffsetDateTime()
        val actual1 = batch.nextBatchTime(now1)
        assertEquals(expected1, actual1)

        // Test that the minDuration comes into play
        val now2 = ZonedDateTime.of(2020, 10, 1, 0, 5, 0, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual2 = batch.nextBatchTime(now2)
        val expected2 = ZonedDateTime.of(2020, 9, 30, 18, 5, 0, 0, ZoneId.of("US/Arizona")).toOffsetDateTime()
        assertEquals(expected2, actual2)
    }
}