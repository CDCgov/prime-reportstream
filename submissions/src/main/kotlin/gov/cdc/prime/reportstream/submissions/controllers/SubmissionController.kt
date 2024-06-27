package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.storage.blob.BlobServiceClient
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value

@RestController
class SubmissionController(
    private val blobServiceClient: BlobServiceClient,
    @Value("\${azure.storage.container-name}") private val containerName: String
) {
    @PostMapping("/api/v1/reports")
    fun submitReport(
        @RequestHeader headers: Map<String, String>,
        @RequestBody data: Map<String, Any>,
    ): ResponseEntity<*> {
        // find and return any issues with the headers
        val headerValidationResult = validateHeaders(headers)
        if (headerValidationResult != null) {
            return headerValidationResult
        }

        // Convert data to ByteArray or suitable format
        val dataByteArray = data.toString().toByteArray()
        val reportId = UUID.randomUUID()

        val blobContainerClient = blobServiceClient.getBlobContainerClient(containerName)
        val blobClient = blobContainerClient.getBlobClient(formBlobName(reportId,headers))

        blobClient.upload(dataByteArray.inputStream(), dataByteArray.size.toLong())

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
        val acceptableContentTypes = listOf("application/hl7-v2", "application/fhir+ndjson")

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

    private fun formBlobName(
        reportId: UUID,
        headers: Map<String, String>
    ): String? {
        val senderName = headers["Client_id"]
        val contentType = headers["Content-Type"]

        return when(contentType) {
            "application/hl7-v2" ->
                "receive/$senderName/$reportId.hl7"
            "application/fhir+ndjson" ->
                "receive/$senderName/$reportId.fhir"

            else -> {null}
        }

    }
}

data class CreationResponse(
    val reportId: UUID,
    val overallStatus: String,
    val timestamp: OffsetDateTime,
)