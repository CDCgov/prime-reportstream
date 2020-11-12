package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.Hl7Converter
import gov.cdc.prime.router.MappableTable
import gov.cdc.prime.router.Receiver
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Functions to store tables into blobs
object TableStorage {

    fun getBlobContainer(name: String): BlobContainerClient {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        val blobServiceClient = BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
        val containerClient = blobServiceClient.getBlobContainerClient(name)
        if (!containerClient.exists()) containerClient.create()
        return containerClient
    }

    fun uploadBlob(container: BlobContainerClient, table: MappableTable, receiver: Receiver) {
        val blobName = "${table.name}-${nowTimestamp()}.${receiver.format.toExt()}"
        val outputStream = ByteArrayOutputStream()
        when (receiver.format) {
            Receiver.TopicFormat.CSV -> CsvConverter.write(table, outputStream)
            Receiver.TopicFormat.HL7 -> Hl7Converter.write(table, outputStream)
        }
        val blobClient = container.getBlobClient(blobName)
        val outputBytes = outputStream.toByteArray()
        blobClient.upload(ByteArrayInputStream(outputBytes), outputBytes.size.toLong(), true)
    }

    private fun nowTimestamp(): String {
        val timestamp = OffsetDateTime.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("YYYYMMDDhhmmssZZZ")
        return formatter.format(timestamp)
    }

}