package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvConverterTests {
    @Test
    fun `test read from csv`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
            a,b
            1,2
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val result = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals(1, result.report.itemCount)
        assertEquals("2", result.report.getString(0, 1))
    }

    @Test
    fun `test read from csv with defaults`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b")),
                Element("c", default = "elementDefault")
            )
        )
        val csv = """
            a,b
            1,2
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val report = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
        assertEquals(1, report.itemCount)
        assertEquals("elementDefault", report.getString(0, "c"))
    }

    @Test
    fun `test read from csv with dynamic defaults`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b")),
                Element("c", default = "elementDefault")
            )
        )
        val csv = """
            a,b
            1,2
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val report = csvConverter.read(
            "one",
            ByteArrayInputStream(csv.toByteArray()),
            listOf(TestSource),
            defaultValues = mapOf("c" to "dynamicDefault")
        ).report
        assertEquals(1, report.itemCount)
        assertEquals("dynamicDefault", report.getString(0, "c"))
    }

    @Test
    fun `test read with different csvField name`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("A")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
            A,b
            1,2
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val report = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
        assertEquals(1, report.itemCount)
        assertEquals("1", report.getString(0, 0))
    }

    @Test
    fun `test read with different csv header order`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("A")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
            b,A
            2,1
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val report = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
        assertEquals(1, report.itemCount)
        assertEquals("1", report.getString(0, 0))
    }

    @Test
    fun `test read with missing csv_field`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("A")),
                Element("b", csvFields = Element.csvFields("b")),
                Element("c", default = "3")
            )
        )
        val csv = """
            A,b
            1,2
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val report = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
        assertEquals(1, report.itemCount)
        assertEquals("3", report.getString(0, 2))
    }

    @Test
    fun `test read using default`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("A")),
                Element("b", csvFields = Element.csvFields("b")),
                Element("c", default = "3")
            )
        )
        val csv = """
            A,b
            1,2
        """.trimIndent()

        val csvConverter = CsvConverter(Metadata(schema = one))
        val report = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
        assertEquals(1, report.itemCount)
        assertEquals("3", report.getString(0, 2))
    }

    @Test
    fun `test read using altDisplay`() {
    }

    @Test
    fun `test write as csv`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource)
        val expectedCsv = """
            a,b
            1,2
            
        """.trimIndent()
        val output = ByteArrayOutputStream()
        val csvConverter = CsvConverter(Metadata(schema = one))
        csvConverter.write(report1, output)
        assertEquals(expectedCsv, output.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `test write as csv with formatting`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.DATE, csvFields = Element.csvFields("_A", format = "MM-dd-yyyy")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val report1 = Report(one, listOf(listOf("20201001", "2")), TestSource)
        val expectedCsv = """
            _A,b
            10-01-2020,2
            
        """.trimIndent()
        val output = ByteArrayOutputStream()
        val csvConverter = CsvConverter(Metadata(schema = one))
        csvConverter.write(report1, output)
        val csv = output.toString(StandardCharsets.UTF_8)
        assertEquals(expectedCsv, csv)
    }

    @Test
    fun `test missing column`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
            a
            1,2
        """.trimIndent()
        val csvConverter = CsvConverter(Metadata(schema = one))
        val result = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertEquals(0, result.errors.size)
        assertEquals(0, result.warnings.size)
        assertEquals("", result.report.getString(0, "b"))
        assertEquals("1", result.report.getString(0, "a"))
    }

    @Test
    fun `test not matching column`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
            a,c
            1,2
        """.trimIndent()
        val csvConverter = CsvConverter(Metadata(schema = one))
        val result = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `test empty`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
        """.trimIndent()
        val csvConverter = CsvConverter(Metadata(schema = one))
        val result = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertTrue(result.warnings.isEmpty())
        assertTrue(result.errors.isEmpty())
        assertEquals(0, result.report.itemCount)
    }

    @Test
    fun `test usage`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", usage = "required", csvFields = Element.csvFields("a")),
                Element("b", usage = "optional", csvFields = Element.csvFields("b")),
                Element("c", usage = "requested", csvFields = Element.csvFields("c"))
            )
        )
        val csvConverter = CsvConverter(Metadata(schema = one))

        val csv1 = """
            a,b,c
            1,2,3
        """.trimIndent()
        val result1 = csvConverter.read("one", ByteArrayInputStream(csv1.toByteArray()), TestSource)
        assertTrue(result1.warnings.isEmpty())
        assertTrue(result1.errors.isEmpty())
        assertEquals(1, result1.report.itemCount)

        val csv2 = """
            a,b
            1,2
        """.trimIndent()
        val result2 = csvConverter.read("one", ByteArrayInputStream(csv2.toByteArray()), TestSource)
        assertEquals(1, result2.warnings.size)
        assertTrue(result2.errors.isEmpty())
        assertEquals(1, result2.report.itemCount)
        assertEquals("", result2.report.getString(0, "c"))
    }
}