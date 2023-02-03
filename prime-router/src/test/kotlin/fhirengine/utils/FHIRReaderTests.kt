package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import java.io.File
import kotlin.test.Test

class FHIRReaderTests {
    @Test
    fun `test decoding of bad FHIR messages`() {
        val actionLogger = ActionLogger()
        val reader = FHIRReader(actionLogger)

        // Empty data
        val badData1 = ""
        reader.getBundles(badData1)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some CSV was sent
        val badData2 = """
            a,b,c
            1,2,3
        """.trimIndent()
        reader.getBundles(badData2)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some truncated HL7
        val badData3 = "MSH|^~\\&#|MEDITECH^2.16.840.1.114222.4.3.2.2.1.321.111^ISO|COCAA^1.2."
        reader.getBundles(badData3)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()
    }

    @Test
    fun `test decoding of good FHIR messages`() {
        val actionLogger = ActionLogger()
        val reader = FHIRReader(actionLogger)

        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        var messages = reader.getBundles(fhirBundle)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(1)
        actionLogger.logs.clear()

        val bulkFhirData = File("src/test/resources/fhirengine/engine/bulk_valid_data.fhir").readText()
        messages = reader.getBundles(bulkFhirData)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(2)
        actionLogger.logs.clear()
    }
}