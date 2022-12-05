package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.fail
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
}