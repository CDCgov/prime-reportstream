package gov.cdc.prime.reportstream.auth.config

import gov.cdc.prime.reportstream.auth.model.Environment
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import java.time.Clock
import kotlin.time.TimeSource

/**
 * Simple class to automatically read configuration from application.yml (or environment variable overrides)
 */
@ConfigurationProperties(prefix = "app")
data class ApplicationConfig(val environment: Environment) {

    @Bean
    fun timeSource(): TimeSource = TimeSource.Monotonic

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}