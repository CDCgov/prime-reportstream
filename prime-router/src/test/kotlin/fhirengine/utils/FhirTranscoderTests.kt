package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isNotNull
import kotlin.test.Test

class FhirTranscoderTests {
    @Test
    fun `test get message engine`() {
        // Just make sure we can get an engine, which means the config is setup correctly.
        assertThat(FhirTranscoder.getMessageEngine()).isNotNull()
    }
}