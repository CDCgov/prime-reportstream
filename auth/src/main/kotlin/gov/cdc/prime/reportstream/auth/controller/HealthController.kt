package gov.cdc.prime.reportstream.auth.controller

import gov.cdc.prime.reportstream.auth.AuthApplicationConstants
import gov.cdc.prime.reportstream.auth.model.ApplicationStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.TimeSource

@RestController
class HealthController(
    timeSource: TimeSource,
) {

    private val applicationStart = timeSource.markNow()

    /**
     * Simple endpoint that returns a healthcheck with uptime
     */
    @GetMapping(
        AuthApplicationConstants.Endpoints.HEALTHCHECK_ENDPOINT_V1,
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun health(): ApplicationStatus {
        val uptime = applicationStart.elapsedNow().toString()
        return ApplicationStatus("auth", "ok", uptime)
    }
}