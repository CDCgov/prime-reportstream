package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
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
    fun `test toNormalize altValues`() {
        val one = Element(
            "b",
            type = Element.Type.CODE,
            altValues = listOf(
                // Use french as an alternative display for these code
                ValueSet.Value("Y", "Oui"),
                ValueSet.Value("N", "Non"),
                ValueSet.Value("UNK", "?")
            ),
            csvFields = Element.csvFields("b", format = "\$alt")
        )
        val noResult = one.toNormalized("Non", "\$alt")
        assertEquals("N", noResult)
        val yesResult = one.toNormalized("Oui", "\$alt")
        assertEquals("Y", yesResult)
    }

    @Test
    fun `test toNormalize date`() {
        val one = Element(
            "a",
            type = Element.Type.DATE,
        )
        // Iso formatted should work
        val result1 = one.toNormalized("1998-03-30")
        assertEquals("19980330", result1)
        val result2 = one.toNormalized("1998-30-03", "yyyy-dd-MM")
        assertEquals("19980330", result2)
    }

    @Test
    fun `test toNormalize dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        val result1 = one.toNormalized("1998-03-30T12:00Z")
        assertEquals("199803301200+0000", result1)
        val result2 = one.toNormalized("199803300000+0000")
        assertEquals("199803300000+0000", result2)

        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )

        val result3 = two.toNormalized("19980330", "yyyyMMdd")
        assertEquals("199803300000-0600", result3)

        val three = Element(
            "a",
            type = Element.Type.DATETIME,
        )

        val result4 = three.toNormalized("2020-12-09", "yyyy-MM-dd")
        assertEquals("202012090000-0600", result4)
    }

    @Test
    fun `test toFormatted dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        val result1 = one.toFormatted("199803301200")
        assertEquals("199803301200", result1)
        val result2 = one.toFormatted("199803300000")
        assertEquals("199803300000", result2)

        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Other formats should work too, including sans the time.
        val result3 = two.toFormatted("202012090000+0000", "yyyy-MM-dd")
        assertEquals("2020-12-09", result3)
    }

    @Test
    fun `test toNormalized zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        val result1 = one.toNormalized("99999")
        assertEquals("99999", result1)
        val result2 = one.toNormalized("99999-9999")
        assertEquals("99999-9999", result2)
        // format should not affect normalization
        val result4 = one.toNormalized("999999999", "\$zipFive")
        assertEquals("999999999", result4)
        val result5 = one.toNormalized("KY1-6666") // Cayman zipcode
        assertEquals("KY1-6666", result5)
        one.toNormalized("KX33-77777") // Letters and numbers, but a made up zipcode
        assertFails {
            one.toNormalized("%%%%XXXX") // Unreasonable
        }
    }

    @Test
    fun `test toNormalized phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
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
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        val result1 = one.toFormatted("5559938322:1:")
        assertEquals("5559938322", result1)
        val result2 = one.toFormatted("5559938322:1:", "\$country-\$area-\$exchange-\$subscriber")
        assertEquals("1-555-993-8322", result2)
        val result3 = one.toFormatted("5559938322:1:", "(\$area)\$exchange-\$subscriber")
        assertEquals("(555)993-8322", result3)
    }

    @Test
    fun `test toFormatted zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        val result1 = one.toFormatted("99999")
        assertEquals("99999", result1)
        val result1a = one.toFormatted("99999-9999")
        assertEquals("99999-9999", result1a)
        val result2 = one.toFormatted("99999-9999", "\$zipFivePlusFour")
        assertEquals("99999-9999", result2)
        val result3 = one.toFormatted("99999-9999", "\$zipFive")
        assertEquals("99999", result3)
        val result4 = one.toFormatted("999999999", "\$zipFive")
        assertEquals("99999", result4)
        val result5 = one.toFormatted("999999999", "\$zipFivePlusFour")
        assertEquals("99999-9999", result5)
        val result6 = one.toFormatted("99999", "\$zipFivePlusFour")
        assertEquals("99999", result6)
        val result7 = one.toFormatted("KY1-5555", "\$zipFivePlusFour")
        assertEquals("KY1-5555", result7)
        val result8 = one.toFormatted("XZ5555", "\$zipFivePlusFour")
        assertEquals("XZ5555", result8)
    }
}