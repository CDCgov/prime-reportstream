package gov.cdc.prime.reportstream.auth.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.TimeSource

@Configuration
@EnableConfigurationProperties(ProxyConfigurationProperties::class)
class ApplicationConfig @Autowired constructor(
    val proxyConfig: ProxyConfigurationProperties,
) {

    @Bean
    fun timeSource(): TimeSource {
        return TimeSource.Monotonic
    }
}

@ConfigurationProperties("proxy")
data class ProxyConfigurationProperties(
    val pathMappings: List<ProxyPathMapping>,
)

data class ProxyPathMapping(
    val baseUrl: String,
    val pathPrefix: String,
)