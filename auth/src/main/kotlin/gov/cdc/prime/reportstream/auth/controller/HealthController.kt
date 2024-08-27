package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.model.ApplicationStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.TimeSource

@RestController
class HealthController @Autowired constructor(
    timeSource: TimeSource,
) {

    private val applicationStart = timeSource.markNow()

    @GetMapping("/health", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun health(): ApplicationStatus {
        val uptime = applicationStart.elapsedNow().toString()
        return ApplicationStatus("auth", "ok", uptime)
    }

    @GetMapping("/test")
    suspend fun test(authentication: BearerTokenAuthentication): String {
        return authentication.tokenAttributes["sub"].toString() + " is the subject"
    }
}