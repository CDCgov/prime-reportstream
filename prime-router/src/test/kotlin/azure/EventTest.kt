package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class EventTest {
    @Test
    fun `test reportEvent encode and decode`() {
        val event = ReportEvent(Event.EventAction.SEND, UUID.randomUUID(), false)
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test reportEvent encode and decode with time`() {
        val event = ReportEvent(Event.EventAction.SEND, UUID.randomUUID(), false, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test reportEvent encode and decode as empty`() {
        val event = ReportEvent(Event.EventAction.SEND, UUID.randomUUID(), true)
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test reportEvent encode and decode as empty with time`() {
        val event = ReportEvent(Event.EventAction.SEND, UUID.randomUUID(), true, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test receiverEvent encode and decode`() {
        val event = BatchEvent(Event.EventAction.BATCH, "test", false)
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test batchEvent encode and decode with time`() {
        val event = BatchEvent(Event.EventAction.BATCH, "test", false, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test batchEvent encode and decode as empty`() {
        val event = BatchEvent(Event.EventAction.BATCH, "test", true)
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test batchEvent encode and decode as empty with time`() {
        val event = BatchEvent(Event.EventAction.BATCH, "test", true, OffsetDateTime.now())
        val message = event.toQueueMessage()
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old reportEvent encode and decode`() {
        val reportId = UUID.randomUUID()
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        val message = "report&SEND&$reportId&false"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old reportEvent encode and decode with time`() {
        val reportId = UUID.randomUUID()
        val at = OffsetDateTime.now()
        val event = ReportEvent(Event.EventAction.SEND, reportId, false, at)
        val message = "report&SEND&$reportId&false&$at"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old reportEvent encode and decode as empty`() {
        val reportId = UUID.randomUUID()
        val event = ReportEvent(Event.EventAction.SEND, reportId, true)
        val message = "report&SEND&$reportId&true"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old reportEvent encode and decode as empty with time`() {
        val reportId = UUID.randomUUID()
        val at = OffsetDateTime.now()
        val event = ReportEvent(Event.EventAction.SEND, reportId, true, at)
        val message = "report&SEND&$reportId&true&$at"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old receiverEvent encode and decode`() {
        val event = BatchEvent(Event.EventAction.BATCH, "test", false)
        val message = "receiver&BATCH&test&false"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old batchEvent encode and decode with time`() {
        val at = OffsetDateTime.now()
        val event = BatchEvent(Event.EventAction.BATCH, "test", false, at)
        val message = "receiver&BATCH&test&false&$at"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old batchEvent encode and decode as empty`() {
        val event = BatchEvent(Event.EventAction.BATCH, "test", true)
        val message = "receiver&BATCH&test&true"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }

    @Test
    fun `test old batchEvent encode and decode as empty with time`() {
        val at = OffsetDateTime.now()
        val event = BatchEvent(Event.EventAction.BATCH, "test", true, at)
        val message = "receiver&BATCH&test&true&$at"
        val returnEvent = Event.parseQueueMessage(message)
        assertThat(returnEvent).isEqualTo(event)
    }
}