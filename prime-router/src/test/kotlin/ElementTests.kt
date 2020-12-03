package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ElementTests {

    @Test
    fun `create element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        assertNotNull(elem1)
    }

    @Test
    fun `compare elements`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertEquals(elem1, elem2)

        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertNotEquals(elem1, elem3)
    }

    @Test
    fun `test schema element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertEquals(elem1, elem2)

        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertNotEquals(elem1, elem3)
    }

    @Test
    fun `test extendFrom`() {
        val elem1 = Element(name = "first")
        val elem2 = Element(name = "first", type = Element.Type.NUMBER, csvFields = Element.csvFields("test"))
        val elem1ExtendedFrom2 = elem1.extendFrom(elem2)
        assertEquals("first", elem1ExtendedFrom2.name)
        assertEquals("test", elem1ExtendedFrom2.csvFields?.first()?.name)
    }

    @Test
    fun `test toNormalize altValues`() {
        Metadata.loadValueSetCatalog("./src/test/unit_test_files")
        val one = Element("b",
            type = Element.Type.CODE,
            valueSet = "hl70136",
            altValues = listOf(
                // Use french as an alternative display for these code
                ValueSet.Value("Y", "Oui"),
                ValueSet.Value("N", "Non"),
                ValueSet.Value("UNK", "?")),
            csvFields = Element.csvFields("b", format = "\$alt"))
        val noResult = one.toNormalized("Non", one.csvFields?.get(0)!!)
        assertEquals("N", noResult)
        val yesResult = one.toNormalized("Oui", one.csvFields?.get(0)!!)
        assertEquals("Y", yesResult)
    }

    @Test
    fun `test toNormalize date`() {
        val one = Element("a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("aDate", format = "yyyy-dd-MM"))
        // Iso formatted should work
        val result1 = one.toNormalized("1998-03-30")
        assertEquals("19980330", result1)
        val result2 = one.toNormalized("1998-30-03", one.csvFields?.get(0)!!)
        assertEquals("19980330", result2)
    }

    @Test
    fun `test toNormalize dateTime`() {
        val one = Element("a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("aDate"))
        // Iso formatted should work
        val result1 = one.toNormalized("1998-03-30T12:00Z")
        assertEquals("199803301200+0000", result1)
        val result2 = one.toNormalized("199803300000+0000")
        assertEquals("199803300000+0000", result2)

        val two = Element("a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("aDate", format = "yyyyMMdd"))

        val result3 = two.toNormalized("19980330", two.csvFields?.get(0))
        assertEquals("199803300000+0000", result3)
    }

    @Test
    fun `test toFormatted dateTime`() {
        val one = Element("a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("aDate"))
        // Iso formatted should work
        val result1 = one.toFormatted("199803301200")
        assertEquals("199803301200", result1)
        val result2 = one.toFormatted("199803300000")
        assertEquals("199803300000", result2)
    }

    @Test
    fun `test toNormalized phone`() {
        val one = Element("a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone"))
        val result1 = one.toNormalized("5559938322")
        assertEquals("5559938322:1:", result1)
        val result2 = one.toNormalized("1(555)-968-5052")
        assertEquals("5559685052:1:", result2)
        val result3 = one.toNormalized("555-968-5052")
        assertEquals("5559685052:1:", result3)
        val result4 = one.toNormalized("+1(555)-968-5052")
        assertEquals("5559685052:1:", result4)
        val result5 = one.toNormalized("968-5052")
        assertEquals("0009685052:1:", result5)
        val result6 = one.toNormalized("+1(555)-968-5052 x5555")
        assertEquals("5559685052:1:5555", result6)
        // MX phone number
        val result7 = one.toNormalized("+52-65-8888-8888")
        assertEquals("6588888888:52:", result7)
    }

    @Test
    fun `test toFormatted phone`() {
        val one = Element("a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone"))
        val result1 = one.toFormatted("5559938322:1:")
        assertEquals("5559938322", result1)
        val result2 = one.toFormatted("5559938322:1:", Element.CsvField("test", "\$country-\$area-\$exchange-\$subscriber"))
        assertEquals("1-555-993-8322", result2)
        val result3 = one.toFormatted("5559938322:1:", Element.CsvField("test", "(\$area)\$exchange-\$subscriber"))
        assertEquals("(555)993-8322", result3)
    }
}