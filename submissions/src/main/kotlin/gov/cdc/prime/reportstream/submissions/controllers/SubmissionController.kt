package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.data.tables.models.TableEntity
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.queue.QueueClient
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import gov.cdc.prime.reportstream.submissions.ReportReceivedEvent
import java.time.OffsetDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController



/**
 * Controller for handling report submissions.
 *
 * This controller provides an endpoint for submitting reports in various formats.
 * The reports are processed and stored in Azure Blob Storage, queued for further processing,
 * and their metadata is saved in Azure Table Storage.
 */
@RestController
class SubmissionController(
    private val blobContainerClient: BlobContainerClient,
    private val queueClient: QueueClient,
    private val tableClient: TableClient,
    private val telemetryClient: TelemetryClient,
) {
    private val logger = LoggerFactory.getLogger(SubmissionController::class.java)

    /**
     * Submits a report.
     *
     * This endpoint accepts reports in HL7 V2 and FHIR NDJSON formats. The report data is uploaded
     * to Azure Blob Storage, queued for further processing, and its metadata is stored in Azure Table Storage.
     * A custom event is also tracked in Application Insights for monitoring.
     *
     * @param headers the HTTP headers of the request
     * @param contentType the content type of the report (must be "application/hl7-v2" or "application/fhir+ndjson")
     * @param clientId the ID of the client submitting the report. Should represent org.senderName
     * @param data the report data
     * @return a ResponseEntity containing the reportID, status, and timestamp
     */
    @PostMapping("/api/v1/reports", consumes = ["application/hl7-v2", "application/fhir+ndjson"])
    fun submitReport(
        @RequestHeader headers: Map<String, String>,
        @RequestHeader("Content-Type") contentType: String,
        @RequestHeader(value = "client_id", required = false) clientId: String?,
        @RequestBody data: String,
    ): ResponseEntity<*> {
        val reportId = UUID.randomUUID()
        val reportReceivedTime = OffsetDateTime.now()
        val status = "Received"
        logger.info("Received report submission: reportId=$reportId, contentType=$contentType, clientId=$clientId")

        try {

            val headerValidationResult = validateHeaders(clientId)
            if (headerValidationResult != null) {
                logger.warn("Header validation failed: $headerValidationResult")
                return headerValidationResult
            }

            // Convert data to ByteArray
            val dataByteArray = data.toByteArray()
            logger.debug("Converted report data to ByteArray")

            // Upload to blob storage
            val blobClient = blobContainerClient.getBlobClient(formBlobName(reportId, headers))
            blobClient.upload(dataByteArray.inputStream(), dataByteArray.size.toLong())
            logger.info("Uploaded report to blob storage: blobUrl=${blobClient.blobUrl}")

            // Create the message for the queue
            val message = mapOf(
                "reportId" to reportId.toString(),
                "blobUrl" to blobClient.blobUrl,
                "headers" to filterHeaders(headers)
            )
            val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val messageString = objectMapper.writeValueAsString(message)
            logger.debug("Created message for queue")

            // Upload to Queue
            queueClient.createIfNotExists()
            queueClient.sendMessage(messageString)
            logger.info("Sent message to queue: queueName=${queueClient.queueName}")

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
            logger.info("Inserted report into table storage: reportId=$reportId")

            // Create and publish custom event
            val reportReceivedEvent = ReportReceivedEvent(
                timeStamp = reportReceivedTime,
                reportId = reportId,
                parentReportId = reportId,
                rootReportId = reportId,
                headers = filterHeaders(headers),
                sender = clientId!!,
                senderIP = headers["x-azure-clientip"].toString(),
                fileSize = headers["content-length"].toString(),
                blobUrl = blobClient.blobUrl
            )
            logger.debug("Created ReportReceivedEvent")

            // Log to Application Insights
            telemetryClient.trackEvent(
                "ReportReceivedEvent",
                mapOf("event" to objectMapper.writeValueAsString(reportReceivedEvent)),
                null)
            telemetryClient.flush()
            logger.info("Tracked ReportReceivedEvent with Application Insights")

            val response =
                CreationResponse(
                    reportId,
                    status,
                    OffsetDateTime.now(),
                )
            logger.info("Report submission successful: reportId=$reportId")

            return ResponseEntity(response, HttpStatus.CREATED)
        } catch (e: Exception) {
            logger.error("Error during report submission: ${e.message}", e)
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

    private fun validateHeaders(clientId: String?): ResponseEntity<String>? {

        if (clientId.isNullOrEmpty()) {
            logger.warn("Missing required header: client_id")
            return ResponseEntity.badRequest().body("Missing required header: client_id.")
        }
        return null
    }
}

/**
 * Data class representing the response for a successful report creation.
 *
 * @property reportId the unique ID of the report
 * @property overallStatus the overall status of the report submission
 * @property timestamp the timestamp when the report was received
 */
data class CreationResponse(val reportId: UUID, val overallStatus: String, val timestamp: OffsetDateTime)