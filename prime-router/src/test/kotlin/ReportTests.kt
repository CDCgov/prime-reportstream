package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ReportTests {
    @Test
    fun `test merge`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val report2 = Report(one, listOf(listOf("5", "6"), listOf("7", "8")), source = TestSource)
        val mergedReport = Report.merge(listOf(report1, report2))
        assertEquals(4, mergedReport.itemCount)
        assertEquals(2, report1.itemCount)
        assertEquals("8", mergedReport.getString(3, "b"))
        assertEquals(2, mergedReport.sources.size)
        assertEquals(report1.id, (mergedReport.sources[0] as ReportSource).id)
    }

    @Test
    fun `test filter`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        assertEquals(2, report1.itemCount)
        val filteredReport = report1.filter(listOf(Pair(jurisdictionalFilter, listOf("a", "1"))))
        assertEquals(one, filteredReport.schema)
        assertEquals(1, filteredReport.itemCount)
        assertEquals("2", filteredReport.getString(0, "b"))
        assertEquals(1, filteredReport.sources.size)
    }

    @Test
    fun `test isEmpty`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val emptyReport = Report(one, emptyList(), source = TestSource)
        assertEquals(true, emptyReport.isEmpty())
        val report1 = Report(one, listOf(listOf("1", "2")), source = TestSource)
        assertEquals(false, report1.isEmpty())
    }

    @Test
    fun `test create with list`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource)
        assertEquals(one, report1.schema)
        assertEquals(1, report1.itemCount)
        assertEquals(TestSource, report1.sources[0] as TestSource)
    }

    @Test
    fun `test applyMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("b")))
        val metadata = Metadata()
        metadata.loadSchemas(one, two)

        val oneReport = Report(schema = one, values = listOf(listOf("a1", "b1"), listOf("a2", "b2")), TestSource)
        assertEquals(2, oneReport.itemCount)
        val mappingOneToTwo = Translator(metadata).buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap())

        val twoTable = oneReport.applyMapping(mappingOneToTwo)
        assertEquals(2, twoTable.itemCount)
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
        val metadata = Metadata()
        metadata.loadSchemas(one, two)

        val twoReport = Report(schema = two, values = listOf(listOf("b1"), listOf("b2")), source = TestSource)
        assertEquals(2, twoReport.itemCount)
        val mappingTwoToOne = Translator(metadata).buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap())

        val oneReport = twoReport.applyMapping(mappingTwoToOne)
        assertEquals(2, oneReport.itemCount)
        assertEquals("~", oneReport.getString(0, colName = "a"))
        assertEquals("b2", oneReport.getString(1, colName = "b"))
    }

    @Test
    fun `test deidentify`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(Element("a", pii = true), Element("b"))
        )

        val oneReport = Report(
            schema = one,
            values = listOf(listOf("a1", "b1"), listOf("a2", "b2")),
            source = TestSource
        )

        val oneDeidentified = oneReport.deidentify()
        assertEquals(2, oneDeidentified.itemCount)
        assertEquals("", oneDeidentified.getString(0, "a"))
        assertEquals("b1", oneDeidentified.getString(0, "b"))
    }
}