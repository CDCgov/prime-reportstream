package gov.cdc.prime.reportstream.auth.config

import com.okta.sdk.resource.api.ApplicationGroupsApi
import com.okta.sdk.resource.client.ApiClient
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

/**
 * We don't want the Okta client to actually attempt to connect to staging Okta during tests
 */
@TestConfiguration
@Profile("test")
class TestOktaClientConfig {

    @Bean
    fun apiClient(): ApiClient = mockk()

    @Bean
    fun applicationGroupsApi(): ApplicationGroupsApi = mockk()
}