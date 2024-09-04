package gov.cdc.prime.reportstream.submissions.controllers

import com.azure.data.tables.TableClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.queue.QueueClient
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.reportstream.shared.Submission
import gov.cdc.prime.reportstream.submissions.ReportReceivedEvent
import gov.cdc.prime.reportstream.submissions.TelemetryService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException.UnsupportedMediaType
import java.io.IOException
import java.time.Instant
import java.util.UUID

private val logger = LoggerFactory.getLogger(SubmissionController::class.java)

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
    private val telemetryService: TelemetryService,
) {
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
        @RequestHeader("client_id") clientId: String,
        @RequestHeader("content-length") contentLength: String,
        @RequestHeader("x-azure-clientip") senderIp: String,
        @RequestHeader(value = "payloadName", required = false) payloadName: String?,
        @RequestBody data: String,
    ): ResponseEntity<*> {
        val reportId = UUID.randomUUID()
        val reportReceivedTime = Instant.now()
        val contentTypeMime = contentType.substringBefore(';')
        val status = "Received"
        val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        logger.info(
            "Received report submission: reportId=$reportId, contentType=$contentTypeMime" +
            ", clientId=$clientId${payloadName?.let { ", payloadName=$it" } ?: ""}}"
        )

        // Convert data to ByteArray
        val dataByteArray = data.toByteArray()
        val digest = BlobUtils.sha256Digest(dataByteArray)
        logger.debug("Converted report data to ByteArray")

        // Upload to blob storage
        val blobClient = blobContainerClient.getBlobClient(formBlobName(reportId, contentTypeMime, clientId))
        blobClient.upload(dataByteArray.inputStream(), dataByteArray.size.toLong())
        logger.info("Uploaded report to blob storage: blobUrl=${blobClient.blobUrl}")

        // Insert into Table
        // TableEntity() sets PartitionKey and RowKey. Both are required by azure and combine to create the PK
        val tableEntity = Submission(reportId.toString(), status, blobClient.blobUrl).toTableEntity()
        tableClient.createEntity(tableEntity)
        logger.info("Inserted report into table storage: reportId=$reportId")

        // Create and publish custom event
        val reportReceivedEvent = ReportReceivedEvent(
            timeStamp = reportReceivedTime,
            reportId = reportId,
            parentReportId = reportId,
            rootReportId = reportId,
            headers = filterHeaders(headers),
            sender = clientId,
            senderIP = senderIp,
            fileSize = contentLength,
            blobUrl = blobClient.blobUrl
        )
        logger.debug("Created ReportReceivedEvent")

        // Log to Application Insights
        telemetryService.trackEvent(
            "ReportReceivedEvent",
            mapOf("event" to objectMapper.writeValueAsString(reportReceivedEvent)),
        )
        telemetryService.flush()
        logger.info("Tracked ReportReceivedEvent with Application Insights")

        // Queue upload should occur as the last step ensuring the other steps successfully process
        // Create the message for the queue
        val message = QueueMessage.ReceiveQueueMessage(
            blobClient.blobUrl,
            BlobUtils.digestToString(digest),
            clientId.lowercase(),
            reportId,
            filterHeaders(headers).toMap(),
        ).serialize()
        logger.debug("Created message for queue")

        // Upload to Queue
        queueClient.createIfNotExists()
        queueClient.sendMessage(message)
        logger.info("Sent message to queue: queueName=${queueClient.queueName}")

        val response =
            CreationResponse(
                reportId,
                status,
                Instant.now(),
            )
        logger.info("Report submission successful: reportId=$reportId")

        return ResponseEntity(response, HttpStatus.CREATED)
    }

    /**
     * A centralized exception handler for handling exceptions that occur during report submission.
     * This class uses Spring's @ControllerAdvice to catch and handle exceptions thrown by any controller in the application.
     */
    @ControllerAdvice
    class SubmissionsExceptionHandler {

        /**
         * Handles exceptions of type UnsupportedMessageTypeException.
         *
         * @param e The UnsupportedMessageTypeException that was thrown.
         * @return A ResponseEntity with an error message and HTTP status code 415 (Unsupported Media Type).
         */
        @ExceptionHandler(UnsupportedMediaType::class)
        fun handleUnsupportedMessageTypeException(e: UnsupportedMediaType): ResponseEntity<String> {
            logger.warn("Unsupported message type exception: ${e.message}")
            return ResponseEntity("Unsupported Media Type: ${e.message}", HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        }

        /**
         * Handles any runtime exception that occurs during report submission.
         *
         * @param e The runtime exception that was thrown.
         * @return A ResponseEntity with an error message and HTTP status code 500 (Internal Server Error).
         */
        @ExceptionHandler(RuntimeException::class)
        fun handleRuntimeException(e: RuntimeException): ResponseEntity<String> {
            // Log the error message and stack trace for debugging purposes
            logger.error("Runtime exception during report submission: ${e.message}", e)

            // Return a response entity with a generic error message and internal server error status
            return ResponseEntity("Internal Server Error: ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR)
        }

        /**
         * Handles exceptions of type IllegalArgumentException.
         *
         * @param e The IllegalArgumentException that was thrown.
         * @return A ResponseEntity with an error message and HTTP status code 400 (Bad Request).
         */
        @ExceptionHandler(IllegalArgumentException::class)
        fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String> {
            // Log a warning message indicating the bad request
            logger.warn("Illegal argument exception: ${e.message}")

            // Return a response entity with a specific error message and bad request status
            return ResponseEntity("Bad Request: ${e.message}", HttpStatus.BAD_REQUEST)
        }

        /**
         * Handles any IO exception that occurs during report submission.
         *
         * @param e The IO exception that was thrown.
         * @return A ResponseEntity with an error message and HTTP status code 500 (Internal Server Error).
         */
        @ExceptionHandler(IOException::class)
        fun handleIOException(e: IOException): ResponseEntity<String> {
            // Log the error message and stack trace for debugging purposes
            logger.error("IO exception during report submission: ${e.message}", e)

            // Return a response entity with a generic error message and internal server error status
            return ResponseEntity("Internal Server Error: ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR)
        }

        /**
         * Handles exceptions of type MissingRequestHeaderException.
         *
         * @param e The MissingRequestHeaderException that was thrown.
         * @return A ResponseEntity with an error message and HTTP status code 400 (Bad Request).
         */
        @ExceptionHandler(MissingRequestHeaderException::class)
        fun handleMissingRequestHeaderException(e: MissingRequestHeaderException): ResponseEntity<String> {
            // Log a warning message indicating the missing header
            logger.warn("Missing request header exception: ${e.message}")

            // Return a response entity with a specific error message and bad request status
            return ResponseEntity("Bad Request: Missing required header: ${e.headerName}", HttpStatus.BAD_REQUEST)
        }
    }

    private fun filterHeaders(headers: Map<String, String>): Map<String, String> {
        val headersToInclude =
            listOf("client_id", "content-type", "payloadname", "x-azure-clientip", "content-length")
        return headers.filter { it.key.lowercase() in headersToInclude }
    }

    private fun formBlobName(
        reportId: UUID,
        contentTypeMime: String,
        clientId: String,
    ): String {
        val senderName = clientId.lowercase()
        return when (contentTypeMime.lowercase()) {
            "application/hl7-v2" -> "receive/$senderName/$reportId.hl7"
            "application/fhir+ndjson" -> "receive/$senderName/$reportId.fhir"
            else -> throw IllegalArgumentException("Unsupported content-type: $this")
        }
    }
}

/**
 * Data class representing the response for a successful report creation.
 *
 * @property reportId the unique ID of the report
 * @property overallStatus the overall status of the report submission
 * @property timestamp the timestamp when the report was received
 */
data class CreationResponse(val reportId: UUID, val overallStatus: String, val timestamp: Instant)