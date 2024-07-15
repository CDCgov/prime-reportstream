package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.data.tables.models.TableEntity
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.queue.QueueServiceClient
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import gov.cdc.prime.reportstream.submissions.ReportReceivedEvent
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
class SubmissionController(
    private val blobServiceClient: BlobServiceClient,
    private val queueServiceClient: QueueServiceClient,
    private val telemetryClient: TelemetryClient,
    private val tableClient: TableClient,
    @Value("\${azure.storage.container-name}") private val containerName: String = "receive",
    @Value("\${azure.storage.queue-name}") private val queueName: String = "elr-fhir-convert",
) {

    // Use of consumes limits the options for Content-Type to only these values
    @PostMapping("/api/v1/reports", consumes = ["application/hl7-v2", "application/fhir+ndjson"])
    fun submitReport(
        @RequestHeader headers: Map<String, String>,
        @RequestHeader("Content-Type") contentType: String,
        @RequestHeader("client_id") clientId: String,
        @RequestBody data: String,
    ): ResponseEntity<*> {
        val reportId = UUID.randomUUID()
        val reportReceivedTime = OffsetDateTime.now()
        val status = "Received"
        try {

            // Convert data to ByteArray
            val dataByteArray = data.toByteArray()

            // Upload to blob storage
            val blobContainerClient = blobServiceClient.getBlobContainerClient(containerName)
            val blobClient = blobContainerClient.getBlobClient(formBlobName(reportId, headers))
            blobClient.upload(dataByteArray.inputStream(), dataByteArray.size.toLong())

            // Create the message for the queue
            val message = mapOf(
                "reportId" to reportId.toString(),
                "blobUrl" to blobClient.blobUrl,
                "headers" to filterHeaders(headers)
            )
            val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val messageString = objectMapper.writeValueAsString(message)

            // Upload to Queue
            val queueClient = queueServiceClient.getQueueClient(queueName)
            queueClient.createIfNotExists()
            queueClient.sendMessage(messageString)

            // Insert into Table
            // TableEntity() sets PartitionKey and RowKey. Both are required by azure and combine to create the PK
            val tableEntity = TableEntity(reportId.toString(), reportId.toString())
            val tableProperties = mapOf(
                "report_received_time" to reportReceivedTime.toString(),
                "report_accepted_time" to reportReceivedTime.toString(), // Will be updated when the report is accepted
                "report_id" to reportId.toString(),
                "status" to status
            )
            tableClient.createEntity(tableEntity.setProperties(tableProperties))

            // Create and publish custom event
            val reportReceivedEvent = ReportReceivedEvent(
                timeStamp = reportReceivedTime,
                reportId = reportId,
                parentReportId = reportId,
                rootReportId = reportId,
                headers = filterHeaders(headers),
                senderIP = headers["x-azure-clientip"].toString(),
                fileSize = headers["content-length"].toString(),
                blobUrl = blobClient.blobUrl
            )

            // Log to Application Insights
            telemetryClient.trackEvent(
                "ReportReceivedEvent",
                mapOf("event" to objectMapper.writeValueAsString(reportReceivedEvent)),
                null)
            telemetryClient.flush()

            val response =
                CreationResponse(
                    reportId,
                    status,
                    OffsetDateTime.now(),
                )

            return ResponseEntity(response, HttpStatus.CREATED)
        } catch (e: Exception) {
            return ResponseEntity("Internal Server Error: ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun filterHeaders(headers: Map<String, String>): Map<String, String> {
        val headersToInclude = listOf("client_id", "Content-Type", "payloadName")
        return headers.filter { it.key in headersToInclude }
    }

    private fun formBlobName(
        reportId: UUID,
        headers: Map<String, String>,
    ): String {
        val senderName = headers["client_id"]?.lowercase()
        return when (headers["Content-Type"]?.lowercase()) {
            "application/hl7-v2" -> "receive/$senderName/$reportId.hl7"
            "application/fhir+ndjson" -> "receive/$senderName/$reportId.fhir"
            else -> "receive/$senderName/$reportId"
                // throw IllegalArgumentException("Unsupported content-type: $contentType")
        }
    }
}

data class CreationResponse(val reportId: UUID, val overallStatus: String, val timestamp: OffsetDateTime)