package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.BeforeEach
import java.util.Date
import kotlin.test.Test

class FhirTranscoderTests {
    private val testBundle = Bundle()
    @BeforeEach
    fun setup() {
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        testBundle.addEntry()
    }
    @Test
    fun `test get message engine`() {
        // Just make sure we can get an engine, which means the config is setup correctly.
        assertThat(FhirTranscoder.getMessageEngine()).isNotNull()
    }

    @Test
    fun `test default parser encode FHIR bundle`() {
        val encodedBundle = FhirTranscoder.encode(testBundle)
        assertThat(encodedBundle).isNotEmpty()
        assertThat(encodedBundle).contains(testBundle.id)
    }

    @Test
    fun `test default parser decode FHIR bundle`() {
        val encodedBundle = FhirTranscoder.encode(testBundle)
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle == testBundle)
    }
}