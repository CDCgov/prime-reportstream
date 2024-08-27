package gov.cdc.prime.reportstream.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun oktaWebClient() = WebClient.create()

    @Bean
    fun proxyWebClient() = WebClient.create()
}