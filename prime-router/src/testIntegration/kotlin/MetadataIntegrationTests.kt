package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class MetadataIntegrationTests {
    @Test
    fun `test loading metadata catalog`() {
        try {
            val metadata = Metadata.getInstance()
            assertThat(metadata).isNotNull()
        } catch (e: Exception) {
            fail("The metadata catalog failed to initialize.  Is the database populated?")
        }
    }
}