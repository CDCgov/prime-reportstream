package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvConvertTests {
    @Test
    fun `test create from csv`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val csv = """
            a,b
            1,2
        """.trimIndent()

        val table = CsvConverter.read("test", one, ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, table.rowCount)
        assertEquals("2", table.getString(0, 1))
    }

    @Test
    fun `test create with csv_field`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(Element("a", csvField = "A"), Element("b"))
        )
        val csv = """
            A,b
            1,2
        """.trimIndent()

        val table = CsvConverter.read("test", one, ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, table.rowCount)
        assertEquals("1", table.getString(0, 0))
    }

    @Test
    fun `test write as csv`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = MappableTable("test", one, listOf(listOf("1", "2")))
        val expectedCsv = """
            a,b
            1,2
            
        """.trimIndent()
        val output = ByteArrayOutputStream()
        CsvConverter.write(table1, output)
        assertEquals(expectedCsv, output.toString(StandardCharsets.UTF_8))
    }

}