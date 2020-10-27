package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TranslatorTests {
    @Test
    fun `test MITranslator`() {
        val rick = "Rick"
        assertEquals("R", MITranslator().apply(listOf(rick)))
    }

}