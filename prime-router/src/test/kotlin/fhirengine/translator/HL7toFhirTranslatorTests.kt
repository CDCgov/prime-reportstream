package gov.cdc.prime.router.fhirengine.translator

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import kotlin.test.Test

class HL7toFhirTranslatorTests {
    @Test
    fun `test get message templates`() {
        // Make sure the message templates are fetched, which means the FHIR configuration is good.
        assertThat(HL7toFhirTranslator.defaultMessageTemplates.size).isGreaterThan(0)
    }

    @Test
    fun `test get message engine`() {
        // Just make sure we can get an engine, which means the config is setup correctly.
        assertThat(HL7toFhirTranslator.getMessageEngine()).isNotNull()
    }
}