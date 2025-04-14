package gov.cdc.prime.reportstream.auth.config

import gov.cdc.prime.reportstream.auth.AuthApplicationConstants
import gov.cdc.prime.reportstream.auth.model.Environment
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Security configuration setup
 *
 * All incoming requests will require authentication via opaque token check
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(private val applicationConfig: ApplicationConfig) : Logging {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf { it.disable() } // TODO: re-enable after 16312
            .authorizeExchange { authorize ->
                authorize
                    // allow health endpoint without authentication
                    .pathMatchers(AuthApplicationConstants.Endpoints.HEALTHCHECK_ENDPOINT_V1).permitAll()

                // allow unauthenticated access to swagger on local environments
                if (applicationConfig.environment == Environment.LOCAL) {
                    logger.info("Allowing unauthenticated Swagger access at http://localhost:9000/swagger/ui.html")
                    authorize.pathMatchers("/swagger/**").permitAll()
                }

                // all other requests must be authenticated
                authorize.anyExchange().authenticated()
            }
            .oauth2ResourceServer {
                it.opaqueToken { }
            }

        return http.build()
    }
}