package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.Hl7Converter
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

const val blobContainerName = "reports"

class BlobAccess {
    fun uploadBody(report: Report): Pair<String, String> {
        val blobFilename = createBodyFilename(report)
        val (bodyFormat, blobBytes) = createBodyBytes(report)
        val blobUrl = uploadBlob(blobFilename, blobBytes)
        return Pair(bodyFormat, blobUrl)
    }

    private fun createBodyBytes(report: Report): Pair<String, ByteArray> {
        val outputStream = ByteArrayOutputStream()
        when (getBodyFormat(report)) {
            OrganizationService.Format.HL7 -> Hl7Converter.write(report, outputStream)
            OrganizationService.Format.CSV -> CsvConverter.write(report, outputStream)
        }
        return Pair(getBodyFormat(report).toString(), outputStream.toByteArray())
    }

    private fun createBodyFilename(report: Report): String {
        return "${report.name}.${report.destination?.format?.toExt() ?: OrganizationService.Format.CSV.toExt()}"
    }

    private fun getBodyFormat(report: Report): OrganizationService.Format {
        return report.destination?.format ?: OrganizationService.Format.CSV
    }

    private fun uploadBlob(fileName: String, bytes: ByteArray): String {
        val blobClient = getBlobContainer(blobContainerName).getBlobClient(fileName)
        blobClient.upload(
            ByteArrayInputStream(bytes),
            bytes.size.toLong()
        )
        return blobClient.blobUrl
    }

    fun downloadBlob(blobUrl: String): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.use { getBlobClient(blobUrl).download(it) }
        return stream.toByteArray()
    }

    fun deleteBlob(blobUrl: String) {
        getBlobClient(blobUrl).delete()
    }

    private fun getBlobContainer(name: String): BlobContainerClient {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        val blobServiceClient = BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
        val containerClient = blobServiceClient.getBlobContainerClient(name)
        if (!containerClient.exists()) containerClient.create()
        return containerClient
    }

    private fun getBlobClient(blobUrl: String): BlobClient {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        return BlobClientBuilder().connectionString(blobConnection).endpoint(blobUrl).buildClient()
    }
}