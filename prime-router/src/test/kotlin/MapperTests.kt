package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapperTests {
    @Test
    fun `test MiddleInitialMapper`() {
        val mapper = MiddleInitialMapper()
        val args = listOf("test_element")
        assertEquals("R", mapper.apply(args, mapOf("test_element" to "Rick")))
        assertEquals("R", mapper.apply(args, mapOf("test_element" to "rick")))
    }

    @Test
    fun `test LookupMapper`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        Metadata.addLookupTable("test", ByteArrayInputStream(csv.toByteArray()))
        val mapper = LookupMapper()
        val args = listOf("test", "a", "standard.patient_age", "c")
        assertEquals(listOf("standard.patient_age"), mapper.elementNames(args))
        assertEquals("y", mapper.apply(args, mapOf("standard.patient_age" to "3")))
    }
}