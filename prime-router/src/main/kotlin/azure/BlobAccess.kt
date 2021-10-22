package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobErrorCode
import com.azure.storage.blob.models.BlobStorageException
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.security.MessageDigest

const val defaultBlobContainerName = "reports"

class BlobAccess(
    private val csvSerializer: CsvSerializer,
    private val hl7Serializer: Hl7Serializer,
    private val redoxSerializer: RedoxSerializer
) : Logging {
    private val defaultConnEnvVar = "AzureWebJobsStorage"

    /**
     * Metadata of a blob container.
     */
    private data class BlobContainerMetadata(val name: String, val connectionString: String)

    /**
     * THe blob containers.
     */
    private val blobContainerClients = mutableMapOf<BlobContainerMetadata, BlobContainerClient>()

    // Basic info about a blob: its format, url in azure, and its sha256 hash
    data class BlobInfo(
        val format: Report.Format,
        val blobUrl: String,
        val digest: ByteArray
    ) {
        companion object {
            /**
             * Get the blob filename from a [blobUrl]
             * @return the blob filename
             * @throws MalformedURLException if the blob URL is malformed
             */
            fun getBlobFilename(blobUrl: String): String {
                return if (blobUrl.isNotBlank())
                    FilenameUtils.getName(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path) else ""
            }
        }
    }

    /**
     * Upload the [report] to the blob store using the [action] to determine a folder as needed.  A [subfolderName]
     * is optional and is added as a prefix to the blob filename.
     * @return the information about the uploaded blob
     */
    fun uploadBody(
        report: Report,
        subfolderName: String? = null,
        action: Event.EventAction = Event.EventAction.NONE
    ): BlobInfo {
        val (bodyFormat, blobBytes) = createBodyBytes(report)
        return uploadBody(bodyFormat, blobBytes, report.name, subfolderName, action)
    }

    /**
     * Upload a raw [blobBytes] in the [bodyFormat] for a given [reportName].  The [action] is used to determine
     * the folder to store the blob in.  A [subfolderName] name is optional.
     * @return the information about the uploaded blob
     */
    fun uploadBody(
        bodyFormat: Report.Format,
        blobBytes: ByteArray,
        reportName: String,
        subfolderName: String? = null,
        action: Event.EventAction = Event.EventAction.NONE
    ): BlobInfo {
        val subfolderNameChecked = if (subfolderName.isNullOrBlank()) "" else "$subfolderName/"
        val blobName = when (action) {
            Event.EventAction.RECEIVE -> "receive/$subfolderNameChecked$reportName"
            Event.EventAction.SEND -> "ready/$subfolderNameChecked$reportName"
            Event.EventAction.BATCH -> "batch/$subfolderNameChecked$reportName"
            else -> "other/$subfolderNameChecked$reportName"
        }
        val digest = sha256Digest(blobBytes)
        val blobUrl = uploadBlob(blobName, blobBytes)
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
        blobName: String,
        bytes: ByteArray,
        blobContainerName: String = defaultBlobContainerName,
        blobConnEnvVar: String = defaultConnEnvVar
    ): String {
        logger.info("Starting uploadBlob of $blobName")
        val blobClient = getBlobContainer(blobContainerName, blobConnEnvVar).getBlobClient(blobName)
        blobClient.upload(
            ByteArrayInputStream(bytes),
            bytes.size.toLong()
        )
        logger.info("Done uploadBlob of $blobName")
        return blobClient.blobUrl
    }

    fun downloadBlob(blobUrl: String): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.use { getBlobClient(blobUrl).download(it) }
        return stream.toByteArray()
    }

    fun copyBlob(fromBlobUrl: String, toBlobContainer: String, toBlobConnEnvVar: String): String {
        val fromBytes = this.downloadBlob(fromBlobUrl)
        logger.info("Ready to copy ${fromBytes.size} bytes from $fromBlobUrl")
        val toFilename = BlobInfo.getBlobFilename(fromBlobUrl)
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

    /**
     * Creates the blob container client for the given blob [name] and connection string (obtained from the
     * environment variable [blobConnEnvVar], or reuses an existing one.
     * @return the blob container client
     */
    fun getBlobContainer(name: String, blobConnEnvVar: String = defaultConnEnvVar): BlobContainerClient {
        val blobConnection = System.getenv(blobConnEnvVar)
        val blobContainerMetadata = BlobContainerMetadata(name, blobConnection)

        return if (blobContainerClients.containsKey(blobContainerMetadata)) {
            blobContainerClients[blobContainerMetadata]!!
        } else {
            val blobServiceClient = BlobServiceClientBuilder().connectionString(blobConnection).buildClient()
            val containerClient = blobServiceClient.getBlobContainerClient(name)
            try {
                if (!containerClient.exists()) containerClient.create()
            } catch (error: BlobStorageException) {
                // This can happen when there are concurrent calls to the API
                if (error.errorCode.equals(BlobErrorCode.CONTAINER_ALREADY_EXISTS)) {
                    logger.warn("Container $name already exists")
                } else {
                    throw error
                }
            }
            containerClient
        }
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