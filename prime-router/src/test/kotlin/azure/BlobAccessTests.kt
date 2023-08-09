package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.fail
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.net.MalformedURLException

class BlobAccessTests {
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
        unmockkAll()
    }

    @Test
    fun `delete blob`() {
        val mockedBlobClient = mockkClass(BlobClient::class)
        every { mockedBlobClient.delete() } answers { }
        mockkConstructor(BlobClientBuilder::class)
        every { anyConstructed<BlobClientBuilder>().connectionString(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().endpoint(any()) } answers
            { BlobClientBuilder() }
        every { anyConstructed<BlobClientBuilder>().buildClient() } returns mockedBlobClient

        BlobAccess.deleteBlob("http://127.0.0.1")

        verify(exactly = 1) { BlobClientBuilder().connectionString(any()) }
        verify(exactly = 1) { BlobClientBuilder().endpoint(any()) }
        verify(exactly = 1) { BlobClientBuilder().buildClient() }
        verify(exactly = 1) { mockedBlobClient.delete() }
        unmockkAll()
    }

    @Test
    fun `check connection`() {
        mockkConstructor(BlobServiceClientBuilder::class)
        every { anyConstructed<BlobServiceClientBuilder>().connectionString(any()) } answers
            { BlobServiceClientBuilder() }
        every { anyConstructed<BlobServiceClientBuilder>().buildClient() } returns mockk()

        BlobAccess.checkConnection("test")

        verify(exactly = 1) { BlobServiceClientBuilder().connectionString(System.getenv("test")) }
        verify(exactly = 1) { BlobServiceClientBuilder().buildClient() }
        unmockkAll()
    }
}