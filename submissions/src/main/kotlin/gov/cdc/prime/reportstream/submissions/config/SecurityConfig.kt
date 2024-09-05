package gov.cdc.prime.reportstream.submissions.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Allow all requests sans any authn/authz checks.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
//                    .requestMatchers("/authorized/**").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer {
                it.jwt { }
            }

        return http.build()
    }
}