package gov.cdc.prime.router.azure

import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class EventTest {
    @Test
    fun `test reportEvent encode and decode`() {
        val event = ReportEvent(Event.Action.SEND, UUID.randomUUID())
        val message = event.toMessage()
        val returnEvent = Event.parse(message)
        assertEquals(event, returnEvent)
    }

    @Test
    fun `test reportEvent encode and decode with time`() {
        val event = ReportEvent(Event.Action.SEND, UUID.randomUUID(), OffsetDateTime.now())
        val message = event.toMessage()
        val returnEvent = Event.parse(message)
        assertEquals(event, returnEvent)
    }

    @Test
    fun `test receiverEvent encode and decode`() {
        val event = ReceiverEvent(Event.Action.SEND, "test")
        val message = event.toMessage()
        val returnEvent = Event.parse(message)
        assertEquals(event, returnEvent)
    }

    @Test
    fun `test receiverEvent encode and decode with time`() {
        val event = ReceiverEvent(Event.Action.SEND, "test", OffsetDateTime.now())
        val message = event.toMessage()
        val returnEvent = Event.parse(message)
        assertEquals(event, returnEvent)
    }
}