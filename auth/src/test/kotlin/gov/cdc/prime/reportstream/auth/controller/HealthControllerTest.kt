package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.AuthApplicationConstants
import gov.cdc.prime.reportstream.auth.config.TestOktaClientConfig
import gov.cdc.prime.reportstream.auth.model.ApplicationStatus
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.test.Test

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureWebTestClient
@Import(TestOktaClientConfig::class)
class HealthControllerTest @Autowired constructor(private val webTestClient: WebTestClient) {

    @Test
    fun `successful healthcheck`() {
        webTestClient
            .get()
            .uri(AuthApplicationConstants.Endpoints.HEALTHCHECK_ENDPOINT_V1)
            .exchange()
            .expectStatus().isOk
            .expectBody(ApplicationStatus::class.java)
    }
}