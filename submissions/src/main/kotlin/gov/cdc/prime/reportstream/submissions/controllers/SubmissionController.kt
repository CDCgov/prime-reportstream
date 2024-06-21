package gov.cdc.prime.reportstream.submissions.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class SubmissionController {
    @PostMapping("/api/v1/reports")
    fun submitReport(
        @RequestHeader headers: Map<String, String>,
        @RequestBody data: Map<String, Any>,
    ): ResponseEntity<Any> {
        val headerValidationResult = validateHeaders(headers)
        if (headerValidationResult != null) {
            return headerValidationResult
        }
    }

    private fun validateHeaders(headers: Map<String, String>): ResponseEntity<String>? {
        val client = headers["Client_id"]
        val contentType = headers["Content-Type"]
        val acceptableContentTypes = listOf("application/hl7-v2", "application/ fhir+ndjson")

        if (client.isNullOrEmpty()) {
            return ResponseEntity.badRequest().body("Missing required header: Client_id.")
        }

        if (contentType !in acceptableContentTypes) {
            return ResponseEntity
                .badRequest()
                .body("Invalid Content-Type header. Acceptable values include: $acceptableContentTypes")
        }

        return null
    }
}