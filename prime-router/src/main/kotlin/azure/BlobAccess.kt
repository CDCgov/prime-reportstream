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
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.BlobUtils.sha256Digest
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.common.Environment
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.Duration

const val defaultBlobContainerName = "reports"
const val defaultBlobDownloadRetryCount = 5
const val listBlobTimeoutSeconds: Long = 30

/**
 * Accessor for Azure blob storage.
 */
class BlobAccess : Logging {
    /**
     * Contains basic info about a Report blob: format, url in Azure, and SHA256 hash
     */
    data class BlobInfo(val format: MimeFormat, val blobUrl: String, val digest: ByteArray) {
        companion object {
            /**
             * Get the blob filename from a [blobUrl]
             * @return the blob filename
             */
            fun getBlobFilename(blobUrl: String): String = if (blobUrl.isNotBlank()) {
                    FilenameUtils.getName(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path)
                } else {
                    ""
                }

            /**
             * Get a file extension from a [blobUrl]
             * @return the blob's file extension
             */
            fun getBlobFileExtension(blobUrl: String): String = if (blobUrl.isNotBlank()) {
                    FilenameUtils.getExtension(URL(URLDecoder.decode(blobUrl, Charset.defaultCharset())).path)
                } else {
                    ""
                }
        }
    }

    /**
     * Data structure for configuring reusable blob store container access.
     */
    data class BlobContainerMetadata(val containerName: String, val connectionString: String) {
        companion object {

            /**
             * Builds a [BlobContainerMetadata] object. [envVar] will be resolved to the blobstore connection string.
             */
            fun build(
                containerName: String,
                envVar: String,
            ): BlobContainerMetadata = BlobContainerMetadata(containerName, getBlobConnection(envVar))

            /**
             * Builds a [BlobContainerMetadata] object. [blobTransport].storageName will be resolved to the blobstore
             * connection string.
             */
            fun build(
                blobTransport: BlobStoreTransportType,
            ): BlobContainerMetadata = BlobContainerMetadata(
                blobTransport.containerName, getBlobConnection(blobTransport.storageName)
            )
        }

        /**
         * Helper function to extract the storage URL from the connection endpoint and configured container name
         *
         * @return the URL for the storage and container that this object represents
         */
        fun getBlobEndpoint(): String {
            val parameters = connectionString
                .split(";")
                .map { it.split("=") }
                .associate { it.first() to it.last() }

            val blobEndpoint = parameters["BlobEndpoint"]
            val endpointSuffix = parameters["EndpointSuffix"]
            val accountName = parameters["AccountName"]
            if (blobEndpoint != null) {
                return "$blobEndpoint/$containerName"
            } else if (accountName != null && endpointSuffix != null) {
                return "https://$accountName.blob.$endpointSuffix/$containerName"
            }
            throw RuntimeException("Connection string is misconfigured and does not contain a blob endpoint URL")
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
    ): BlobInfo = uploadBody(report.bodyFormat, blobBytes, report.id.toString(), subfolderName, action)

    companion object : Logging {
        private const val defaultBlobDownloadRetryVar = "AzureBlobDownloadRetryCount"
        private val defaultEnvVar = Environment.get().storageEnvVar
        val defaultBlobMetadata by lazy {
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
         * Gets the root directory name for storing a blob associated with an EventAction
         */
        internal fun directoryForAction(action: Event.EventAction?): String =

         when (action) {
            Event.EventAction.RECEIVE -> "receive"
            Event.EventAction.BATCH -> "batch"
            Event.EventAction.PROCESS -> "process"
            Event.EventAction.DESTINATION_FILTER -> "destination-filter"
            Event.EventAction.RECEIVER_FILTER -> "receiver-filter"
            Event.EventAction.ROUTE -> "route"
            Event.EventAction.TRANSLATE -> "translate"
            Event.EventAction.NONE -> "none"
            Event.EventAction.SEND -> "ready"
            else -> "other"
        }

        /**
         * Upload a raw [blobBytes] in the [bodyFormat] for a given [reportName].
         * The [action] is used to determine the folder to store the blob in.
         * A [subfolderName] name is optional.
         * @return the information about the uploaded blob
         */
        fun uploadBody(
            bodyFormat: MimeFormat,
            blobBytes: ByteArray,
            reportName: String,
            subfolderName: String? = null,
            action: Event.EventAction = Event.EventAction.OTHER,
        ): BlobInfo {
            val subfolderNameChecked = if (subfolderName.isNullOrBlank()) "" else "$subfolderName/"
            val blobName = "${directoryForAction(action)}/$subfolderNameChecked$reportName.${bodyFormat.ext}"

            val digest = sha256Digest(blobBytes)
            val blobUrl = uploadBlob(blobName, blobBytes)
            return BlobInfo(bodyFormat, blobUrl, digest)
        }

        /**
         * Obtain the blob connection string for a given environment variable name.
         */
        fun getBlobConnection(blobConnEnvVar: String = defaultEnvVar): String = System.getenv(blobConnEnvVar)

        /**
         * Obtain a client for interacting with the blob store.
         */
        private fun getBlobClient(
            blobUrl: String,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
        ): BlobClient = BlobClientBuilder()
            .connectionString(blobConnInfo.connectionString)
            .endpoint(blobUrl).buildClient()

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
        fun exists(
            blobUrl: String,
            blobConnInfo: BlobContainerMetadata = defaultBlobMetadata,
        ): Boolean = getBlobClient(blobUrl, blobConnInfo).exists()

        /**
         * Copies all blobs prefixed with the [directory] value from the soure to destination
         *
         * @param directory - the prefix (or directory) containing the blobs to be copied
         * @param source - the source account to copy from
         * @param destination - the destination to copy to
         */
        fun copyDir(
            directory: String,
            source: BlobContainerMetadata,
            destination: BlobContainerMetadata,
            blobFilter: (blob: BlobItemAndPreviousVersions) -> Boolean = { _ -> true },
        ) {
            val sourceContainer = getBlobContainer(source)
            val destinationContainer = getBlobContainer(destination)
            val blobsToCopy = listBlobs(directory, source)
            blobsToCopy.filter(blobFilter).forEach { blob ->
                val sourceBlobClient = sourceContainer.getBlobClient(blob.currentBlobItem.name)
                val destinationBlobClient = destinationContainer.getBlobClient(blob.currentBlobItem.name)
                // Azurite does not support copying between instances of azurite
                // https://github.com/Azure/Azurite/issues/767
                // which would be the correct way to implement this functionality rather than
                // downloading the content and uploading.  That solution would be:
                //  val expiryTime = OffsetDateTime.now().plusDays(1)
                //  val sasPermission = BlobSasPermission()
                //    .setReadPermission(true)
                //  val sasSignatureValues = BlobServiceSasSignatureValues(expiryTime, sasPermission)
                //    .setStartTime(OffsetDateTime.now())
                //  val sasToken = fromBlobClient.generateSas(sasSignatureValues)
                //  toBlobClient.copyFromUrl("${fromBlobClient.blobUrl}?$sasToken")
                val data = sourceBlobClient.downloadContent()
                destinationBlobClient.upload(data, true)
            }
        }

        /**
         * Data class for returning the current version of a blob
         * and optionally its past versions
         */
        data class BlobItemAndPreviousVersions(
            val currentBlobItem: BlobItem,
            val previousBlobItemVersions: List<BlobItem>?,
        ) {
            val blobName: String by lazy {
                currentBlobItem.name
            }
        }

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
         * @param includeVersion - whether to return the past versions of the blobs
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
                    // If there is no current version that means the blob has been soft-deleted and we
                    // will not include it in the results
                    val current = blobs.find { it.isCurrentVersion } ?: return@mapNotNull null
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
         * Download the file associated with a RawSubmission message
         */
        fun downloadBlob(
            blobUrl: String,
            digest: String,
        ): String {
            val blobContent = downloadBlobAsByteArray(blobUrl)
            val localDigest = BlobUtils.digestToString(sha256Digest(blobContent))
            check(digest == localDigest) {
                "Downloaded file does not match expected file\n$digest | $localDigest"
            }
            return String(blobContent)
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
         * Helper function that converts a [BlobItem] into a blob URL and downloads it
         *
         * @param blobItem the item to download
         * @param blobConnInfo the azure blob store to download from
         * @param retries number of download retries
         * @return the byte array with contents of the blob
         */
        fun downloadBlobAsByteArray(
            blobItem: BlobItem,
            blobConnInfo: BlobContainerMetadata,
            retries: Int = blobDownloadRetryCount,
        ): ByteArray {
            val blobClient = getBlobContainer(blobConnInfo).getBlobClient(blobItem.name)
            return downloadBlobAsByteArray(blobClient.blobUrl, blobConnInfo, retries)
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
         * Accepts a [BlobItemAndPreviousVersions] and grabs the most recent previous version and updates
         * the blob to it.
         *
         * If the list of previous versions is empty (meaning there is nothing to restore), an error is logged
         * and nothing occurs
         *
         * @param blobItemAndPreviousVersions - the blob to restore the most recent previous version
         * @param blobConnInfo the azure blob connection info
         */
        fun restorePreviousVersion(
            blobItemAndPreviousVersions: BlobItemAndPreviousVersions,
            blobConnInfo: BlobContainerMetadata,
        ) {
            val container = getBlobContainer(blobConnInfo)
            val currentBlobClient = container.getBlobClient(blobItemAndPreviousVersions.currentBlobItem.name)
            val previousVersionId = blobItemAndPreviousVersions.previousBlobItemVersions?.firstOrNull()?.versionId
            if (previousVersionId != null) {
                val previousVersionBlobClient = container.getBlobVersionClient(
                    blobItemAndPreviousVersions.currentBlobItem.name,
                    previousVersionId
                )
                currentBlobClient.copyFromUrl(previousVersionBlobClient.blobUrl)
            } else {
                logger.error(
                    "${blobItemAndPreviousVersions.currentBlobItem.name} did not have any previous versions to restore"
                )
            }
        }

        /**
         * Download all blobs located at [sourceBlobDirectoryPath] with container info [sourceBlobContainerInfo] to the
         * [destinationDirectoryPath].
         */
        fun downloadBlobsInDirectoryToLocal(
            sourceBlobDirectoryPath: String,
            sourceBlobContainerInfo: BlobContainerMetadata,
            destinationDirectoryPath: String,
        ) {
            val sourceContainer = getBlobContainer(sourceBlobContainerInfo)
            val blobsToCopy = listBlobs(sourceBlobDirectoryPath, sourceBlobContainerInfo)
            if (blobsToCopy.isEmpty()) {
                logger.warn("No files to copy in directory '$sourceBlobDirectoryPath'")
            }
            blobsToCopy.forEach { currentBlob ->
                val sourceBlobClient = sourceContainer.getBlobClient(currentBlob.currentBlobItem.name)
                val data = sourceBlobClient.downloadContent()
                val file = File("$destinationDirectoryPath/${currentBlob.currentBlobItem.name}")
                FileUtils.writeByteArrayToFile(file, data.toBytes())
            }
        }

        /**
         * Delete a blob at [blobUrl]
         */
        fun deleteBlob(blobUrl: String, blobConnInfo: BlobContainerMetadata = defaultBlobMetadata) {
            getBlobClient(blobUrl, blobConnInfo).delete()
        }

        /**
         * Accepts a [BlobItem] and attempts to delete from the passed blob container info
         *
         *
         * @param blobItem the blob item to delete
         * @param blobContainerMetadata the blob container connection info
         */
        fun deleteBlob(blobItem: BlobItem, blobContainerMetadata: BlobContainerMetadata) {
            val blobContainer = getBlobContainer(blobContainerMetadata)
            val blobClient = blobContainer.getBlobClient(blobItem.name)
            blobClient.delete()
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
        internal fun getBlobContainer(
            blobConnInfo: BlobContainerMetadata,
        ): BlobContainerClient = blobContainerClients.getOrElse(blobConnInfo) {
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
}