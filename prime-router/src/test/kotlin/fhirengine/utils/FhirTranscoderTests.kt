package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.json.JSONException
import org.json.JSONObject
import java.io.File
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
        assertThat(jsonObj.get("id")).isEqualTo(testBundle.id)
        assertThat(jsonObj.get("type")).isEqualTo(testBundle.type.name.lowercase())
    }

    @Test
    fun `test default parser decode FHIR bundle`() {
        val encodedBundle = """
            {"resourceType":"Bundle","id":"someid","type":"message","timestamp":"2022-06-14T12:11:50.281-04:00"}
        """.trimIndent()
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle.id).isEqualTo("Bundle/someid")
        assertThat(decodedBundle.type).isEqualTo(Bundle.BundleType.MESSAGE)
    }

    @Test
    fun `test encoding and decoding combination`() {
        // Create test FHIR Bundle
        val testBundle = Bundle()
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        val resource = Patient()
        resource.id = "somePatientId"
        testBundle.addEntry().resource = resource

        val encodedBundle = FhirTranscoder.encode(testBundle)
        val decodedBundle = FhirTranscoder.decode(encodedBundle)
        // Ensure the decoded output is the same as the input pre-encode
        assertThat(decodedBundle).isNotNull()
        assertThat(decodedBundle.id).isEqualTo("Bundle/someid")
        assertThat(decodedBundle.type.name).isEqualTo(testBundle.type.name)
        assertThat(decodedBundle.timestamp).isEqualTo(testBundle.timestamp)
        assertThat(decodedBundle.entry.size).isEqualTo(testBundle.entry.size)
    }

    @Test
    fun `test decoding of bad FHIR messages`() {
        val actionLogger = ActionLogger()

        // Empty data
        val badData1 = ""
        FhirTranscoder.getBundles(badData1, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Empty bundle
        val emptyBundle = """
            {"resourceType":"Bundle"}
        """.trimIndent()
        FhirTranscoder.getBundles(emptyBundle, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some CSV was sent
        val badData2 = """
            a,b,c
            1,2,3
        """.trimIndent()
        FhirTranscoder.getBundles(badData2, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some truncated HL7
        val badData3 = "MSH|^~\\&#|MEDITECH^2.16.840.1.114222.4.3.2.2.1.321.111^ISO|COCAA^1.2."
        FhirTranscoder.getBundles(badData3, actionLogger)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()
    }

    @Test
    fun `test decoding of good FHIR messages`() {
        val actionLogger = ActionLogger()

        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        var messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(1)
        actionLogger.logs.clear()

        val bulkFhirData = File("src/test/resources/fhirengine/engine/bulk_valid_data.fhir").readText()
        messages = FhirTranscoder.getBundles(bulkFhirData, actionLogger)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(2)
        actionLogger.logs.clear()
    }
}