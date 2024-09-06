package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.service.ProxyURIStrategy
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOpaqueToken
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureWebTestClient
class AuthControllerTest @Autowired constructor(
    private val webTestClient: WebTestClient,
    @MockBean private val mockedUriStrategy: ProxyURIStrategy
) {

    private val server: MockWebServer = MockWebServer()

    @BeforeEach
    fun setUp() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful proxy`() {
        server.enqueue(
            MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .setBody("hello world!")
        )

        val incomingUri = URI("/service/path")
        val outgoingUri = URI(server.url("/path").toString())
        given(mockedUriStrategy.getTargetURI(incomingUri)).willReturn(outgoingUri)

        webTestClient
            .mutateWith(csrf())
            .mutateWith(
                mockOpaqueToken()
                .attributes { map ->
                    map["sub"] = "sub"
                    map["scope"] = listOf("scope1", "scope2")
                }
            )
            .post()
            .uri("/service/path")
            .accept(MediaType.TEXT_PLAIN)
            .headers { headers ->
                headers.add("x-test-header", "Pass this along")
            }
            .bodyValue("body")
            .exchange()
            // assertions on the response received from the mock server
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.TEXT_PLAIN)
            .expectBody(String::class.java).isEqualTo("hello world!")

        // assertions on recorded request to proxy
        val recordedRequest = server.takeRequest()
        assertEquals(
            recordedRequest.headers.get("x-test-header"),
            "Pass this along"
        )
        assertEquals(
            recordedRequest.body.readString(Charset.defaultCharset()),
            "body"
        )
    }

    @Test
    fun `authorization fails in proxied server`() {
        server.enqueue(MockResponse().setResponseCode(403))

        given(mockedUriStrategy.getTargetURI(any()))
            .willReturn(URI(server.url("/").toString()))

        webTestClient
            .mutateWith(csrf())
            .mutateWith(
                mockOpaqueToken()
                .attributes { map ->
                    map["sub"] = "sub"
                    map["scope"] = listOf("scope1", "scope2")
                }
            )
            .post()
            .uri("/random")
            .accept(MediaType.TEXT_PLAIN)
            .headers { headers ->
                headers.add("x-test-header", "Pass this along")
            }
            .bodyValue("body")
            .exchange()
            // assertions on the response received from the mock server
            .expectStatus().isForbidden

        // assertions on recorded request to proxy
        val recordedRequest = server.takeRequest()
        assertEquals(
            recordedRequest.headers.get("x-test-header"),
            "Pass this along"
        )
        assertEquals(
            recordedRequest.body.readString(Charset.defaultCharset()),
            "body"
        )
    }

    @Test
    fun `authentication fails`() {
        given(mockedUriStrategy.getTargetURI(any()))
            .willReturn(URI(server.url("/").toString()))

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/random")
            .exchange()
            .expectStatus().isUnauthorized

        // no request should be made to server
        assertEquals(server.requestCount, 0)
    }
}