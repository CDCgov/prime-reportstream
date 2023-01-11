package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.unittest.UnitTestUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CsvSerializerTests {
    @Test
    fun `test read from csv`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result.actionLogs.isEmpty()).isTrue()
        assertThat(result.report.itemCount).isEqualTo(1)
        assertThat(result.report.getString(0, 1)).isEqualTo("2")
    }

    @Test
    fun `test read from csv with defaults`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result.actionLogs.isEmpty()).isTrue()
        assertThat(result.report.itemCount).isEqualTo(1)
        assertThat(result.report.getString(0, "c")).isEqualTo("elementDefault")
    }

    @Test
    fun `test read from csv with dynamic defaults`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.getString(0, "c")).isEqualTo("dynamicDefault")
    }

    @Test
    fun `test read with different csvField name`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.getString(0, 0)).isEqualTo("1")
    }

    @Test
    fun `test read with different csv header order`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.getString(0, 0)).isEqualTo("1")
    }

    @Test
    fun `test read with missing csv_field`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result.actionLogs.isEmpty()).isTrue()
        assertThat(result.report.itemCount).isEqualTo(1)
        assertThat(result.report.getString(0, 2)).isEqualTo("3")
    }

    @Test
    fun `test read using default`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.getString(0, 2)).isEqualTo("3")
    }

    @Test
    fun `test read using altDisplay`() {
    }

    @Test
    fun `test write as csv`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
        val expectedCsv = """
            a,b
            1,2
            
        """.trimIndent()
        val output = ByteArrayOutputStream()
        val csvConverter = CsvSerializer(Metadata(schema = one))
        csvConverter.write(report1, output)
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo(expectedCsv)
    }

    @Test
    fun `test write as csv with formatting`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", type = Element.Type.DATE, csvFields = Element.csvFields("_A", format = "MM-dd-yyyy")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val report1 = Report(one, listOf(listOf("20201001", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
        val expectedCsv = """
            _A,b
            10-01-2020,2
            
        """.trimIndent()
        val output = ByteArrayOutputStream()
        val csvConverter = CsvSerializer(Metadata(schema = one))
        csvConverter.write(report1, output)
        val csv = output.toString(StandardCharsets.UTF_8)
        assertThat(csv).isEqualTo(expectedCsv)
    }

    @Test
    fun `test multiple datetime formatting`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = Element.csvFields("_A")),
                Element("b", type = Element.Type.DATETIME, csvFields = Element.csvFields("_B"))
            )
        )

        val csv = """
            _A,_B
            MMddyyyy,12012021
            M/d/yyyy,12/2/2021
            yyyy/M/d,2021/12/3
            M/d/yyyy HH:mm,12/4/2021 09:00
            yyyy/M/d HH:mm,2021/12/05 10:00
        """.trimIndent()

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertThat(result.actionLogs.hasErrors()).isFalse()
        assertThat(result.report.getString(0, 1)).isEqualTo("20211201000000+0000")
        assertThat(result.report.getString(1, 1)).isEqualTo("20211202000000+0000")
        assertThat(result.report.getString(2, 1)).isEqualTo("20211203000000+0000")
        assertThat(result.report.getString(3, 1)).isEqualTo("20211204090000+0000")
        assertThat(result.report.getString(4, 1)).isEqualTo("20211205100000+0000")
    }

    @Test
    fun `test check error for multiple datetime formatting`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element(
                    "a",
                    csvFields = Element.csvFields("_A")
                ),
                Element(
                    "b",
                    type = Element.Type.DATETIME,
                    cardinality = Element.Cardinality.ONE,
                    csvFields = Element.csvFields("_B")
                )
            )
        )

        val csv = """
            _A,_B
            MMddyyyy,13012021
            M/d/yyyy,11/50/2021
            yyyy/M/d,2021/13/3
        """.trimIndent()

        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertThat(result.actionLogs.errors.size).isEqualTo(3)
    }

    @Test
    fun `test missing column`() {
        // setup a malformed CSV
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertFailsWith<ActionError> {
            csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        }
    }

    @Test
    fun `test missing row`() {
        // setup a malformed CSV
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        val err = assertFailsWith<ActionError> {
            csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        }
        assertThat(err.details.size).isEqualTo(1)
    }

    @Test
    fun `test not matching column`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result.actionLogs.warnings.size).isEqualTo(2) // one for not present and one for ignored
    }

    @Test
    fun `test empty`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val csv = """
        """.trimIndent()
        val csvConverter = CsvSerializer(Metadata(schema = one))
        val result = csvConverter.readExternal("one", ByteArrayInputStream(csv.toByteArray()), TestSource)
        assertThat(result.actionLogs.isEmpty()).isFalse()
        assertThat(result.actionLogs.hasErrors()).isFalse()
        assertThat(result.report.itemCount).isEqualTo(0)
    }

    @Test
    fun `test cardinality`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result1.actionLogs.hasErrors()).isFalse()
        assertThat(result1.actionLogs.warnings.size).isEqualTo(1) // Missing d header)
        assertThat(result1.report.itemCount).isEqualTo(1)
        assertThat(result1.report.getString(0, "a")).isEqualTo("x")
        assertThat(result1.report.getString(0, "b")).isEqualTo("2")
        assertThat(result1.report.getString(0, "c")).isEqualTo("y")
        assertThat(result1.report.getString(0, "d")).isEqualTo("")

        // Should fail
        val csv2 = """
            a
            1
        """.trimIndent()
        val err = assertFailsWith<ActionError> {
            csvConverter.readExternal("one", ByteArrayInputStream(csv2.toByteArray()), TestSource)
        }
        assertThat(err.details.size).isEqualTo(2)

        // Happy path
        val csv3 = """
            a,b,c,d
            1,2,3,4
        """.trimIndent()
        val result3 = csvConverter.readExternal("one", ByteArrayInputStream(csv3.toByteArray()), TestSource)
        assertThat(result3.actionLogs.isEmpty()).isTrue()
        assertThat(result3.report.getString(0, "a")).isEqualTo("1")
        assertThat(result3.report.getString(0, "b")).isEqualTo("2")
        assertThat(result3.report.getString(0, "c")).isEqualTo("3")
        assertThat(result3.report.getString(0, "d")).isEqualTo("4")
    }

    @Test
    fun `test cardinality and default`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result4.actionLogs.hasErrors()).isTrue()

        val csv5 = """
            a,b,c
            1,2,3
            1,,3
        """.trimIndent()
        val result5 = csvConverter.readExternal("one", ByteArrayInputStream(csv5.toByteArray()), TestSource)
        assertThat(result5.actionLogs.isEmpty()).isTrue()
        assertThat(result5.report.itemCount).isEqualTo(2)
        assertThat(result5.report.getString(0, "b")).isEqualTo("2")
        assertThat(result5.report.getString(0, "d")).isEqualTo("D")
    }

    @Test
    fun `test cardinality and BLANK`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result4.actionLogs.hasErrors()).isFalse()
        assertThat(result4.report.getString(0, "a")).isEqualTo("")
        assertThat(result4.report.getString(1, "a")).isEqualTo("1")
        assertThat(result4.report.getString(0, "b")).isEqualTo("2")
        assertThat(result4.report.getString(1, "b")).isEqualTo("")
        assertThat(result4.report.getString(0, "c")).isEqualTo("y")
        assertThat(result4.report.getString(1, "c")).isEqualTo("3")
    }

    @Test
    fun `test using international characters`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
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
        assertThat(result.actionLogs.isEmpty()).isTrue()
        assertThat(result.report.itemCount).isEqualTo(1)
        assertThat(result.report.getString(0, "a")).isEqualTo(koreanString)
        assertThat(result.report.getString(0, "b")).isEqualTo(greekString)
    }

    @Test
    fun `test incorrect CSV content`() {
        val schema = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b"))
            )
        )
        val serializer = CsvSerializer(Metadata(schema = schema))

        val emptyCSV = ByteArrayInputStream("".toByteArray())
        var result = serializer.readExternal(schema.name, emptyCSV, TestSource)
        assertThat(result.actionLogs.warnings).isNotEmpty()
        assertThat(result.report).isNotNull()
        assertThat(result.report.itemCount).isEqualTo(0)

        val incompleteCSV = ByteArrayInputStream("a,b".toByteArray())
        result = serializer.readExternal(schema.name, incompleteCSV, TestSource)
        assertThat(result.actionLogs.warnings).isNotEmpty()
        assertThat(result.report).isNotNull()
        assertThat(result.report.itemCount).isEqualTo(0)

        val hl7Data = ByteArrayInputStream(
            """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Doe^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            """.trimIndent().toByteArray()
        )
        val err = assertFailsWith<ActionError> {
            result = serializer.readExternal(schema.name, hl7Data, TestSource)
        }
        assertThat(err.details).isNotEmpty()
    }

    @Test
    fun `test schema changes do not affect reading`() {
        val schema = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("c", csvFields = Element.csvFields("c"))
            )
        )
        val serializer = CsvSerializer(Metadata(schema))

        // Internal CSV with extra fields.
        var internalCsv = """
            a,b,c
            a1,b1,c1
            a2,b2,c2
        """.trimIndent()
        var report = serializer.readInternal(
            schema.name, ByteArrayInputStream(internalCsv.toByteArray()),
            emptyList()
        )
        assertThat(report.itemCount).isEqualTo(2)
        assertThat(report.getString(0, "a")).isEqualTo("a1")
        assertThat(report.getString(0, "c")).isEqualTo("c1")
        assertThat(report.getString(1, "c")).isEqualTo("c2")

        // Internal CSV with fewer fields.
        internalCsv = """
            c
            c1
            c2
        """.trimIndent()
        report = serializer.readInternal(
            schema.name, ByteArrayInputStream(internalCsv.toByteArray()),
            emptyList()
        )
        assertThat(report.itemCount).isEqualTo(2)
        assertThat(report.getString(0, "a")).isEqualTo("")
        assertThat(report.getString(0, "c")).isEqualTo("c1")
        assertThat(report.getString(1, "c")).isEqualTo("c2")
    }
}