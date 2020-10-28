package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals

class TranslatorTests {
    @Test
    fun `test MITranslator`() {
        assertEquals("R", MITranslator().apply(listOf("Rick")))
        assertEquals("R", MITranslator().apply(listOf("rick")))
    }

}