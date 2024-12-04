package gov.cdc.prime.reportstream.auth.config

import com.okta.sdk.client.AuthorizationMode
import com.okta.sdk.client.Clients
import com.okta.sdk.resource.api.ApplicationGroupsApi
import com.okta.sdk.resource.client.ApiClient
import gov.cdc.prime.reportstream.shared.StringUtilities.base64Decode
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class OktaClientConfig(
    private val oktaClientProperties: OktaClientProperties,
) {

    @Bean
    fun apiClient(): ApiClient {
        return Clients.builder()
            .setOrgUrl(oktaClientProperties.orgUrl)
            .setAuthorizationMode(AuthorizationMode.PRIVATE_KEY)
            .setClientId(oktaClientProperties.clientId)
            .setScopes(oktaClientProperties.requiredScopes)
            .setPrivateKey(oktaClientProperties.apiPrivateKey)
            // .setCacheManager(...) TODO: investigate caching since groups don't often change
            .build()
    }

    @Bean
    fun applicationGroupsApi(): ApplicationGroupsApi {
        return ApplicationGroupsApi(apiClient())
    }

    @ConfigurationProperties(prefix = "okta.admin-client")
    data class OktaClientProperties(
        val orgUrl: String,
        val clientId: String,
        val requiredScopes: Set<String>,
        private val apiEncodedPrivateKey: String,
    ) {
        // PEM encoded format
        val apiPrivateKey = apiEncodedPrivateKey.base64Decode()
    }
}