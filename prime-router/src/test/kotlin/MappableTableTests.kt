package gov.cdc.prime.router

import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.test.*

class MappableTableTests {
    @Test
    fun `test concat`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = MappableTable("example1", one, listOf(listOf("1", "2"), listOf("3", "4")))
        val table2 = MappableTable("example2", one, listOf(listOf("5", "6"), listOf("7", "8")))
        val concatTable = table1.concat("concat", table2)
        assertEquals(4, concatTable.rowCount)
        assertEquals(2, table1.rowCount)
        assertEquals("8", concatTable.getString(3, "b"))
    }

    @Test
    fun `test filter`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = MappableTable("example", one, listOf(listOf("1", "2"), listOf("3", "4")))
        assertEquals(2, table1.rowCount)
        val filteredTable = table1.filter("filtered", mapOf("a" to "1"))
        assertEquals("filtered", filteredTable.name)
        assertEquals(one, filteredTable.schema)
        assertEquals(1, filteredTable.rowCount)
        assertEquals("2", filteredTable.getString(0, "b"))
    }

    @Test
    fun `test isEmpty`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val emptyTable = MappableTable("test", one)
        assertEquals(true, emptyTable.isEmpty())
        val table1 = MappableTable("test", one, listOf(listOf("1", "2")))
        assertEquals(false, table1.isEmpty())
    }

    @Test
    fun `test create with list`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = MappableTable("test", one, listOf(listOf("1", "2")))
        assertEquals("test", table1.name)
        assertEquals(one, table1.schema)
        assertEquals(1, table1.rowCount)
    }

    @Test
    fun `test applyMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("b")))

        val oneTable =
            MappableTable(name = "one", schema = one, values = listOf(listOf("a1", "b1"), listOf("a2", "b2")))
        assertEquals(2, oneTable.rowCount)
        val mappingOneToTwo = one.buildMapping(toSchema = two)

        val twoTable = oneTable.applyMapping(name = "two", mappingOneToTwo)
        assertEquals(2, twoTable.rowCount)
        assertEquals("two", twoTable.name)
        assertEquals("b2", twoTable.getString(1, "B"))
    }

    @Test
    fun `test applyMapping with default`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(Element("a", default = "~"), Element("b"))
        )
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("b")))

        val twoTable =
            MappableTable(name = "one", schema = two, values = listOf(listOf("b1"), listOf("b2")))
        assertEquals(2, twoTable.rowCount)
        val mappingTwoToOne = two.buildMapping(toSchema = one)

        val oneTable = twoTable.applyMapping(name = "one", mappingTwoToOne)
        assertEquals(2, oneTable.rowCount)
        assertEquals("~", oneTable.getString(0, colName = "a"))
        assertEquals("b2", oneTable.getString(1, colName = "b"))
    }

    @Test
    fun `test deidentify`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(Element("a", pii = true), Element("b"))
        )

        val oneTable =
            MappableTable(name = "one", schema = one, values = listOf(listOf("a1", "b1"), listOf("a2", "b2")))

        val oneDeidentified = oneTable.deidentify()
        assertEquals(2, oneDeidentified.rowCount)
        assertEquals("", oneDeidentified.getString(0, "a"))
        assertEquals("b1", oneDeidentified.getString(0, "b"))
    }
}