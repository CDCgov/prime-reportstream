package gov.cdc.prime.reportstream.auth.config

import gov.cdc.prime.reportstream.auth.model.Environment
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.TimeSource

/**
 * Simple class to automatically read configuration from application.yml (or environment variable overrides)
 */
@Configuration
@ConfigurationProperties(prefix = "app")
class ApplicationConfig(
    var environment: Environment = Environment.LOCAL
) {

    @Bean
    fun timeSource(): TimeSource {
        return TimeSource.Monotonic
    }

}