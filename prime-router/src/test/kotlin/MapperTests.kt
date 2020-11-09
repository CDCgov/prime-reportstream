package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTests {
    @Test
    fun `test MiddleInitialMapper`() {
        val mapper = MiddleInitialMapper()
        val args = listOf("test_element")
        assertEquals("R", mapper.apply(args, mapOf("test_element" to "Rick")))
        assertEquals("R", mapper.apply(args, mapOf("test_element" to "rick")))
    }
}