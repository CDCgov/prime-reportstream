package gov.cdc.prime.reportstream.submissions

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TelemetryService {
    private val logger = LoggerFactory.getLogger(TelemetryService::class.java)
    private var telemetryClient: TelemetryClient? = null

    init {
        val connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING")

        if (!connectionString.isNullOrBlank()) {
            telemetryClient = TelemetryClient()
        } else {
            telemetryClient = null
            logger.warn("Application Insights connection string is not set. Telemetry will not be sent.")
        }
    }

    fun trackEvent(name: String, properties: Map<String, String> = emptyMap()) {
        telemetryClient?.trackEvent(name, properties, null)
    }

    fun flush() {
        telemetryClient?.flush()
    }
}
