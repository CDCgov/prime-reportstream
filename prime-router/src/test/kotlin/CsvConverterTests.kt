package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
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
        assertEquals(1, result.report?.itemCount)
        assertEquals("2", result.report?.getString(0, 1))
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
        val result = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertEquals(0, result.warnings.size)
        assertEquals(1, result.report?.itemCount)
        assertEquals("elementDefault", result.report?.getString(0, "c"))
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
        assertEquals(1, report?.itemCount)
        assertEquals("dynamicDefault", report?.getString(0, "c"))
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
        assertEquals(1, report?.itemCount)
        assertEquals("1", report?.getString(0, 0))
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
        assertEquals(1, report?.itemCount)
        assertEquals("1", report?.getString(0, 0))
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
        val result = csvConverter.read("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertEquals(0, result.warnings.size)
        assertEquals(1, result.report?.itemCount)
        assertEquals("3", result.report?.getString(0, 2))
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
        assertEquals(1, report?.itemCount)
        assertEquals("3", report?.getString(0, 2))
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
        assertEquals(1, result.warnings.size)
        assertEquals("", result.report?.getString(0, "b"))
        assertEquals("1", result.report?.getString(0, "a"))
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
        assertEquals(2, result.warnings.size) // one for not present and one for ignored
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
        assertEquals(0, result.report?.itemCount)
    }

    @Test
    fun `test cardinality`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element(
                    "a",
                    cardinality = Element.Cardinality.ONE,
                    csvFields = Element.csvFields("a"),
                    default = "x"
                ),
                Element(
                    "b",
                    cardinality = Element.Cardinality.ONE,
                    csvFields = Element.csvFields("b"),
                ),
                Element(
                    "c",
                    cardinality = Element.Cardinality.ZERO_OR_ONE,
                    csvFields = Element.csvFields("c"),
                    default = "y"
                ),
                Element(
                    "d",
                    cardinality = Element.Cardinality.ZERO_OR_ONE,
                    csvFields = Element.csvFields("d"),
                ),
            )
        )
        val csvConverter = CsvConverter(Metadata(schema = one))

        // Should just warn about column d, but convert because of cardinality and defaults
        val csv1 = """
            b
            2
        """.trimIndent()
        val result1 = csvConverter.read("one", ByteArrayInputStream(csv1.toByteArray()), TestSource)
        assertTrue(result1.errors.isEmpty())
        assertEquals(1, result1.warnings.size) // Missing d header
        assertEquals(1, result1.report?.itemCount)
        assertEquals("x", result1.report?.getString(0, "a"))
        assertEquals("2", result1.report?.getString(0, "b"))
        assertEquals("y", result1.report?.getString(0, "c"))
        assertEquals("", result1.report?.getString(0, "d"))

        // Should fail
        val csv2 = """
            a
            1
        """.trimIndent()
        val result2 = csvConverter.read("one", ByteArrayInputStream(csv2.toByteArray()), TestSource)
        assertEquals(1, result2.warnings.size) // Missing d header
        assertEquals(1, result2.errors.size) // Missing b header
        assertNull(result2.report)

        // Happy path
        val csv3 = """
            a,b,c,d
            1,2,3,4
        """.trimIndent()
        val result3 = csvConverter.read("one", ByteArrayInputStream(csv3.toByteArray()), TestSource)
        assertEquals(0, result3.warnings.size)
        assertEquals("1", result3.report?.getString(0, "a"))
        assertEquals("2", result3.report?.getString(0, "b"))
        assertEquals("3", result3.report?.getString(0, "c"))
        assertEquals("4", result3.report?.getString(0, "d"))
    }

    @Test
    fun `test cardinality and default`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", cardinality = Element.Cardinality.ONE, csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"), default = "B"),
                Element("c", cardinality = Element.Cardinality.ZERO_OR_ONE, csvFields = Element.csvFields("c")),
                Element("d", cardinality = Element.Cardinality.ONE, default = "D"),
            )
        )
        val csvConverter = CsvConverter(Metadata(schema = one))

        val csv4 = """
            a,b,c
            ,2,3
            1,,3
        """.trimIndent()
        val result4 = csvConverter.read("one", ByteArrayInputStream(csv4.toByteArray()), TestSource)

        assertEquals(0, result4.warnings.size)
        assertEquals(1, result4.errors.size)
        assertEquals(1, result4.report?.itemCount)
        assertEquals("B", result4.report?.getString(0, "b"))
        assertEquals("D", result4.report?.getString(0, "d"))
    }

    @Test
    fun `test blank and default`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element(
                    "a",
                    cardinality = Element.Cardinality.ONE,
                    type = Element.Type.TEXT_OR_BLANK,
                    csvFields = Element.csvFields("a"),
                    default = "y" // should be incompatible with TEXT_OR_BLANK
                ),
            )
        )
        assertFails { Metadata(one) }
    }

    @Test
    fun `test cardinality and BLANK`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element(
                    "a",
                    cardinality = Element.Cardinality.ONE,
                    type = Element.Type.TEXT_OR_BLANK,
                    csvFields = Element.csvFields("a")
                ),
                Element(
                    "b",
                    cardinality = Element.Cardinality.ZERO_OR_ONE,
                    csvFields = Element.csvFields("b")
                ),
                Element(
                    "c",
                    type = Element.Type.TEXT,
                    cardinality = Element.Cardinality.ONE,
                    csvFields = Element.csvFields("c"),
                    default = "y"
                ),
            )
        )
        val csvConverter = CsvConverter(Metadata(schema = one))

        val csv4 = """
            a,b,c
            ,2,
            1,,3
        """.trimIndent()
        val result4 = csvConverter.read("one", ByteArrayInputStream(csv4.toByteArray()), TestSource)
        assertEquals(0, result4.errors.size)
        assertEquals("", result4.report?.getString(0, "a"))
        assertEquals("1", result4.report?.getString(1, "a"))
        assertEquals("2", result4.report?.getString(0, "b"))
        assertEquals("", result4.report?.getString(1, "b"))
        assertEquals("y", result4.report?.getString(0, "c"))
        assertEquals("3", result4.report?.getString(1, "c"))
    }
}