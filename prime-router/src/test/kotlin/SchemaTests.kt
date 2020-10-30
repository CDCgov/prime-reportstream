package gov.cdc.prime.router

import kotlin.test.*

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

    @Test
    fun `test buildMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))

        val oneToTwo = one.buildMapping(two)
        assertEquals(one, oneToTwo.fromSchema)
        assertEquals(two, oneToTwo.toSchema)
        assertEquals(1, oneToTwo.useDirectly.size)
        assertEquals("a", oneToTwo.useDirectly["a"])
        assertEquals(true, oneToTwo.useDefault.contains("b"))
        assertEquals(0, oneToTwo.missing.size)

        val twoToOne = two.buildMapping(toSchema = one)
        assertEquals(1, twoToOne.useDirectly.size)
        assertEquals("a", twoToOne.useDirectly["a"])
        assertEquals(0, twoToOne.useDefault.size)
        assertEquals(0, twoToOne.missing.size)
    }

    @Test
    fun `test buildMapping with missing`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val three = Schema(
            name = "three",
            topic = "test",
            elements = listOf(Element("a"), Element("c", required = true))
        )
        val oneToThree = one.buildMapping(toSchema = three)
        assertEquals(1, oneToThree.useDirectly.size)
        assertEquals("a", oneToThree.useDirectly["a"])
        assertEquals(0, oneToThree.useDefault.size)
        assertEquals(1, oneToThree.missing.size)
    }
}