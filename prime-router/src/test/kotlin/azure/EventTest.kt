package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.Options
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
    fun `test valid options param sent`() {
        val result = Options.valueOfOrNone("CheckConnections")
        assertThat(result).isEqualTo(Options.CheckConnections)
    }

    @Test
    fun `test incorrect capitalization options param sent`() {
        val result = Options.valueOfOrNone("CHECKCONNECTIONS")
        assertThat(result).isEqualTo(Options.None)
    }

    @Test
    fun `test entirely invalid options param sent`() {
        val result = Options.valueOfOrNone("NoGood")
        assertThat(result).isEqualTo(Options.None)
    }
}