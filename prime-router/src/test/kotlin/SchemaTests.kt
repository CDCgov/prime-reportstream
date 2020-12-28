package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SchemaTests {
    @Test
    fun `create schema`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        assertNotNull(one)
    }

    @Test
    fun `compare schemas`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val oneAgain = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        assertEquals(one, oneAgain)
        assertNotEquals(one, two)
    }

    @Test
    fun `find element`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        assertEquals(one.findElement("a"), Element("a"))
        assertNull(one.findElement("c"))
    }
}