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

    @Test
    fun `test inherit from`() {
        val parent = Element(
            "parent",
            type = Element.Type.TEXT,
            csvFields = Element.csvFields("sampleField"),
            documentation = "I am the parent element"
        )

        var child = Element("child")
        child = child.inheritFrom(parent)

        assertEquals(parent.documentation, child.documentation, "Documentation value didn't carry over from parent")
        assertEquals(parent.type, child.type, "Element type didn't carry over from parent")
    }

    @Test
    fun `test normalize and formatted round-trips`() {
        val postal = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        assertEquals(
            "94040",
            postal.toFormatted(postal.toNormalized("94040"))
        )
        assertEquals(
            "94040-6000",
            postal.toFormatted(postal.toNormalized("94040-6000"))
        )
        assertEquals(
            "94040",
            postal.toFormatted(postal.toNormalized("94040-3600", Element.zipFiveToken), Element.zipFiveToken)
        )
        assertEquals(
            "94040-3600",
            postal.toFormatted(
                postal.toNormalized("94040-3600", Element.zipFivePlusFourToken),
                Element.zipFivePlusFourToken
            )
        )

        val telephone = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        assertEquals(
            "6509999999",
            telephone.toFormatted(telephone.toNormalized("6509999999"))
        )
        assertEquals(
            "6509999999",
            telephone.toFormatted(telephone.toNormalized("+16509999999"))
        )

        val date = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date")
        )
        assertEquals(
            "20201220",
            date.toFormatted(date.toNormalized("20201220"))
        )
        assertEquals(
            "20201220",
            date.toFormatted(date.toNormalized("2020-12-20"))
        )

        val datetime = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )
        assertEquals(
            "202012200000+0000",
            datetime.toFormatted(datetime.toNormalized("202012200000+0000"))
        )
        assertEquals(
            "202012200000+0000",
            datetime.toFormatted(datetime.toNormalized("2020-12-20T00:00Z"))
        )

        val hd = Element(
            "a",
            type = Element.Type.HD,
            csvFields = Element.csvFields("hd")
        )
        assertEquals(
            "HDName",
            hd.toFormatted(hd.toNormalized("HDName"))
        )
        assertEquals(
            "HDName^0.0.0.0.0.1^ISO",
            postal.toFormatted(hd.toNormalized("HDName^0.0.0.0.0.1^ISO"))
        )

        val ei = Element(
            "a",
            type = Element.Type.EI,
            csvFields = Element.csvFields("ei")
        )
        assertEquals(
            "EIName",
            ei.toFormatted(ei.toNormalized("EIName"))
        )
        assertEquals(
            "EIName^EINamespace^0.0.0.0.0.1^ISO",
            postal.toFormatted(ei.toNormalized("EIName^EINamespace^0.0.0.0.0.1^ISO"))
        )
    }

    @Test
    fun `test normalize with multiple csvFields`() {
        val sendingApp = Element(
            "a",
            type = Element.Type.HD,
            csvFields = listOf(
                Element.CsvField("sending_app", format = "\$name"),
                Element.CsvField("sending_oid", format = "\$universalId")
            )
        )
        val normalized = sendingApp.toNormalized(
            listOf(
                Element.SubValue("sending_app", "happy", "\$name"),
                Element.SubValue("sending_oid", "0.0.0.011", "\$universalId")
            )
        )
        assertEquals("happy^0.0.0.011^ISO", normalized)

        val sendingAppName = sendingApp.toFormatted(normalized, Element.hdNameToken)
        assertEquals("happy", sendingAppName)
        val sendingOid = sendingApp.toFormatted(normalized, Element.hdUniversalIdToken)
        assertEquals("0.0.0.011", sendingOid)
    }

    @Test
    fun `test code format field`() {
        val values = ValueSet(
            "test",
            system = ValueSet.SetSystem.LOCAL,
            values = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unk")
            )
        )
        val one = Element(
            "a",
            valueSet = "test",
            valueSetRef = values,
            type = Element.Type.CODE,
            altValues = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unknown")
            )
        )
        // because something is a
        assertEquals(one.toFormatted("Y", "\$code"), "Y")
        assertEquals(one.toNormalized("Y", "\$code"), "Y")
        assertEquals(one.checkForError("Y", "\$code"), null)
    }
}