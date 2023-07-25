package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobErrorCode
import com.azure.storage.blob.models.BlobStorageException
import gov.cdc.prime.router.Report
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.security.MessageDigest

const val defaultBlobContainerName = "reports"

class BlobAccess : Logging {

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
             */
            fun getBlobFilename(blobUrl: String): String {
                return if (blobUrl.isNotBlank()) {
                    FilenameUtils.getName(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path)
                } else ""
            }

            /**
             * Get a file extension from a [blobUrl]
             * @return the blob's file extension
             */
            fun getBlobFileExtension(blobUrl: String): String {
                return if (blobUrl.isNotBlank()) {
                    FilenameUtils.getExtension(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path)
                } else ""
            }
        }
    }

    /**
     * Upload the [report] to the blob store using the [action] to determine a folder as needed.
     * A [subfolderName] is optional and is added as a prefix to the blob filename.
     * @return the information about the uploaded blob
     */
    fun uploadReport(
        report: Report,
        blobBytes: ByteArray,
        subfolderName: String? = null,
        action: Event.EventAction = Event.EventAction.NONE
    ): BlobInfo {
        return uploadBody(report.bodyFormat, blobBytes, report.name, subfolderName, action)
    }

    companion object : Logging {
        private const val defaultConnEnvVar = "AzureWebJobsStorage"

        /**
         * Metadata of a blob container.
         */
        private data class BlobContainerMetadata(val name: String, val connectionString: String)

        /**
         * THe blob containers.
         */
        private val blobContainerClients = mutableMapOf<BlobContainerMetadata, BlobContainerClient>()

        /**
         * Upload a raw [blobBytes] in the [bodyFormat] for a given [reportName].
         * The [action] is used to determine the folder to store the blob in.
         * A [subfolderName] name is optional.
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
                Event.EventAction.ROUTE -> "route/$subfolderNameChecked$reportName"
                Event.EventAction.TRANSLATE -> "translate/$subfolderNameChecked$reportName"
                Event.EventAction.NONE -> "none/$subfolderNameChecked$reportName"
                else -> "other/$subfolderNameChecked$reportName"
            }
            val digest = sha256Digest(blobBytes)
            val blobUrl = uploadBlob(blobName, blobBytes)
            return BlobInfo(bodyFormat, blobUrl, digest)
        }

        /**
         * Obtain a client for interacting with the blob store.
         */
        private fun getBlobClient(blobUrl: String, blobConnEnvVar: String = defaultConnEnvVar): BlobClient {
            val blobConnection = System.getenv(blobConnEnvVar)
            return BlobClientBuilder().connectionString(blobConnection).endpoint(blobUrl).buildClient()
        }

        /**
         * Upload a raw blob [bytes] as [blobName]
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
         * Creates the blob container client for the given blob [name] and connection string
         * (obtained from the environment variable [blobConnEnvVar]), or reuses an existing one.
         * @return the blob container client
         */
        private fun getBlobContainer(name: String, blobConnEnvVar: String = defaultConnEnvVar): BlobContainerClient {
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
         * Hash a ByteArray [input] with method [type]
         */
        private fun hashBytes(type: String, input: ByteArray): ByteArray {
            return MessageDigest
                .getInstance(type)
                .digest(input)
        }
    }
}