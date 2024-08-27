package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.helper.ErrorResponseHelper
import gov.cdc.prime.reportstream.auth.helper.ProxyURIStrategy
import gov.cdc.prime.reportstream.auth.service.AuthenticationService
import gov.cdc.prime.reportstream.auth.service.ProxyService
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.gateway.webflux.ProxyExchange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
class AuthController @Autowired constructor(
    private val proxyService: ProxyService,
    private val authenticationService: AuthenticationService,
    private val errorResponseHelper: ErrorResponseHelper,
    private val proxyURIStrategy: ProxyURIStrategy,
) : Logging {

    @RequestMapping("**")
    suspend fun proxy(
        exchange: ServerWebExchange,
        proxy: ProxyExchange<ByteArray>,
    ): ResponseEntity<ByteArray> {
        val uri = proxyURIStrategy.getURL(exchange.request.uri)
        proxy.uri(uri.toString())

        val proxyResponse = proxy.forward().awaitSingle()

        logger.info(proxyResponse.statusCode)
        logger.info(proxyResponse.headers)
        proxyResponse.body?.let { logger.info(it.decodeToString()) }

        return proxyResponse

//        val authHeader = exchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull()
//        return authenticationService.authenticate(authHeader).fold({
//            errorResponseHelper.authenticationErrorToResponseEntity(it)
//        }, { _ ->
//            proxyService.forwardRequest(exchange.request, body)
//        })
//        return proxyService.forwardRequest(exchange.request, body)
    }
}