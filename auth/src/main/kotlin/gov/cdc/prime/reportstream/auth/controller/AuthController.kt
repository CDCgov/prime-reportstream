package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.service.ProxyURIStrategy
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.gateway.webflux.ProxyExchange
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
class AuthController @Autowired constructor(
    private val proxyURIStrategy: ProxyURIStrategy,
) : Logging {

    @RequestMapping("**")
    suspend fun proxy(
        exchange: ServerWebExchange,
        proxy: ProxyExchange<ByteArray>,
        auth: BearerTokenAuthentication,
    ): ResponseEntity<ByteArray> {
        val sub = auth.tokenAttributes["sub"]
        val scopes = auth.tokenAttributes["scope"]

        logger.info("Token with sub=$sub and scopes=$scopes is authenticated with Okta")

        val uri = proxyURIStrategy.getTargetURI(exchange.request.uri)
        proxy.uri(uri.toString())

        logger.info("Proxying request to ${exchange.request.method} $uri")
        val response = proxy.forward().awaitSingle()
        logger.info("Proxy response from  ${exchange.request.method} $uri status=${response.statusCode}")

        return response
    }
}