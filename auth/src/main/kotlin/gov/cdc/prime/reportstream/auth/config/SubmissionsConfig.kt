package gov.cdc.prime.reportstream.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * Configuration for Submissions microservice
 */
@ConfigurationProperties(prefix = "submissions")
data class SubmissionsConfig @ConstructorBinding constructor(
    val baseUrl: String,
)