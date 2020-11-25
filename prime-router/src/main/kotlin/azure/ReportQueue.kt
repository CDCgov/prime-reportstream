package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.Hl7Converter
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationService.Format
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Source
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.*


/**
 * Methods to enqueue and dequeue a Report to Azure storage.
 * The queue consists of a pair of header and body items in a Azure Queue and Blob containers
 *
 * @see gov.cdc.prime.router.Report
 * @see AzureStorage
 */
object ReportQueue {

    /**
     * The names of the logical queues
     */
    enum class Name {
        VALIDATED,
        PROCESSED,
        MERGED,
        SENT;

        val blobContainer get() = this.toString().toLowerCase()
        val queueName get() = this.toString().toLowerCase()
    }

    /**
     * Header represents Report metadata that is serialized into the queue storage
     */
    data class Header(
        val id: UUID,
        val schemaName: String, // eg, pdi-covid-19
        val sources: List<Source>, // eg. ReportSource(id:
        val destination: String,
        val format: Format,
        val createdDateTime: OffsetDateTime,
        val blobUrl: String, // URL of the blob in Azure storage.
    ) {
        constructor(report: Report, blobUrl: String) : this(
            report.id,
            report.schema.name,
            report.sources,
            report.destination?.fullName ?: "",
            getFormat(report),
            report.createdDateTime,
            blobUrl
        )

        fun copy(destination: String? = null, blobUrl: String? = null): Header {
            return Header(
                this.id,
                this.schemaName,
                this.sources,
                destination ?: this.destination,
                this.format,
                this.createdDateTime,
                blobUrl ?: this.blobUrl
            )
        }

        fun encode(): String {
            return objectMapper.writeValueAsString(this)
        }

        companion object {
            fun decode(message: String): Header {
                return objectMapper.readValue(message, Header::class.java)
            }
        }
    }

    fun sendReport(queueName: Name, report: Report): String {
        val blobUrl = uploadBlob(queueName.blobContainer, report)
        val message = Header(report, blobUrl).encode()
        AzureStorage.uploadMessage(queueName.queueName, message)
        return message
    }

    fun sendHeaderAndBody(queueName: Name, header: Header, body: ByteArray): String {
        val blobFileName = Report.formFileName(header.id, header.schemaName, header.format, header.createdDateTime)
        val blobUrl = uploadBlob(queueName.blobContainer, blobFileName, body)
        val message = header.copy(blobUrl = blobUrl).encode()
        AzureStorage.uploadMessage(queueName.queueName, message)
        return message
    }

    fun receiveReport(queueName: Name, message: String): Report {
        val (header, blobBytes) = receiveHeaderAndBody(queueName, message)
        return createReport(header, blobBytes)
    }

    fun receiveHeaderAndBody(queueName: Name, message: String): Pair<Header, ByteArray> {
        val header = Header.decode(message)
        val blobBytes = downloadBlob(header.blobUrl)
        return Pair(header, blobBytes)
    }

    fun createReport(header: Header, bytes: ByteArray): Report {
        val schema = Metadata.findSchema(header.schemaName) ?: error("Invalid schema in queue: ${header.schemaName}")
        val destination = Metadata.findService(header.destination)
        return when (header.format) {
            Format.CSV -> CsvConverter.read(schema, ByteArrayInputStream(bytes), header.sources, destination)
            else -> error("Unsupported read format")
        }
    }

    private val objectMapper = ObjectMapper().registerKotlinModule()

    init {
        objectMapper.registerModule(JavaTimeModule())
        Source.registerSubTypes(objectMapper)
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    private fun uploadBlob(containerName: String, report: Report): String {
        val blobFilename = Report.formFileName(report.id, report.schema.name, report.destination?.format, report.createdDateTime)
        val blobBytes = createBlobBytes(report)
        return uploadBlob(containerName, blobFilename, blobBytes)
    }

    private fun uploadBlob(containerName: String, fileName: String, bytes: ByteArray): String {
        val blobClient = AzureStorage
            .getBlobContainer(containerName)
            .getBlobClient(fileName)
        blobClient.upload(
            ByteArrayInputStream(bytes),
            bytes.size.toLong()
        )
        return blobClient.blobUrl
    }

    private fun downloadBlob(blobUrl: String): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.use { AzureStorage.getBlobClient(blobUrl).download(it) }
        return stream.toByteArray()
    }

    private fun createBlobBytes(report: Report): ByteArray {
        val outputStream = ByteArrayOutputStream()
        when (getFormat(report)) {
            Format.HL7 -> Hl7Converter.write(report, outputStream)
            Format.CSV -> CsvConverter.write(report, outputStream)
        }
        return outputStream.toByteArray()
    }

    private fun getFormat(report: Report): Format {
        return report.destination?.format ?: Format.CSV
    }
}