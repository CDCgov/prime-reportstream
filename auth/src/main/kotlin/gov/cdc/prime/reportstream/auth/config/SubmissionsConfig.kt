package gov.cdc.prime.reportstream.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Submissions microservice
 */
@Configuration
@ConfigurationProperties(prefix = "submissions")
data class SubmissionsConfig(
    var baseUrl: String = "http://localhost:8080"
)
