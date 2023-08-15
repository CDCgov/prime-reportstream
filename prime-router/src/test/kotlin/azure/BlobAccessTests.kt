package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.fail
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException

class BlobAccessTests {
    @AfterEach
    fun tearDown() {
        unmockkAll()
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
        val testName = "testblob"
        val testContainer = "testcontainer"
        val testBytes = "testbytes".toByteArray()
        val testEnv = "testenvvar"
        val testUrl = "testurlname"

        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(testEnv) } returns "testconnection"
        val mockedBlobClient = mockkClass(BlobClient::class)
        every { mockedBlobClient.upload(any<ByteArrayInputStream>(), any<Long>()) } returns(Unit)
        every { mockedBlobClient.blobUrl } returns testUrl
        val mockedContainerClient = mockkClass(BlobContainerClient::class)
        every { mockedContainerClient.exists() } returns false
        every { mockedContainerClient.create() } returns(Unit)
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

        val result = BlobAccess.uploadBlob(testName, testBytes, testContainer, testEnv)
        // upload a second blob to the same container to test container client reuse
        val result2 = BlobAccess.uploadBlob(testName, testBytes, testContainer, testEnv)

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
        every { mockedBlobClient.downloadStream(capture(streamSlot)) } answers
            { streamSlot.captured.writeBytes("test".toByteArray()) }
        mockkConstructor(BlobClientBuilder::class)
        every { anyConstructed<BlobClientBuilder>().connectionString(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().endpoint(testUrl) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().buildClient() } returns mockedBlobClient

        val result = BlobAccess.downloadBlob(testUrl)

        verify(exactly = 1) { BlobClientBuilder().connectionString(any()) }
        verify(exactly = 1) { BlobClientBuilder().endpoint(testUrl) }
        verify(exactly = 1) { BlobClientBuilder().buildClient() }
        assertThat(result).isEqualTo("test".toByteArray())
    }

    @Test
    fun `copy blob`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)

        val testUrl = "http://testurl/testfile"
        val testFile = BlobAccess.BlobInfo.getBlobFilename(testUrl)

        every { BlobAccess.Companion.downloadBlob(testUrl) }.returns("testblob".toByteArray())
        every {
            BlobAccess.Companion.uploadBlob(
                testFile,
                "testblob".toByteArray(),
                "testcontainer",
                "testenvvar"
            )
        }.returns("http://testurl2")

        val result = BlobAccess.copyBlob(testUrl, "testcontainer", "testenvvar")

        verify(exactly = 1) { BlobAccess.Companion.downloadBlob(testUrl) }
        verify(exactly = 1) {
            BlobAccess.Companion.uploadBlob(
                testFile,
                "testblob".toByteArray(),
                "testcontainer",
                "testenvvar"
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

        BlobAccess.checkConnection("test")

        verify(exactly = 1) { BlobServiceClientBuilder().connectionString("testconnection") }
        verify(exactly = 1) { BlobServiceClientBuilder().buildClient() }
    }
}