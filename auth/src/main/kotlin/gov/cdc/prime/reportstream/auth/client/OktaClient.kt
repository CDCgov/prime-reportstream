package gov.cdc.prime.reportstream.auth.client

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import gov.cdc.prime.reportstream.auth.config.ApplicationConfig
import gov.cdc.prime.reportstream.auth.model.AuthenticationFailure
import gov.cdc.prime.reportstream.auth.model.IntrospectBody
import gov.cdc.prime.reportstream.auth.model.IntrospectResponse
import gov.cdc.prime.reportstream.auth.model.OktaFailure
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import java.util.Base64

interface OktaClient {
    suspend fun introspect(token: String): Either<AuthenticationFailure, IntrospectResponse>
}

@Service
@Profile("remoteVerification")
class OktaClientImpl @Autowired constructor(
    private val oktaWebClient: WebClient,
    private val applicationConfig: ApplicationConfig,
) : OktaClient, Logging {

    private val authHeader = createAuthHeader()

    override suspend fun introspect(token: String): Either<AuthenticationFailure, IntrospectResponse> {
        val uri = UriComponentsBuilder
            .fromUriString(applicationConfig.oktaConfig.baseUrl)
            // .path("/oauth2/$clientId/v1/introspect")
//            .path("/oauth2/ausekaai7gUuUtHda1d7/v1/introspect")
            .path("/oauth2/default/v1/introspect")
            .build()
            .toUri()

        val body = IntrospectBody(token)

        return oktaWebClient
            .post()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .bodyValue(body.formDataMap)
            .awaitExchange { clientResponse ->
                val responseBody = clientResponse.bodyToMono<IntrospectResponse>().awaitSingle()
                logger.info(responseBody.toString())
                if (clientResponse.statusCode() == HttpStatus.OK) {
                    responseBody.right()
                } else {
                    logger.error("Error introspecting response from Okta")
                    OktaFailure.left()
                }
            }
    }

    private fun createAuthHeader(): String {
        val encoded = Base64.getEncoder().encodeToString("$clientId:$secret".toByteArray())
        return "Basic $encoded"
    }
}

@Service
@Profile("passthrough")
class LocalOktaClientImpl : OktaClient, Logging {

    override suspend fun introspect(token: String): Either<AuthenticationFailure, IntrospectResponse> {
        logger.info("Bypassing token verification in local environment")
        return IntrospectResponse.localPassthrough.right()
    }
}