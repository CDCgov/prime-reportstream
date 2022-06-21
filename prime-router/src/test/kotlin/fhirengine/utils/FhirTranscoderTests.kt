package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import org.hl7.fhir.r4.model.Bundle
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import kotlin.test.Test

class FhirTranscoderTests {
    @Test
    fun `test get message engine`() {
        // Just make sure we can get an engine, which means the config is setup correctly.
        assertThat(FhirTranscoder.getMessageEngine()).isNotNull()
    }

    @Test
    fun `test default parser encode FHIR bundle`() {
        fun isValidJSONObject(jsonVal: String): Boolean {
            try {
                JSONObject(jsonVal)
            } catch (ex: JSONException) {
                return false
            }
            return true
        }
        // Create test FHIR Bundle
        val testBundle = Bundle()
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        testBundle.addEntry()

        val encodedBundle = FhirTranscoder.encode(testBundle)

        assertThat(encodedBundle).isNotEmpty()
        assertThat(isValidJSONObject(encodedBundle)).isTrue()

        // Ensure contains id and type
        val jsonObj = JSONObject(encodedBundle)
        assertThat(jsonObj.get("id")).equals(testBundle.id)
        assertThat(jsonObj.get("type")).equals(testBundle.type.name.lowercase())
    }

    @Test
    fun `test default parser decode FHIR bundle`() {
        val encodedBundle = """
            {"resourceType":"Bundle","id":"someid","type":"message","timestamp":"2022-06-14T12:11:50.281-04:00"}
        """.trimIndent()
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle.id).equals("someid")
        assertThat(decodedBundle.type).equals(Bundle.BundleType.MESSAGE)
    }

    @Test
    fun `test encoding and decoding combination`() {
        // Create test FHIR Bundle
        val testBundle = Bundle()
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        testBundle.addEntry()

        val encodedBundle = FhirTranscoder.encode(testBundle)
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        // Ensure the decoded output is the same as the input pre-encode
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle).equals(testBundle)
    }
}