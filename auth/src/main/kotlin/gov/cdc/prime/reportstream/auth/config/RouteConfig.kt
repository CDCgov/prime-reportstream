package gov.cdc.prime.reportstream.auth.config

import gov.cdc.prime.reportstream.auth.AuthApplicationConstants
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class to set up route forwarding
 */
@Configuration
class RouteConfig(
    private val submissionsConfig: SubmissionsConfig
) {

    @Bean
    fun routes(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route {
                it
                    .path(AuthApplicationConstants.SubmissionsEndpoints.REPORTS_ENDPOINT_V1)
                    .uri(submissionsConfig.baseUrl)
            }
            .build()
    }

}