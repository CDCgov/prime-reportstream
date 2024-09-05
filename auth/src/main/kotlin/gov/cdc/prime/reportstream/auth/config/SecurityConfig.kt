package gov.cdc.prime.reportstream.auth.config

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
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { authorize ->
                authorize
                    .pathMatchers("/health").permitAll() // allow health endpoint without authentication
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer {
                it.opaqueToken { }
            }

        return http.build()
    }
}