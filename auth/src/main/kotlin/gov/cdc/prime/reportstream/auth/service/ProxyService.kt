package gov.cdc.prime.reportstream.auth.service

import org.springframework.http.ResponseEntity
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.HttpMethod
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URISyntaxException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class ProxyService {
    var domain: String = "example.com"
    private static
    val logger: Logger = LogManager.getLogger(ProxyService::class.java)

    @Throws(
        URISyntaxException::class
    )
    fun processProxyRequest(
        body: String?,
        method: HttpMethod?, request: HttpServletRequest, response: HttpServletResponse?, traceId: String?,
    ): ResponseEntity<String> {
        ThreadContext.put("traceId", traceId)
        val requestUrl = request.requestURI
        //log if required in this line
        var uri: URI = URI("https", null, domain, -1, null, null, null)
        // replacing context path form urI to match actual gateway URI
        uri = UriComponentsBuilder.fromUri(uri)
            .path(requestUrl)
            .query(request.queryString)
            .build(true).toUri()
        val headers: HttpHeaders = HttpHeaders()
        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            headers.set(headerName, request.getHeader(headerName))
        }
        headers.set("TRACE", traceId)
        headers.remove(HttpHeaders.ACCEPT_ENCODING)
        val httpEntity: HttpEntity<String> = HttpEntity(body, headers)
        val factory: ClientHttpRequestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
        val restTemplate = RestTemplate(factory)
        try {
            val serverResponse: ResponseEntity<String> =
                restTemplate.exchange(uri, method, httpEntity, String::class.java)
            val responseHeaders: HttpHeaders = HttpHeaders()
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.headers[HttpHeaders.CONTENT_TYPE])
            logger.info(serverResponse)
            return serverResponse
        } catch (e: HttpStatusCodeException) {
            logger.error(e.message)
            return ResponseEntity.status(e.rawStatusCode)
                .headers(e.responseHeaders)
                .body(e.responseBodyAsString)
        }
    }
}