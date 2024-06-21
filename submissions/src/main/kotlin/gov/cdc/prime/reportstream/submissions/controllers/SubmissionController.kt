package gov.cdc.prime.reportstream.submissions.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.*

@RestController
class SubmissionController {
    @PostMapping("/api/v1/reports")
    fun submitReport(
        @RequestHeader headers: Map<String, String>,
        @RequestBody data: Map<String, Any>,
    ): ResponseEntity<Any> {
        // find and return any issues with the headers
        val headerValidationResult = validateHeaders(headers)
        if (headerValidationResult != null) {
            return headerValidationResult
        }

        // Process the data (e.g., save to database)
        // For this example, let's assume we've saved it and got an ID
        val reportId = UUID.randomUUID()

        val response =
            CreationResponse(
                reportId,
                "Received",
                OffsetDateTime.now(),
            )

        return ResponseEntity(response, HttpStatus.CREATED)
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

data class CreationResponse(
    val reportId: UUID,
    val overallStatus: String,
    val timestamp: OffsetDateTime,
)