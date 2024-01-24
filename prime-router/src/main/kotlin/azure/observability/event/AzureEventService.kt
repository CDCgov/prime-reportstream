package gov.cdc.prime.router.azure.observability.event

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.logging.log4j.kotlin.Logging

/**
 * Service to emit events to Azure AppInsights
 */
class AzureEventService(
    private val telemetryClient: TelemetryClient = TelemetryClient(),
) : Logging {

    /**
     * Send event to Azure AppInsights using the Azure TelemetryClient
     */
    fun trackEvent(event: AzureCustomEvent) {
        val name = event.javaClass.simpleName
        logger.info("Sending event of type $name to Azure AppInsights")
        telemetryClient.trackEvent(name, event.serialize(), emptyMap())
    }
}