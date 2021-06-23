package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

const val defaultBlobContainerName = "reports"

class BlobAccess(
    private val csvSerializer: CsvSerializer,
    private val hl7Serializer: Hl7Serializer,
    private val redoxSerializer: RedoxSerializer
) : Logging {
    private val defaultConnEnvVar = "AzureWebJobsStorage"

    // Basic info about a blob: its format, url in azure, and its sha256 hash
    data class BlobInfo(
        val format: Report.Format,
        val blobUrl: String,
        val digest: ByteArray,
    )

    fun uploadBody(report: Report): BlobInfo {
        val (bodyFormat, blobBytes) = createBodyBytes(report)
        val digest = sha256Digest(blobBytes)
        val blobUrl = uploadBlob(report.name, blobBytes)
        return BlobInfo(bodyFormat, blobUrl, digest)
    }

    private fun createBodyBytes(report: Report): Pair<Report.Format, ByteArray> {
        val outputStream = ByteArrayOutputStream()
        when (report.bodyFormat) {
            Report.Format.INTERNAL -> csvSerializer.writeInternal(report, outputStream)
            // HL7 needs some additional configuration we set on the translation in organization
            Report.Format.HL7 -> hl7Serializer.write(report, outputStream)
            Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, outputStream)
            Report.Format.CSV -> csvSerializer.write(report, outputStream)
            Report.Format.REDOX -> redoxSerializer.write(report, outputStream)
        }
        val contentBytes = outputStream.toByteArray()
        return Pair(report.bodyFormat, contentBytes)
    }

    private fun uploadBlob(
        fileName: String,
        bytes: ByteArray,
        blobContainerName: String = defaultBlobContainerName,
        blobConnEnvVar: String = defaultConnEnvVar
    ): String {
        val blobClient = getBlobContainer(blobContainerName, blobConnEnvVar).getBlobClient(fileName)
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

    /**
     * Returns the blobURL of the newly created copy.
     * Right now, only copies from our internal reports blob store.
     */
    fun copyBlob_OLD(fromBlobUrl: String, toBlobContainer: String, toBlobConnEnvVar: String): String {
        val fromBlobClient = getBlobClient(fromBlobUrl)
        val blobContainer = getBlobContainer(toBlobContainer, toBlobConnEnvVar)
        logger.info("Copying from blob ${fromBlobClient.blobName}")
        val toBlobClient = blobContainer.getBlobClient(fromBlobClient.blobName, toBlobConnEnvVar)
        toBlobClient.copyFromUrl(fromBlobUrl) // returns a uuid 'copy id'.  Not sure what use it it.
        return toBlobClient.blobUrl
    }

    fun copyBlob(fromBlobUrl: String, toBlobContainer: String, toBlobConnEnvVar: String): String {
        val fromBytes = this.downloadBlob(fromBlobUrl)
        logger.info("Ready to copy ${fromBytes.size} bytes from $fromBlobUrl")
        val fromBlobClient = getBlobClient(fromBlobUrl) // only used to get the filename.
        val toFilename = fromBlobClient.blobName
        logger.info("New blob filename will be $toFilename")
        val toBlobUrl = uploadBlob(toFilename, fromBytes, toBlobContainer, toBlobConnEnvVar)
        logger.info("New blob URL is $toBlobUrl")
        return toBlobUrl
    }

    fun deleteBlob(blobUrl: String) {
        getBlobClient(blobUrl).delete()
    }

    fun checkConnection(blobConnEnvVar: String = defaultConnEnvVar) {
        val blobConnection = System.getenv(blobConnEnvVar)
        BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
    }

    fun getBlobContainer(name: String, blobConnEnvVar: String = defaultConnEnvVar): BlobContainerClient {
        val blobConnection = System.getenv(blobConnEnvVar)
        val blobServiceClient = BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
        val containerClient = blobServiceClient.getBlobContainerClient(name)
        if (!containerClient.exists()) containerClient.create()
        return containerClient
    }

    fun getBlobClient(blobUrl: String, blobConnEnvVar: String = defaultConnEnvVar): BlobClient {
        val blobConnection = System.getenv(blobConnEnvVar)
        return BlobClientBuilder().connectionString(blobConnection).endpoint(blobUrl).buildClient()
    }

    companion object {

        /**
         * Create a hex string style of a digest.
         */
        fun digestToString(digest: ByteArray): String {
            return digest.joinToString(separator = "", limit = 40) { Integer.toHexString(it.toInt()) }
        }

        fun sha256Digest(input: ByteArray): ByteArray {
            return hashBytes("SHA-256", input)
        }

        fun hashBytes(type: String, input: ByteArray): ByteArray {
            return MessageDigest
                .getInstance(type)
                .digest(input)
        }
    }
}