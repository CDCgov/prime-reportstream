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

    @PreAuthorize("hasAuthority('SCOPE_sender')")
    @GetMapping("/authorized/health")
    suspend fun authorizedHealth(): String {
        return "up"
    }

    @PreAuthorize("hasAuthority('SCOPE_bad')")
    @GetMapping("/authorized/health2")
    suspend fun unauthorizedHealth(): String {
        return "up"
    }
}