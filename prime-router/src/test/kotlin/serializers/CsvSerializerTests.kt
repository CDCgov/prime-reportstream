package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CsvSerializerTests {
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val report = csvConverter.readExternal(
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val report = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val report = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
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

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val report = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource).report
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
        val csvConverter = CsvSerializer(Metadata(schema = one))
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
        val csvConverter = CsvSerializer(Metadata(schema = one))
        csvConverter.write(report1, output)
        val csv = output.toString(StandardCharsets.UTF_8)
        assertEquals(expectedCsv, csv)
    }

    @Test
    fun `test missing column`() {
        // setup a malformed CSV
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
        val csvConverter = CsvSerializer(Metadata(schema = one))
        // Run it
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        // Expect the converter to catch the error. Our serializer will error on malformed CSVs.
        assertEquals(1, result.errors.size)
        assertEquals(0, result.warnings.size)
        assertNull(result.report)
    }

    @Test
    fun `test missing row`() {
        // setup a malformed CSV
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
        val csvConverter = CsvSerializer(Metadata(schema = one))
        // Run it
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        // Expect the converter to catch the error. Our serializer will error on malformed CSVs.
        assertEquals(1, result.errors.size)
        assertEquals(0, result.warnings.size)
        assertNull(result.report)
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
        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
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
        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertTrue(result.warnings.isNotEmpty())
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
        val csvConverter = CsvSerializer(Metadata(schema = one))

        // Should just warn about column d, but convert because of cardinality and defaults
        val csv1 = """
            b
            2
        """.trimIndent()
        val result1 = csvConverter.readExternal("one", ByteArrayInputStream(csv1.toByteArray()), TestSource)
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
        val result2 = csvConverter.readExternal("one", ByteArrayInputStream(csv2.toByteArray()), TestSource)
        assertEquals(1, result2.warnings.size) // Missing d header
        assertEquals(1, result2.errors.size) // Missing b header
        assertNull(result2.report)

        // Happy path
        val csv3 = """
            a,b,c,d
            1,2,3,4
        """.trimIndent()
        val result3 = csvConverter.readExternal("one", ByteArrayInputStream(csv3.toByteArray()), TestSource)
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
        val csvConverter = CsvSerializer(Metadata(schema = one))

        val csv4 = """
            a,b,c
            ,2,3
            1,,3
        """.trimIndent()
        val result4 = csvConverter.readExternal("one", ByteArrayInputStream(csv4.toByteArray()), TestSource)

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
        val csvConverter = CsvSerializer(Metadata(schema = one))

        val csv4 = """
            a,b,c
            ,2,
            1,,3
        """.trimIndent()
        val result4 = csvConverter.readExternal("one", ByteArrayInputStream(csv4.toByteArray()), TestSource)
        assertEquals(0, result4.errors.size)
        assertEquals("", result4.report?.getString(0, "a"))
        assertEquals("1", result4.report?.getString(1, "a"))
        assertEquals("2", result4.report?.getString(0, "b"))
        assertEquals("", result4.report?.getString(1, "b"))
        assertEquals("y", result4.report?.getString(0, "c"))
        assertEquals("3", result4.report?.getString(1, "c"))
    }

    @Test
    fun `test using international characters`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )

        // Sample UTF-8 taken from https://www.kermitproject.org/utf8.html as a byte array, so we are not
        // restricted by the encoding of this code file
        val koreanString = String(
            byteArrayOf(-21, -126, -104, -21, -118, -108, 32, -20, -100, -96, -21, -90, -84, -21, -91, -68),
            Charsets.UTF_8
        )
        val greekString = String(
            byteArrayOf(-50, -100, -49, -128, -50, -65, -49, -127, -49, -114),
            Charsets.UTF_8
        )

        // Java strings are stored as UTF-16
        val csv = """
            a,b
            $koreanString,$greekString
        """.trimIndent()

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals(1, result.report?.itemCount)
        assertEquals(koreanString, result.report?.getString(0, "a"))
        assertEquals(greekString, result.report?.getString(0, "b"))
    }

    @Test
    fun `test incorrect CSV content`() {
        val schema = Schema(
            name = "one",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val serializer = CsvSerializer(Metadata(schema = schema))

        val emptyCSV = ByteArrayInputStream("".toByteArray())
        var result = serializer.readExternal(schema.name, emptyCSV, TestSource)
        assertThat(result.warnings).isNotEmpty()
        assertThat(result.report).isNotNull()
        assertThat(result.report!!.itemCount).isEqualTo(0)

        val incompleteCSV = ByteArrayInputStream("a,b".toByteArray())
        result = serializer.readExternal(schema.name, incompleteCSV, TestSource)
        assertThat(result.warnings).isNotEmpty()
        assertThat(result.report).isNotNull()
        assertThat(result.report!!.itemCount).isEqualTo(0)

        val hl7Data = ByteArrayInputStream(
            """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Doe^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            """.trimIndent().toByteArray()
        )
        result = serializer.readExternal(schema.name, hl7Data, TestSource)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.report).isNull()
    }
}