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
    private val csvSerializer: CsvSerializer? = null,
    private val hl7Serializer: Hl7Serializer? = null
) : Logging {

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
    fun generateBodyAndUploadReport(
        report: Report,
        subfolderName: String? = null,
        action: Event.EventAction = Event.EventAction.NONE,
        sendingApplicationReport: String? = null,
        receivingApplicationReport: String? = null,
        receivingFacilityReport: String? = null
    ): BlobInfo {
        val (bodyFormat, blobBytes) = createBodyBytes(
            report,
            sendingApplicationReport,
            receivingApplicationReport,
            receivingFacilityReport
        )
        return uploadBody(bodyFormat, blobBytes, report.name, subfolderName, action)
    }

    /**
     * Uses a serializer that matches the bodyFormat of the passed in [report] to generate a ByteArray to upload
     * to the blobstore. [sendingApplicationReport], [receivingApplicationReport], and [receivingFacilityReport] are
     * optional parameter that should be populated solely for empty HL7_BATCH files.
     */
    private fun createBodyBytes(
        report: Report,
        sendingApplicationReport: String? = null,
        receivingApplicationReport: String? = null,
        receivingFacilityReport: String? = null
    ): Pair<Report.Format, ByteArray> {
        val outputStream = ByteArrayOutputStream()
        when (report.bodyFormat) {
            Report.Format.INTERNAL -> csvSerializer?.writeInternal(report, outputStream)
            // HL7 needs some additional configuration we set on the translation in organization
            Report.Format.HL7 -> hl7Serializer?.write(report, outputStream)
            Report.Format.HL7_BATCH -> hl7Serializer?.writeBatch(
                report,
                outputStream,
                sendingApplicationReport,
                receivingApplicationReport,
                receivingFacilityReport
            )
            Report.Format.CSV, Report.Format.CSV_SINGLE -> csvSerializer?.write(report, outputStream)
            else -> throw UnsupportedOperationException("Unsupported ${report.bodyFormat}")
        }
        val contentBytes = outputStream.toByteArray()
        return Pair(report.bodyFormat, contentBytes)
    }

    companion object : Logging {
        private val defaultConnEnvVar = "AzureWebJobsStorage"

        /**
         * Metadata of a blob container.
         */
        private data class BlobContainerMetadata(val name: String, val connectionString: String)

        /**
         * THe blob containers.
         */
        private val blobContainerClients = mutableMapOf<BlobContainerMetadata, BlobContainerClient>()

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
                Event.EventAction.PROCESS -> "process/$subfolderNameChecked$reportName"
                Event.EventAction.TRANSLATE -> "translate/$subfolderNameChecked$reportName"
                else -> "other/$subfolderNameChecked$reportName"
            }
            val digest = sha256Digest(blobBytes)
            val blobUrl = uploadBlob(blobName, blobBytes)
            return BlobInfo(bodyFormat, blobUrl, digest)
        }

        /**
         * Obtain a client for interacting with the blob store.
         */
        fun getBlobClient(blobUrl: String, blobConnEnvVar: String = defaultConnEnvVar): BlobClient {
            val blobConnection = System.getenv(blobConnEnvVar)
            return BlobClientBuilder().connectionString(blobConnection).endpoint(blobUrl).buildClient()
        }

        /**
         * Upload a raw [blobBytes] as [blobName]
         * @return the url for the uploaded blob
         */
        internal fun uploadBlob(
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

        /** Checks if a blob actually exists in the blobstore */
        fun exists(blobUrl: String): Boolean {
            return getBlobClient(blobUrl).exists()
        }

        /**
         * Download the blob at the given [blobUrl]
         */
        fun downloadBlob(blobUrl: String): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.use { getBlobClient(blobUrl).downloadStream(it) }
            return stream.toByteArray()
        }

        /**
         * Copy a blob at [fromBlobUrl] to a blob in [toBlobContainer]
         */
        fun copyBlob(fromBlobUrl: String, toBlobContainer: String, toBlobConnEnvVar: String): String {
            val fromBytes = downloadBlob(fromBlobUrl)
            logger.info("Ready to copy ${fromBytes.size} bytes from $fromBlobUrl")
            val toFilename = BlobInfo.getBlobFilename(fromBlobUrl)
            logger.info("New blob filename will be $toFilename")
            val toBlobUrl = uploadBlob(toFilename, fromBytes, toBlobContainer, toBlobConnEnvVar)
            logger.info("New blob URL is $toBlobUrl")
            return toBlobUrl
        }

        /**
         * Delete a blob at [blobUrl]
         */
        fun deleteBlob(blobUrl: String) {
            getBlobClient(blobUrl).delete()
        }

        /**
         * Check the connection to the blob store
         */
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

        /**
         * Create a hex string style of a digest.
         */
        fun digestToString(digest: ByteArray): String {
            return digest.joinToString(separator = "", limit = 40) { Integer.toHexString(it.toInt()) }
        }

        /**
         * Hash a ByteArray [input] with SHA 256
         */
        fun sha256Digest(input: ByteArray): ByteArray {
            return hashBytes("SHA-256", input)
        }

        /**
         * Hash a ByteArray [input] with methond [type]
         */
        fun hashBytes(type: String, input: ByteArray): ByteArray {
            return MessageDigest
                .getInstance(type)
                .digest(input)
        }
    }
}