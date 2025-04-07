package gov.cdc.prime.router.azure.observability.event

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.logging.log4j.kotlin.Logging

/**
 * Service to emit events to Azure AppInsights
 */
interface AzureEventService {
    /**
     * Tracks an event to be later queried against in Azure AppInsights
     *
     * @param event the event to be tracked
     */
    fun trackEvent(event: AzureCustomEvent)

    fun trackEvent(eventName: ReportStreamEventName, event: AzureCustomEvent)
}

/**
 * Default implementation
 */
class AzureEventServiceImpl(private val telemetryClient: TelemetryClient = TelemetryClient()) :
    AzureEventService,
    Logging {

    /**
     * Send event to Azure AppInsights using the Azure TelemetryClient
     */
    override fun trackEvent(event: AzureCustomEvent) {
        val name = event.javaClass.simpleName
        logger.debug("Sending event of type $name to Azure AppInsights")
        telemetryClient.trackEvent(name, event.serialize(), emptyMap())
    }

    override fun trackEvent(eventName: ReportStreamEventName, event: AzureCustomEvent) {
        logger.debug("Sending event of type $eventName to Azure AppInsights")
        telemetryClient.trackEvent(eventName.name, event.serialize(), emptyMap())
    }
}

class InMemoryAzureEventService :
    AzureEventService,
    Logging {

    val events: MutableList<AzureCustomEvent> = mutableListOf()
    val reportStreamEvents = mutableMapOf<ReportStreamEventName, MutableList<AzureCustomEvent>>()

    override fun trackEvent(event: AzureCustomEvent) {
        val name = event.javaClass.simpleName
        logger.debug("Recording '$name' event in memory.")
        events.add(event)
    }

    override fun trackEvent(eventName: ReportStreamEventName, event: AzureCustomEvent) {
        logger.debug("Recording '$eventName' event in memory.")
        reportStreamEvents.getOrPut(eventName) {
            mutableListOf()
        }.add(event)
        events.add(event)
    }
}