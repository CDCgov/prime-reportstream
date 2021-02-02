package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

const val blobContainerName = "reports"

class BlobAccess(
    private val csvSerializer: CsvSerializer,
    private val hl7Serializer: Hl7Serializer,
    private val redoxSerializer: RedoxSerializer
) {
    fun uploadBody(report: Report): Pair<String, String> {
        val (bodyFormat, blobBytes) = createBodyBytes(report)
        val blobUrl = uploadBlob(report.name, blobBytes)
        return Pair(bodyFormat, blobUrl)
    }

    private fun createBodyBytes(report: Report): Pair<String, ByteArray> {
        val outputStream = ByteArrayOutputStream()
        when (report.bodyFormat) {
            Report.Format.INTERNAL -> csvSerializer.writeInternal(report, outputStream)
            Report.Format.HL7 -> hl7Serializer.write(report, outputStream)
            Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, outputStream)
            Report.Format.CSV -> csvSerializer.write(report, outputStream)
            Report.Format.REDOX -> redoxSerializer.write(report, outputStream)
        }
        return Pair(report.bodyFormat.toString(), outputStream.toByteArray())
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

    fun checkConnection() {
        val blobConnection = System.getenv("AzureWebJobsStorage")
        BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
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