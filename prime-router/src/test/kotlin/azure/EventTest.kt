package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.reportstream.shared.EventAction
import gov.cdc.prime.reportstream.shared.ReportOptions
import gov.cdc.prime.router.azure.Event.Companion.parsePrimeRouterQueueMessage
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class EventTest {
    @Test
    fun `test reportEvent encode and decode`() {
        val event = ReportEvent(EventAction.SEND, UUID.randomUUID(), false)
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test reportEvent encode and decode with time`() {
        val event = ReportEvent(EventAction.SEND, UUID.randomUUID(), false, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test reportEvent encode and decode as empty`() {
        val event = ReportEvent(EventAction.SEND, UUID.randomUUID(), true)
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test reportEvent encode and decode as empty with time`() {
        val event = ReportEvent(EventAction.SEND, UUID.randomUUID(), true, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test receiverEvent encode and decode`() {
        val event = BatchEvent(EventAction.BATCH, "test", false)
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test batchEvent encode and decode with time`() {
        val event = BatchEvent(EventAction.BATCH, "test", false, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test batchEvent encode and decode as empty`() {
        val event = BatchEvent(EventAction.BATCH, "test", true)
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test batchEvent encode and decode as empty with time`() {
        val event = BatchEvent(EventAction.BATCH, "test", true, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test processEvent encode and decode with time`() {
        val at = OffsetDateTime.now()
        val reportId = UUID.randomUUID()
        val event = ProcessEvent(EventAction.PROCESS, reportId, ReportOptions.None, emptyMap(), emptyList(), at)
        val message = event.toQueueMessage()
        val returnEvent = parsePrimeRouterQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }
}