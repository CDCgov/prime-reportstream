package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.Hl7Converter
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.OrganizationService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// Functions to store tables into blobs
object BlobStorage {

    fun getBlobContainer(name: String): BlobContainerClient {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        val blobServiceClient = BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
        val containerClient = blobServiceClient.getBlobContainerClient(name)
        if (!containerClient.exists()) containerClient.create()
        return containerClient
    }

    fun uploadBlob(container: BlobContainerClient, report: Report, organizationService: OrganizationService) {
        val blobName = "${report.name}.${organizationService.format.toExt()}"
        val outputStream = ByteArrayOutputStream()
        when (organizationService.format) {
            OrganizationService.TopicFormat.CSV -> CsvConverter.write(report, outputStream)
            OrganizationService.TopicFormat.HL7 -> Hl7Converter.write(report, outputStream)
        }
        val blobClient = container.getBlobClient(blobName)
        val outputBytes = outputStream.toByteArray()
        blobClient.upload(ByteArrayInputStream(outputBytes), outputBytes.size.toLong(), true)
    }
}