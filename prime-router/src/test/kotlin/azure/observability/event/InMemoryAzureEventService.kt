package gov.cdc.prime.router.azure.observability.event

import java.util.Collections

/**
 * Simple Event service that holds on to tracked events in-memory for test assertions
 */
class InMemoryAzureEventService : AzureEventService {

    private val events = mutableListOf<AzureCustomEvent>()

    override fun trackEvent(event: AzureCustomEvent) {
        events.add(event)
    }

    fun getEvents(): List<AzureCustomEvent> {
        return Collections.unmodifiableList(events)
    }

    fun clear() {
        events.clear()
    }
}