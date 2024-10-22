package gov.cdc.prime.reportstream.auth.config

import gov.cdc.prime.reportstream.auth.model.Environment
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.Bean
import kotlin.time.TimeSource

/**
 * Simple class to automatically read configuration from application.yml (or environment variable overrides)
 */
@ConfigurationProperties(prefix = "app")
data class ApplicationConfig @ConstructorBinding constructor(
    val environment: Environment,
) {

    @Bean
    fun timeSource(): TimeSource {
        return TimeSource.Monotonic
    }
}