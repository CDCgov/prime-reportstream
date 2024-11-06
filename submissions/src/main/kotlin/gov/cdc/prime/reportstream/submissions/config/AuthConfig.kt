package gov.cdc.prime.reportstream.submissions.config

import gov.cdc.prime.reportstream.shared.auth.AuthZService
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWTReader
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthConfig(
    private val jwtKeyConfig: JWTKeyConfig,
) {

    @Bean
    fun oktaGroupsJWTReader(): OktaGroupsJWTReader {
        return OktaGroupsJWTReader(jwtKeyConfig.jwtEncodedPublicKeyJWK)
    }

    @Bean
    fun authZService(): AuthZService {
        return AuthZService(oktaGroupsJWTReader())
    }

    @ConfigurationProperties(prefix = "auth")
    data class JWTKeyConfig(
        val jwtEncodedPublicKeyJWK: String,
    )
}