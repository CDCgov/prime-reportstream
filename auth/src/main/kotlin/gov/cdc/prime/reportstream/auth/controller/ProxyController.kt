package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.service.ProxyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URISyntaxException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
class ProxyController {
    @Autowired
    var service: ProxyService? = null
    @RequestMapping("/api/waters/**")
    @Throws(URISyntaxException::class)
    fun sendRequestToSPM(
        @RequestBody(required = false) body: String?,
        method: HttpMethod?, request: HttpServletRequest?, response: HttpServletResponse?,
    ): ResponseEntity<String> {
        return service.processProxyRequest(body, method, request, response, UUID.randomUUID().toString())
    }
}