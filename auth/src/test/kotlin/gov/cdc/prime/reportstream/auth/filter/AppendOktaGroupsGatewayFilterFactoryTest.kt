package gov.cdc.prime.reportstream.auth.filter

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import gov.cdc.prime.reportstream.auth.config.TestOktaClientConfig
import gov.cdc.prime.reportstream.auth.service.OktaGroupsService
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOpaqueToken
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * The testing strategy here was modeled off how Spring Cloud Gateway tested their own add request filter.
 *
 * A Wiremock endpoint is set up to take the request header and return it as a part of the body. We then check
 * that the body matches what we expect.
 */
@ExtendWith(SpringExtension::class)
@AutoConfigureWebTestClient
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@Import(TestOktaClientConfig::class)
class AppendOktaGroupsGatewayFilterFactoryTest @Autowired constructor(
    private val client: WebTestClient,
    private val oktaGroupsService: OktaGroupsService,
) {

    @TestConfiguration
    class Config(
        @Value("\${wiremock.server.port}") val port: Int,
    ) {

        @Bean
        fun oktaGroupsService(): OktaGroupsService {
            return mockk()
        }

        @Bean
        fun appendOktaGroupsGatewayFilterFactory(): AppendOktaGroupsGatewayFilterFactory {
            return AppendOktaGroupsGatewayFilterFactory(oktaGroupsService())
        }

        @Bean
        fun testRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
            val filterFactory = appendOktaGroupsGatewayFilterFactory()
            return builder.routes()
                .route("wiremock_route") {
                    it
                        .path("/get")
                        .filters { filter ->
                            filter.filter(filterFactory.apply())
                        }
                        .uri("http://localhost:$port")
                }
                .build()
        }
    }

    @BeforeEach
    fun setUp() {
        stubFor(
            get(
                urlEqualTo("/get")
            )
                .willReturn(
                    aResponse()
                        .withTransformers("response-template")
                        .withBody("{{ request.headers.Okta-Groups }}") // reflect the request header back into the body
                )
        )
    }

    @Test
    fun `Successfully pass Okta-Groups header when sender scope is present`() {
        val expectedJwt = "okta-groups-jwt"

        coEvery { oktaGroupsService.generateOktaGroupsJWT(any()) }
            .returns(expectedJwt)

        client
            .mutateWith(
                mockOpaqueToken()
                    .attributes { map ->
                        map["sub"] = "appId"
                        map["scope"] = listOf("sender")
                    }
            )
            .get()
            .uri("/get")
            .exchange()
            .expectBody(String::class.java)
            .isEqualTo(expectedJwt)
    }

    @Test
    fun `Do not pass Okta-Groups header when organization scope is present`() {
        client
            .mutateWith(
                mockOpaqueToken()
                    .attributes { map ->
                        map["sub"] = "email@cdc.gov"
                        map["scope"] = listOf("openid", "email")
                        map["organization"] = listOf("org")
                    }
            )
            .get()
            .uri("/get")
            .exchange()
            .expectBody()
            .isEmpty
    }
}