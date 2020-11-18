package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class CsvConverterTests {
    @Test
    fun `test create from csv`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val csv = """
            a,b
            1,2
        """.trimIndent()

        val report = CsvConverter.read(one, ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertEquals(1, report.rowCount)
        assertEquals("2", report.getString(0, 1))
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

        val report = CsvConverter.read(one, ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertEquals(1, report.rowCount)
        assertEquals("1", report.getString(0, 0))
    }

    @Test
    fun `test write as csv`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource)
        val expectedCsv = """
            a,b
            1,2
            
        """.trimIndent()
        val output = ByteArrayOutputStream()
        CsvConverter.write(report1, output)
        assertEquals(expectedCsv, output.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `test missing column`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val csv = """
            a
            1,2
        """.trimIndent()
        assertFails { CsvConverter.read(one, ByteArrayInputStream(csv.toByteArray()), TestSource) }
    }

    @Test
    fun `test not matching column`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val csv = """
            a,c
            1,2
        """.trimIndent()
        assertFails { CsvConverter.read(one, ByteArrayInputStream(csv.toByteArray()), TestSource) }
    }

    @Test
    fun `test empty`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val csv = """
        """.trimIndent()
        assertFails { CsvConverter.read(one, ByteArrayInputStream(csv.toByteArray()), TestSource) }
    }


}