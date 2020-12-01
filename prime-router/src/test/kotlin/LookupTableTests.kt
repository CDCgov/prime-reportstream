package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LookupTableTests {
    @Test
    fun `test read table`() {
        val csv = """
            a,b
            1,2
            3,4
            5,6
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(3, table.table.size)
    }

    @Test
    fun `test lookup`() {
        val csv = """
            a,b
            1,2
            3,4
            5,6
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        assertTrue(table.hasColumn("a"))
        assertEquals("4", table.lookupValue("a", "3", "b"))
    }

    @Test
    fun `test bad lookup`() {
        val csv = """
            a,b
            1,2
            3,4
            5,6
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        assertFalse(table.hasColumn("c"))
        assertNull(table.lookupValue("a", "3", "c"))
    }
}