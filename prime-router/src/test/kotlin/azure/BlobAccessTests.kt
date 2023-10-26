package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.fail
import com.azure.core.http.HttpRequest
import com.azure.core.http.rest.PagedIterable
import com.azure.core.http.rest.PagedResponse
import com.azure.core.http.rest.PagedResponseBase
import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobDownloadContentResponse
import com.azure.storage.blob.models.BlobDownloadResponse
import com.azure.storage.blob.models.BlobItem
import gov.cdc.prime.router.BlobStoreTransportType
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.common.Environment
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class BlobAccessTests {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    class BlobAccessIntegrationTests {
        val azuriteContainer1 =
            GenericContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.25.1"))
                .withEnv("AZURITE_ACCOUNTS", "devstoreaccount1:keydevstoreaccount1")
                .withExposedPorts(10000, 10001, 10002)

        val azuriteContainer2 =
            GenericContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.25.1"))
                .withEnv("AZURITE_ACCOUNTS", "devstoreaccount2:keydevstoreaccount2")
                .withExposedPorts(10000, 10001, 10002)

        init {
            azuriteContainer1.start()
            azuriteContainer2.start()
        }

        @AfterEach
        fun afterEach() {
            clearAllMocks()
        }

        @Test
        fun `can copy directory of blobs between storage accounts`() {
            val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )

            val destinationBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount2;AccountKey=keydevstoreaccount2;BlobEndpoint=http://${azuriteContainer2.host}:${
                    azuriteContainer2.getMappedPort(
                        10000
                    )
                }/devstoreaccount2;QueueEndpoint=http://${azuriteContainer2.host}:${
                    azuriteContainer2.getMappedPort(
                        10001
                    )
                }/devstoreaccount2;"""
            )

            val contentToUpload =
                listOf("foo/item1.txt", "foo/item2.txt", "foo/item13.txt", "bar/item2.txt", "foo/baz/item3.txt")
            contentToUpload.forEach { content ->
                BlobAccess.uploadBlob(
                    content,
                    content.toByteArray(),
                    sourceBlobContainerMetadata
                )
            }

            val currentBlobs = BlobAccess.listBlobs("foo", destinationBlobContainerMetadata)
            assertThat(currentBlobs).hasSize(0)

            BlobAccess.copyDir("foo", sourceBlobContainerMetadata, destinationBlobContainerMetadata)
            val copiedBlobs = BlobAccess.listBlobs("foo", destinationBlobContainerMetadata)
            assertThat(copiedBlobs).hasSize(4)
        }

        @Test
        fun `copyDir should overwrite any existing files`() {
            val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )

            val destinationBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount2;AccountKey=keydevstoreaccount2;BlobEndpoint=http://${azuriteContainer2.host}:${
                    azuriteContainer2.getMappedPort(
                        10000
                    )
                }/devstoreaccount2;QueueEndpoint=http://${azuriteContainer2.host}:${
                    azuriteContainer2.getMappedPort(
                        10001
                    )
                }/devstoreaccount2;"""
            )

            BlobAccess.uploadBlob(
                "foo/item1.txt",
                "new content".toByteArray(),
                sourceBlobContainerMetadata
            )

            val destinationBlobUrl = BlobAccess.uploadBlob(
                "foo/item1.txt",
                "content to be overridden".toByteArray(),
                destinationBlobContainerMetadata
            )

            val originalData = BlobAccess.downloadBlobAsByteArray(destinationBlobUrl, destinationBlobContainerMetadata)
                .toString(Charsets.UTF_8)
            assertThat(originalData).isEqualTo("content to be overridden")

            BlobAccess.copyDir("foo", sourceBlobContainerMetadata, destinationBlobContainerMetadata)

            val updatedData = BlobAccess.downloadBlobAsByteArray(destinationBlobUrl, destinationBlobContainerMetadata)
                .toString(Charsets.UTF_8)
            assertThat(updatedData).isEqualTo("new content")
        }

        @Test
        fun `can upload a blob`() {
            val testContent = "test content"
            val blobContainerMetadata1 = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )
            val url = BlobAccess.uploadBlob(
                "test.txt",
                testContent.toByteArray(),
                blobContainerMetadata1
            )
            assertThat(url).isNotNull()

            val downloadedData =
                BlobAccess.downloadBlobAsByteArray(url, blobContainerMetadata1).toString(Charsets.UTF_8)
            assertThat(downloadedData).isEqualTo(testContent)
        }

        @Test
        fun `can list blobs in a directory`() {
            val blobContainerMetadata1 = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )
            val contentToUpload = listOf("foo/item1.txt", "bar/item2.txt", "foo/baz/item3.txt")
            contentToUpload.forEach { content ->
                BlobAccess.uploadBlob(
                    content,
                    content.toByteArray(),
                    blobContainerMetadata1
                )
            }

            val blobsInFoo =
                BlobAccess.listBlobs("foo", blobContainerMetadata1)
            assertThat(blobsInFoo).hasSize(2)
            assertThat(blobsInFoo.map { it.currentBlobItem.name })
                .isEqualTo(listOf("foo/baz/item3.txt", "foo/item1.txt"))

            val blobsInBar =
                BlobAccess.listBlobs("bar", blobContainerMetadata1)
            assertThat(blobsInBar).hasSize(1)
            assertThat(blobsInBar.map { it.currentBlobItem.name }).isEqualTo(listOf("bar/item2.txt"))

            val blobsInFooBaz =
                BlobAccess.listBlobs("foo/baz", blobContainerMetadata1)
            assertThat(blobsInFooBaz).hasSize(1)
            assertThat(blobsInFooBaz.map { it.currentBlobItem.name }).isEqualTo(listOf("foo/baz/item3.txt"))

            val blobsInRoot =
                BlobAccess.listBlobs("", blobContainerMetadata1)
            assertThat(blobsInRoot).hasSize(3)
            assertThat(blobsInRoot.map { it.currentBlobItem.name })
                .isEqualTo(listOf("bar/item2.txt", "foo/baz/item3.txt", "foo/item1.txt"))
        }

        // Azurite does not support the versioning functionality that azure does, so we cannot test it without mocking
        // https://github.com/Azure/Azurite/issues/665
        // The test here lines up with the expected return from the API which is to include versions in the response in
        // order of oldest to the current version
        @Test
        fun `can list blobs with their versions`() {
            val blobContainerMetadata1 = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )

            val now = OffsetDateTime.now()
            val blobName = "foo/blob.txt"
            val blobVersion1 = mockk<BlobItem>()
            val blobVersion2 = mockk<BlobItem>()
            val blobVersion3 = mockk<BlobItem>()
            // Current version
            val blobVersion4 = mockk<BlobItem>()

            every { blobVersion1.versionId } returns now.minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME)
            every { blobVersion1.isCurrentVersion } returns false
            every { blobVersion1.name } returns blobName

            every { blobVersion2.versionId } returns now.minusMinutes(4).format(DateTimeFormatter.ISO_DATE_TIME)
            every { blobVersion2.isCurrentVersion } returns false
            every { blobVersion2.name } returns blobName

            every { blobVersion3.versionId } returns now.minusMinutes(3).format(DateTimeFormatter.ISO_DATE_TIME)
            every { blobVersion3.isCurrentVersion } returns false
            every { blobVersion3.name } returns blobName

            every { blobVersion4.versionId } returns now.minusMinutes(2).format(DateTimeFormatter.ISO_DATE_TIME)
            every { blobVersion4.isCurrentVersion } returns true
            every { blobVersion4.name } returns blobName

            val pagedResponse: () -> PagedResponse<BlobItem> = {
                PagedResponseBase(
                    mockk<HttpRequest>(),
                    200,
                    null,
                    // Return the versions starting with the oldest version
                    mutableListOf(blobVersion1, blobVersion2, blobVersion3, blobVersion4),
                    null,
                    null
                )
            }
            mockkConstructor(BlobContainerClient::class)
            every { anyConstructed<BlobContainerClient>().listBlobs(any(), any()) } returns PagedIterable(pagedResponse)

            val results = BlobAccess.listBlobs(
                "",
                blobContainerMetadata1,
                true
            )

            assertThat(results).hasSize(1)

            assertThat(results.get(0).currentBlobItem.name).isEqualTo(blobName)
            assertThat(results.get(0).previousBlobItemVersions).isNotNull()
            assertThat(results.get(0).previousBlobItemVersions!!.size).isEqualTo(3)
            assertThat(results.get(0).previousBlobItemVersions!!.map { it.versionId }).isEqualTo(
                listOf(
                    now.minusMinutes(3).format(DateTimeFormatter.ISO_DATE_TIME),
                    now.minusMinutes(4).format(DateTimeFormatter.ISO_DATE_TIME),
                    now.minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME)
                )
            )
        }

        @Test
        fun `listBlobs with versions should not include a result that has been soft-deleted`() {
            val blobContainerMetadata1 = BlobAccess.BlobContainerMetadata(
                "container1",
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )

            val now = OffsetDateTime.now()
            val blobName1 = "foo.txt"
            val blobName2 = "bar.txt"
            // Soft-deleted blob with no current version
            val fooBlobVersion1 = mockk<BlobItem>()
            val barBlobVersion1 = mockk<BlobItem>()
            // Current version
            val barBlobVersion2 = mockk<BlobItem>()

            every { fooBlobVersion1.versionId } returns now.minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME)
            every { fooBlobVersion1.isCurrentVersion } returns false
            every { fooBlobVersion1.name } returns blobName1

            every { barBlobVersion1.versionId } returns now.minusMinutes(4).format(DateTimeFormatter.ISO_DATE_TIME)
            every { barBlobVersion1.isCurrentVersion } returns false
            every { barBlobVersion1.name } returns blobName2

            every { barBlobVersion2.versionId } returns now.minusMinutes(2).format(DateTimeFormatter.ISO_DATE_TIME)
            every { barBlobVersion2.isCurrentVersion } returns true
            every { barBlobVersion2.name } returns blobName2

            val pagedResponse: () -> PagedResponse<BlobItem> = {
                PagedResponseBase(
                    mockk<HttpRequest>(),
                    200,
                    null,
                    // Return the versions starting with the oldest version
                    mutableListOf(fooBlobVersion1, barBlobVersion1, barBlobVersion2),
                    null,
                    null
                )
            }
            mockkConstructor(BlobContainerClient::class)
            every { anyConstructed<BlobContainerClient>().listBlobs(any(), any()) } returns PagedIterable(pagedResponse)

            val results = BlobAccess.listBlobs(
                "",
                blobContainerMetadata1,
                true
            )

            // fooBlob does not have a current version and should not be included in the results
            assertThat(results).hasSize(1)
            assertThat(results.get(0).currentBlobItem.name).isEqualTo(blobName2)
        }
    }

    @Test
    fun `blob info filename`() {
        var filename = BlobAccess.BlobInfo
            .getBlobFilename("http://127.0.0.1:10000/devstoreaccount1/reports/ready/pima-az-phd.elr/filename.csv")
        assertThat(filename).isEqualTo("filename.csv")

        // This is how the URLs are returned from the Blob store
        filename = BlobAccess.BlobInfo
            .getBlobFilename("http://azurite:10000/devstoreaccount1/reports/ready%2Ftx-doh.elr%2Ffilename.csv")
        assertThat(filename).isEqualTo("filename.csv")

        try {
            BlobAccess.BlobInfo.getBlobFilename("ggg://somethingweird")
            fail("Expected malformed URL Exception")
        } catch (e: MalformedURLException) {
            assertThat(e).isNotNull()
        }

        filename = BlobAccess.BlobInfo.getBlobFilename("")
        assertThat(filename).isEqualTo("")
    }

    @Test
    fun `blob info extension`() {
        var extension = BlobAccess.BlobInfo
            .getBlobFileExtension("http://127.0.0.1:10000/devstoreaccount1/reports/ready/pima-az-phd.elr/filename.csv")
        assertThat(extension).isEqualTo("csv")

        extension = BlobAccess.BlobInfo
            .getBlobFileExtension("http://azurite:10000/devstoreaccount1/reports/ready%2Ftx-doh.elr%2Ffilename.txt")
        assertThat(extension).isEqualTo("txt")

        extension = BlobAccess.BlobInfo.getBlobFileExtension("http://nofileextension.com")
        assertThat(extension).isEqualTo("")
        extension = BlobAccess.BlobInfo.getBlobFileExtension("")
        assertThat(extension).isEqualTo("")

        try {
            BlobAccess.BlobInfo.getBlobFileExtension("ggg://somethingweird")
            fail("Expected malformed URL Exception")
        } catch (e: MalformedURLException) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `upload report`() {
        val testUrl = "http://uploadreport"
        val testFormat = Report.Format.CSV
        val testBytes = "testbytes".toByteArray()

        val one = Schema(name = "one", topic = Topic.TEST)
        val metadata = Metadata(schema = one)
        val report1 = Report(
            one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource,
            bodyFormat = testFormat, metadata = metadata
        )

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every {
            BlobAccess.uploadBody(
                report1.bodyFormat, testBytes, report1.name, null,
                Event.EventAction.NONE
            )
        } returns
            BlobAccess.BlobInfo(report1.bodyFormat, testUrl, BlobAccess.sha256Digest(testBytes))

        val testBlob = BlobAccess()
        val result = testBlob.uploadReport(report1, testBytes)

        assertThat(result.format).isEqualTo(testFormat)
        assertThat(result.blobUrl).isEqualTo(testUrl)
        assertThat(result.digest).isEqualTo(BlobAccess.sha256Digest(testBytes))
    }

    @Test
    fun `upload body`() {
        val blobSlot = CapturingSlot<String>()
        val testFormat = Report.Format.CSV
        val testName = "testblob"
        val testBytes = "testbytes".toByteArray()
        val testFolder = "testfolder"
        val testEnv = "testenvvar"
        val testEvents: List<Event.EventAction?> = listOf(
            Event.EventAction.RECEIVE,
            Event.EventAction.SEND,
            Event.EventAction.BATCH,
            Event.EventAction.PROCESS,
            Event.EventAction.ROUTE,
            Event.EventAction.TRANSLATE,
            Event.EventAction.NONE,
            Event.EventAction.CONVERT,
            null
        )

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(testEnv) } returns "testconnection"
        every { BlobAccess.Companion.uploadBlob(capture(blobSlot), testBytes) } answers
            { "http://" + blobSlot.captured }

        testEvents.forEach {
            val result = when (it) {
                null -> BlobAccess.uploadBody(testFormat, testBytes, testName, "")
                // testing with and without reportName passed in to improve code coverage
                Event.EventAction.CONVERT -> BlobAccess.uploadBody(testFormat, testBytes, testName, action = it)
                else -> BlobAccess.uploadBody(testFormat, testBytes, testName, testFolder, it)
            }

            assertThat(result.format).isEqualTo(testFormat)
            // test blobUrl is as expected for the EventAction
            assertThat(
                result.blobUrl.contains(
                    when (it?.name) {
                        null -> "none"
                        "SEND" -> "ready"
                        "CONVERT" -> "other"
                        else -> it.name.lowercase()
                    }
                )
            ).isTrue()
            assertThat(result.digest).isEqualTo(BlobAccess.sha256Digest(testBytes))
        }
    }

    @Test
    fun `upload blob`() {
        val testEnv = "testenvvar"

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(testEnv) } returns "testconnection"

        val testName = "testblob"
        val testContainer = "testcontainer"
        val testBytes = "testbytes".toByteArray()
        val testUrl = "testurlname"
        val testBlobMetadata = BlobAccess.BlobContainerMetadata.build(testContainer, testEnv)

        val mockedBlobClient = mockkClass(BlobClient::class)
        every { mockedBlobClient.upload(any<ByteArrayInputStream>(), any<Long>()) } returns (Unit)
        every { mockedBlobClient.blobUrl } returns testUrl
        val mockedContainerClient = mockkClass(BlobContainerClient::class)
        every { mockedContainerClient.exists() } returns false
        every { mockedContainerClient.create() } returns (Unit)
        every { mockedContainerClient.getBlobClient(testName) } returns mockedBlobClient
        val mockedServiceClient = mockkClass(BlobServiceClient::class)
        every { mockedServiceClient.getBlobContainerClient(testContainer) } returns mockedContainerClient
        mockkConstructor(BlobServiceClientBuilder::class)
        every { anyConstructed<BlobServiceClientBuilder>().connectionString(any()) } answers
            { BlobServiceClientBuilder() }
        every { anyConstructed<BlobServiceClientBuilder>().buildClient() } returns mockedServiceClient
        mockkConstructor(BlobClientBuilder::class)
        every { anyConstructed<BlobClientBuilder>().connectionString(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().buildClient() } returns mockedBlobClient

        val result = BlobAccess.uploadBlob(testName, testBytes, testBlobMetadata)
        // upload a second blob to the same container to test container client reuse
        val result2 = BlobAccess.uploadBlob(testName, testBytes, testBlobMetadata)

        verify(exactly = 1) { mockedContainerClient.create() }
        verify(exactly = 2) { mockedBlobClient.upload(any<ByteArrayInputStream>(), testBytes.size.toLong()) }
        assertThat(result).isEqualTo("testurlname")
        assertThat(result2).isEqualTo("testurlname")
    }

    @Test
    fun `blob exists`() {
        val testUrl = "http://blobexists"

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val mockedBlobClient = mockkClass(BlobClient::class)
        every { mockedBlobClient.exists() } returns true
        mockkConstructor(BlobClientBuilder::class)
        every { anyConstructed<BlobClientBuilder>().connectionString(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().endpoint(testUrl) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().buildClient() } returns mockedBlobClient

        val result = BlobAccess.exists(testUrl)
        assertThat(result).isTrue()
    }

    @Test
    fun `download blob`() {
        val testUrl = "http://downloadblob"
        val streamSlot = CapturingSlot<ByteArrayOutputStream>()

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val mockedBlobClient = mockkClass(BlobClient::class)
        every {
            mockedBlobClient.downloadStreamWithResponse(capture(streamSlot), any(), any(), any(), any(), any(), any())
        } answers
            {
                streamSlot.captured.writeBytes("test".toByteArray())
                mockk<BlobDownloadResponse>()
            }
        every { mockedBlobClient.downloadContentWithResponse(any(), any(), any(), any()) } answers
            {
                val response = mockk<BlobDownloadContentResponse>()
                every { response.value } returns BinaryData.fromString("test")
                response
            }
        mockkConstructor(BlobClientBuilder::class)
        every { anyConstructed<BlobClientBuilder>().connectionString(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().endpoint(testUrl) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().buildClient() } returns mockedBlobClient

        val resultByteArray = BlobAccess.downloadBlobAsByteArray(testUrl)
        val resultBinaryData = BlobAccess.downloadBlobAsBinaryData(testUrl)
        val expectedResult = "test"

        verify(exactly = 2) { BlobClientBuilder().connectionString(any()) }
        verify(exactly = 2) { BlobClientBuilder().endpoint(testUrl) }
        verify(exactly = 2) { BlobClientBuilder().buildClient() }
        assertThat(resultByteArray).isEqualTo(expectedResult.toByteArray())
        assertThat(resultBinaryData.toString()).isEqualTo(expectedResult)
    }

    @Test
    fun `copy blob`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        val testUrl = "http://testurl/testfile"
        val testFile = BlobAccess.BlobInfo.getBlobFilename(testUrl)
        val testBlobMetadata = BlobAccess.BlobContainerMetadata.build("testcontainer", "testenvvar")

        every { BlobAccess.Companion.downloadBlobAsByteArray(testUrl) }.returns("testblob".toByteArray())
        every {
            BlobAccess.Companion.uploadBlob(
                testFile,
                "testblob".toByteArray(),
                testBlobMetadata
            )
        }.returns("http://testurl2")

        val result = BlobAccess.copyBlob(testUrl, testBlobMetadata)

        verify(exactly = 1) { BlobAccess.Companion.downloadBlobAsByteArray(testUrl, any(), any()) }
        verify(exactly = 1) {
            BlobAccess.Companion.uploadBlob(
                testFile,
                "testblob".toByteArray(),
                testBlobMetadata
            )
        }
        assertThat(result).isEqualTo("http://testurl2")
    }

    @Test
    fun `delete blob`() {
        val testUrl = "http://deleteblob"
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val mockedBlobClient = mockkClass(BlobClient::class)
        every { mockedBlobClient.delete() } answers { }
        mockkConstructor(BlobClientBuilder::class)
        every { anyConstructed<BlobClientBuilder>().connectionString(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().endpoint(testUrl) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().buildClient() } returns mockedBlobClient

        BlobAccess.deleteBlob(testUrl)

        verify(exactly = 1) { BlobClientBuilder().connectionString(any()) }
        verify(exactly = 1) { BlobClientBuilder().endpoint(testUrl) }
        verify(exactly = 1) { BlobClientBuilder().buildClient() }
        verify(exactly = 1) { mockedBlobClient.delete() }
    }

    @Test
    fun `check connection`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        mockkConstructor(BlobServiceClientBuilder::class)
        every { anyConstructed<BlobServiceClientBuilder>().connectionString(any()) } answers
            { BlobServiceClientBuilder() }
        every { anyConstructed<BlobServiceClientBuilder>().buildClient() } returns mockk()
        val testBlobMetadata = BlobAccess.BlobContainerMetadata.build("testcontainer", "test")

        BlobAccess.checkConnection(testBlobMetadata)

        verify(exactly = 1) { BlobServiceClientBuilder().connectionString("testconnection") }
        verify(exactly = 1) { BlobServiceClientBuilder().buildClient() }
    }

    @Test
    fun `test build container metadata`() {
        val defaultEnvVar = Environment.get().blobEnvVar
        val testEnvVar = "testenv"
        val testContainer = "testcontainer"

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(defaultEnvVar) } returns "defaultconnection"
        every { BlobAccess.Companion.getBlobConnection(testEnvVar) } returns "testconnection"

        val defaultBlobMetadata = BlobAccess.BlobContainerMetadata.build(testContainer, defaultEnvVar)
        val testBlobTransport = BlobStoreTransportType(testEnvVar, testContainer)
        val testBlobMetadata = BlobAccess.BlobContainerMetadata.build(testBlobTransport)

        assertThat(defaultBlobMetadata.containerName).isEqualTo(testContainer)
        assertThat(defaultBlobMetadata.connectionString).isEqualTo("defaultconnection")
        assertThat(testBlobMetadata.containerName).isEqualTo(testContainer)
        assertThat(testBlobMetadata.connectionString).isEqualTo("testconnection")
    }
}