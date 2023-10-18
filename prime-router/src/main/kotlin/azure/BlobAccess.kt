package gov.cdc.prime.router.azure

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobErrorCode
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.BlobListDetails
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.models.DownloadRetryOptions
import com.azure.storage.blob.models.ListBlobsOptions
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.common.Environment
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Duration

const val defaultBlobContainerName = "reports"
const val defaultBlobDownloadRetryCount = 5
const val listBlobTimeoutSeconds: Long = 30

/**
 * Accessor for Azure blob storage.
 */
class BlobAccess() : Logging {
    /**
     * Contains basic info about a Report blob: format, url in Azure, and SHA256 hash
     */
    data class BlobInfo(
        val format: Report.Format,
        val blobUrl: String,
        val digest: ByteArray,
    ) {
        companion object {
            /**
             * Get the blob filename from a [blobUrl]
             * @return the blob filename
             */
            fun getBlobFilename(blobUrl: String): String {
                return if (blobUrl.isNotBlank()) {
                    FilenameUtils.getName(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path)
                } else {
                    ""
                }
            }

            /**
             * Get a file extension from a [blobUrl]
             * @return the blob's file extension
             */
            fun getBlobFileExtension(blobUrl: String): String {
                return if (blobUrl.isNotBlank()) {
                    FilenameUtils.getExtension(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path)
                } else {
                    ""
                }
            }
        }
    }

    /**
     * Data structure for configuring reusable blob store container access.
     */
    data class BlobContainerMetadata(
        val containerName: String,
        val connectionString: String,
    ) {
        companion object {
            /**
             * Builds a [BlobContainerMetadata] object. [envVar] will be resolved to the blobstore connection string.
             */
            fun build(containerName: String, envVar: String): BlobContainerMetadata {
                return BlobContainerMetadata(containerName, getBlobConnection(envVar))
            }

            /**
             * Builds a [BlobContainerMetadata] object. [blobTransport].storageName will be resolved to the blobstore
             * connection string.
             */
            fun build(blobTransport: BlobStoreTransportType): BlobContainerMetadata {
                return BlobContainerMetadata(blobTransport.containerName, getBlobConnection(blobTransport.storageName))
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
        action: Event.EventAction = Event.EventAction.NONE,
    ): BlobInfo {
        return uploadBody(report.bodyFormat, blobBytes, report.name, subfolderName, action)
    }

    companion object : Logging {
        private const val defaultBlobDownloadRetryVar = "AzureBlobDownloadRetryCount"
        private val defaultEnvVar = Environment.get().blobEnvVar
        private val defaultBlobMetadata by lazy {
            BlobContainerMetadata.build(
                defaultBlobContainerName,
                defaultEnvVar
            )
        }
        private val blobDownloadRetryCount = System.getenv(defaultBlobDownloadRetryVar)?.toIntOrNull()
            ?: defaultBlobDownloadRetryCount

        /**
         * Map of reusable blob containers corresponding with specific blob container Metadata.
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
            action: Event.EventAction = Event.EventAction.NONE,
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
         * Obtain the blob connection string for a given environment variable name.
         */
        fun getBlobConnection(blobConnEnvVar: String = defaultEnvVar): String {
            return System.getenv(blobConnEnvVar)
        }

        /**
         * Obtain a client for interacting with the blob store.
         */
        private fun getBlobClient(
            blobUrl: String,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
        ): BlobClient {
            return BlobClientBuilder().connectionString(blobConnInfo.connectionString).endpoint(blobUrl).buildClient()
        }

        /**
         * Upload a raw blob [bytes] as [blobName]
         * @return the url for the uploaded blob
         */
        internal fun uploadBlob(
            blobName: String,
            bytes: ByteArray,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
        ): String {
            logger.info("Starting uploadBlob of $blobName")
            val blobClient = getBlobContainer(blobConnInfo).getBlobClient(blobName)
            blobClient.upload(
                ByteArrayInputStream(bytes),
                bytes.size.toLong()
            )
            logger.info("Done uploadBlob of $blobName")
            return blobClient.blobUrl
        }

        /** Checks if a blob actually exists in the blobstore */
        fun exists(blobUrl: String, blobConnInfo: BlobContainerMetadata = defaultBlobMetadata): Boolean {
            return getBlobClient(blobUrl, blobConnInfo).exists()
        }

        /**
         * Data class for returning the current version of a blob
         * and optionally it's past versions
         */
        data class BlobItemAndPreviousVersions(
            val currentBlobItem: BlobItem,
            val previousBlobItemVersions: List<BlobItem>?
        )

        /**
         * Fetches all the blobs prefixed with [directory].  Azure stores blobs in a flat
         * structure (i.e. no actual folders).
         *
         * If the following blobs were stored
         * foo/item.txt
         * foo/baz/item2.txt
         * bar/item3.txt
         *
         * using "foo" as the directory would return foo/item.txt and foo/baz/item2.txt
         *
         * @param directory - the prefix to filter the blobs by
         * @param blobConnInfo - metadata of which azure storage account to read from
         * @param includeVersion - whether or not to return the past versions of the blobs
         * @returns the list of current blob items with optionally the previous versions
         */
        fun listBlobs(
            directory: String,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
            includeVersion: Boolean = false,
        ): List<BlobItemAndPreviousVersions> {
            val options = ListBlobsOptions().setPrefix(directory).setDetails(
                BlobListDetails().setRetrieveVersions(includeVersion)
            )
            val blobContainer = getBlobContainer(blobConnInfo)
            val results = blobContainer.listBlobs(options, Duration.ofSeconds(listBlobTimeoutSeconds))
            return if (!includeVersion) {
                results.map { BlobItemAndPreviousVersions(it, null) }
            } else {
                val grouped = results.groupBy { it.name }
                grouped.values.mapNotNull { blobs ->
                    val current = blobs.find { it.isCurrentVersion }
                    if (current == null) {
                        // If there is no current version that means the blob has been soft-deleted and we
                        // will not include it in the results
                        return@mapNotNull null
                    }
                    // Blob version IDs are the timestamps of when the version is created
                    // so perform a sort previousBlobItemVersions such that first item is the most
                    // recent previous version
                    val previousVersions = blobs
                        .filter { !it.isCurrentVersion }
                        .sortedByDescending { it.versionId }
                    BlobItemAndPreviousVersions(current, previousVersions)
                }
            }
        }

        /**
         * Download the blob at the given [blobUrl] as a ByteArray
         */
        fun downloadBlobAsByteArray(
            blobUrl: String,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
            retries: Int = blobDownloadRetryCount,
        ): ByteArray {
            val stream = ByteArrayOutputStream()
            logger.debug("BlobAccess Starting download for blobUrl $blobUrl")
            val options = DownloadRetryOptions().setMaxRetryRequests(retries)
            stream.use {
                getBlobClient(blobUrl, blobConnInfo).downloadStreamWithResponse(
                    it,
                    null,
                    options,
                    null,
                    false,
                    null,
                    null
                )
            }
            logger.debug("BlobAccess Finished download for blobUrl $blobUrl")
            return stream.toByteArray()
        }

        /**
         * Download the blob at the given [blobUrl] as BinaryData
         */
        fun downloadBlobAsBinaryData(
            blobUrl: String,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
            retries: Int = blobDownloadRetryCount,
        ): BinaryData {
            logger.debug("BlobAccess Starting download for blobUrl $blobUrl")
            val options = DownloadRetryOptions().setMaxRetryRequests(retries)
            val binaryData = getBlobClient(blobUrl, blobConnInfo).downloadContentWithResponse(
                options,
                null,
                null,
                null
            ).value
            logger.debug("BlobAccess Finished download for blobUrl $blobUrl")
            return binaryData
        }

        /**
         * Copy a blob at [fromBlobUrl] to a blob in [blobConnInfo]
         */
        fun copyBlob(fromBlobUrl: String, blobConnInfo: BlobContainerMetadata): String {
            val fromBytes = downloadBlobAsByteArray(fromBlobUrl)
            logger.info("Ready to copy ${fromBytes.size} bytes from $fromBlobUrl")
            val toFilename = BlobInfo.getBlobFilename(fromBlobUrl)
            logger.info("New blob filename will be $toFilename")
            val toBlobUrl = uploadBlob(toFilename, fromBytes, blobConnInfo)
            logger.info("New blob URL is $toBlobUrl")
            return toBlobUrl
        }

        /**
         * Delete a blob at [blobUrl]
         */
        fun deleteBlob(blobUrl: String, blobConnInfo: BlobContainerMetadata = defaultBlobMetadata) {
            getBlobClient(blobUrl, blobConnInfo).delete()
        }

        /**
         * Check the connection to the blob store
         */
        fun checkConnection(blobConnInfo: BlobContainerMetadata = defaultBlobMetadata) {
            BlobServiceClientBuilder().connectionString(blobConnInfo.connectionString).buildClient()
        }

        /**
         * Creates the blob container client for the given [blobConnInfo].
         * If one exists for the container name and connection string, the existing one will be reused.
         * @return the blob container client
         */
        fun getBlobContainer(blobConnInfo: BlobContainerMetadata): BlobContainerClient {
            return if (blobContainerClients.containsKey(blobConnInfo)) {
                blobContainerClients[blobConnInfo]!!
            } else {
                val blobServiceClient = BlobServiceClientBuilder()
                    .connectionString(blobConnInfo.connectionString)
                    .buildClient()
                val containerClient = blobServiceClient
                    .getBlobContainerClient(blobConnInfo.containerName)
                try {
                    if (!containerClient.exists()) containerClient.create()
                    blobContainerClients[blobConnInfo] = containerClient
                } catch (error: BlobStorageException) {
                    // This can happen when there are concurrent calls to the API
                    if (error.errorCode.equals(BlobErrorCode.CONTAINER_ALREADY_EXISTS)) {
                        logger.warn("Container ${blobConnInfo.containerName} already exists")
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