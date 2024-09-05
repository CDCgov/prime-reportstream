package gov.cdc.prime.reportstream.submissions.controllers

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/health")
    suspend fun health(): String {
        return "up"
    }
}