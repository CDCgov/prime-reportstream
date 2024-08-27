package gov.cdc.prime.reportstream.auth.service

import gov.cdc.prime.reportstream.auth.config.ApplicationConfig
import gov.cdc.prime.reportstream.auth.helper.ProxyURIStrategy
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.toEntity

@Service
class ProxyService @Autowired constructor(
    private val applicationConfig: ApplicationConfig,
    private val proxyWebClient: WebClient,
    private val proxyURIStrategy: ProxyURIStrategy,
) : Logging {

    suspend fun forwardRequest(
        incomingRequest: ServerHttpRequest,
        body: ByteArray?,
    ): ResponseEntity<ByteArray> {
        val uri = proxyURIStrategy.getURL(incomingRequest.uri)
        val method = incomingRequest.method

        val outgoingRequest = proxyWebClient
            .method(method)
            .uri(uri)
            .headers { it.addAll(incomingRequest.headers) }
        // TODO: add additional X-Forward proxy headers?

        body?.let { outgoingRequest.body(BodyInserters.fromValue(it)) }

        logger.info("Proxying request to $uri")
        return outgoingRequest.awaitExchange { clientResponse ->
            logger.info("Proxy response from $uri has status ${clientResponse.statusCode()}")
            clientResponse.toEntity<ByteArray>().awaitSingle()
        }
    }
}