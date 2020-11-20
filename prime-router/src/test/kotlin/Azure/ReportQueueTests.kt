package gov.cdc.prime.router.Azure

import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.ReportQueue
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportQueueTests {
    @Test
    fun `test HeaderEncode`() {
        val header = ReportQueue.Header(
            UUID.randomUUID(),
            "schema",
            emptyList(),
            "service",
            OrganizationService.Format.CSV,
            OffsetDateTime.now(),
            "blob"
        )
        val json = header.encode()
        assertTrue(json.isNotEmpty())
    }

    @Test
    fun `test HeaderDecodeWithSource`() {
        val header = ReportQueue.Header(
            UUID.randomUUID(),
            "schema",
            listOf(ClientSource("org", "client")),
            "service",
            OrganizationService.Format.CSV,
            OffsetDateTime.now(ZoneOffset.UTC),
            "blob"
        )
        val json = header.encode()
        val result = ReportQueue.Header.decode(json)
        assertEquals(header, result)
    }
}