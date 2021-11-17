package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.metadata.LookupTable
import java.io.ByteArrayInputStream
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.fail

class MapperTests {
    private val livdPath = "./metadata/tables/LIVD-SARS-CoV-2-2021-01-20.csv"

    @Test
    fun `test MiddleInitialMapper`() {
        val mapper = MiddleInitialMapper()
        val args = listOf("test_element")
        val element = Element("test")
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "Rick")))).isEqualTo("R")
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "rick")))).isEqualTo("R")
    }

    @Test
    fun `test LookupMapper`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
                Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val indexElement = metadata.findSchema("test")?.findElement("a") ?: fail("")
        val lookupElement = metadata.findSchema("test")?.findElement("c") ?: fail("")
        val mapper = LookupMapper()
        val args = listOf("a")
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("a"))
        assertThat(mapper.apply(lookupElement, args, listOf(ElementAndValue(indexElement, "3")))).isEqualTo("y")
    }

    @Test
    fun `test LookupMapper with two`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
                Element("b", type = Element.Type.TABLE, table = "test", tableColumn = "b"),
                Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val lookupElement = metadata.findSchema("test")?.findElement("c") ?: fail("")
        val indexElement = metadata.findSchema("test")?.findElement("a") ?: fail("")
        val index2Element = metadata.findSchema("test")?.findElement("b") ?: fail("")
        val mapper = LookupMapper()
        val args = listOf("a", "b")
        val elementAndValues = listOf(ElementAndValue(indexElement, "3"), ElementAndValue(index2Element, "4"))
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("a", "b"))
        assertThat(mapper.apply(lookupElement, args, elementAndValues)).isEqualTo("y")
    }

    @Test
    fun `test livdLookup with DeviceId`() {
        val lookupTable = LookupTable.read(livdPath)
        val codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = "Test Ordered LOINC Code"
        )
        val deviceElement = Element("device_id")
        val mapper = LIVDLookupMapper()

        // Test with a EUA
        val ev1 = ElementAndValue(
            deviceElement,
            "BinaxNOW COVID-19 Ag Card Home Test_Abbott Diagnostics Scarborough, Inc._EUA"
        )
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1))).isEqualTo("94558-4")

        // Test with a truncated device ID
        val ev1a = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card Home Test_Abb#")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1a))).isEqualTo("94558-4")

        // Test with a ID NOW device id which is has a FDA number
        val ev2 = ElementAndValue(deviceElement, "10811877011269_DII")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev2))).isEqualTo("94534-5")

        // With GUDID DI
        val ev3 = ElementAndValue(deviceElement, "10811877011269")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev3))).isEqualTo("94534-5")
    }

    @Test
    fun `test livdLookup with Equipment Model Name`() {
        val lookupTable = LookupTable.read(livdPath)
        val codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = "Test Ordered LOINC Code"
        )
        val modelElement = Element("equipment_model_name")
        val mapper = LIVDLookupMapper()

        // Test with a EUA
        val ev1 = ElementAndValue(modelElement, "BinaxNOW COVID-19 Ag Card")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1))).isEqualTo("94558-4")

        // Test with a ID NOW device id
        val ev2 = ElementAndValue(modelElement, "ID NOW")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev2))).isEqualTo("94534-5")
    }

    @Test
    fun `test livdLookup for Sofia 2`() {
        val lookupTable = LookupTable.read("./metadata/tables/LIVD-SARS-CoV-2-2021-04-28.csv")
        val codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = "Test Performed LOINC Long Name"
        )
        val modelElement = Element("equipment_model_name")
        val testPerformedElement = Element("test_performed_code")
        val mapper = LIVDLookupMapper()

        mapper.apply(
            codeElement,
            emptyList(),
            listOf(
                ElementAndValue(modelElement, "Sofia 2 Flu + SARS Antigen FIA*"),
                ElementAndValue(testPerformedElement, "95209-3")
            )
        ).let {
            assertThat(it)
                .equals("SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay")
        }
    }

    @Test
    fun `test livdLookup supplemental table by device_id`() {
        val lookupTable = LookupTable.read("./metadata/tables/LIVD-Supplemental-2021-06-07.csv")
        val codeElement = Element(
            "test_authorized_for_otc",
            tableRef = lookupTable,
            tableColumn = "is_otc"
        )
        val deviceElement = Element("device_id")
        val mapper = LIVDLookupMapper()

        // Test with an FDA device id
        val ev1 = ElementAndValue(deviceElement, "10811877011337")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1))).isEqualTo("N")

        // Test with a truncated device ID
        val ev1a = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card 2 Home#")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1a))).isEqualTo("Y")
    }

    @Test
    fun `test livdLookup supplemental table by model`() {
        val lookupTable = LookupTable.read("./metadata/tables/LIVD-Supplemental-2021-06-07.csv")
        val codeElement = Element(
            "test_authorized_for_otc",
            tableRef = lookupTable,
            tableColumn = "is_otc"
        )
        val deviceElement = Element("equipment_model_name")
        val mapper = LIVDLookupMapper()

        // Test with an FDA device id
        val ev1 = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card Home Test")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1))).isEqualTo("N")

        // Test with another
        val ev1a = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card 2 Home Test")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1a))).isEqualTo("Y")
    }

    @Test
    fun `test livdLookup model variation lookup`() {
        val lookupTable = LookupTable.read("./metadata/tables/LIVD-SARS-CoV-2-2021-09-29.csv")
        val element = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = "Test Ordered LOINC Code"
        )

        // Cue COVID-19 Test does not have an * in the table
        var testModel = "Cue COVID-19 Test"
        var expectedTestOrderedLoinc = "95409-9"
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, testModel, emptyMap()))
            .isEqualTo(expectedTestOrderedLoinc)

        // Add an * to the end of the model name
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, "$testModel*", emptyMap()))
            .isEqualTo(expectedTestOrderedLoinc)

        // Add some other character to fail the lookup
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, "$testModel^", emptyMap()))
            .isNull()

        // Accula SARS-Cov-2 Test does have an * in the table
        testModel = "Accula SARS-Cov-2 Test"
        expectedTestOrderedLoinc = "95409-9"
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, testModel, emptyMap()))
            .isEqualTo(expectedTestOrderedLoinc)

        // Add an * to the end of the model name
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, "$testModel*", emptyMap()))
            .isEqualTo(expectedTestOrderedLoinc)
    }

    @Test
    fun `test value variation`() {
        assertThat(LIVDLookupMapper.getValueVariation("dummy", "*")).isEqualTo("dummy*")
        assertThat(LIVDLookupMapper.getValueVariation("dummy*", "*")).isEqualTo("dummy")
        assertThat(LIVDLookupMapper.getValueVariation("dummy????", "???")).isEqualTo("dummy?")

        assertThat(LIVDLookupMapper.getValueVariation("dummyCaSe", "CASE")).isEqualTo("dummy")
        assertThat(LIVDLookupMapper.getValueVariation("dummyCaSe", "CASE", false)).isEqualTo("dummyCaSeCASE")

        assertFailsWith<IllegalArgumentException>(
            block = {
                LIVDLookupMapper.getValueVariation("dummy", "")
            }
        )

        assertFailsWith<IllegalArgumentException>(
            block = {
                LIVDLookupMapper.getValueVariation("", "*")
            }
        )
    }

    @Test
    fun `test ifPresent`() {
        val element = Element("a")
        val mapper = IfPresentMapper()
        val args = listOf("a", "const")
        assertThat(mapper.valueNames(element, args)).isEqualTo(listOf("a"))
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "3")))).isEqualTo("const")
        assertThat(mapper.apply(element, args, emptyList())).isNull()
    }

    @Test
    fun `test use`() {
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")
        val mapper = UseMapper()
        val args = listOf("b", "c")
        assertThat(mapper.valueNames(elementA, args)).isEqualTo(listOf("b", "c"))
        assertThat(
            mapper.apply(elementA, args, listOf(ElementAndValue(elementB, "B"), ElementAndValue(elementC, "C")))
        ).isEqualTo("B")
        assertThat(mapper.apply(elementA, args, listOf(ElementAndValue(elementC, "C")))).isEqualTo("C")
        assertThat(mapper.apply(elementA, args, emptyList())).isNull()
    }

    @Test
    fun `test ConcatenateMapper`() {
        val mapper = ConcatenateMapper()
        val args = listOf("a", "b", "c")
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        assertThat(mapper.apply(elementA, args, values)).isEqualTo("string1, string2, string3")
    }

    @Test
    fun `test concatenate mapper with custom delimiter`() {
        // arrange
        val mapper = ConcatenateMapper()
        val args = listOf("a", "b", "c")
        val elementA = Element("a", delimiter = "^")
        val elementB = Element("b")
        val elementC = Element("c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        // act
        val expected = "string1^string2^string3"
        val actual = mapper.apply(elementA, args, values)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with seconds`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "seconds",
            "6"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "202103020000-0600")
        )
        // act
        val expected = "20210302000006.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with negative seconds`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "seconds",
            "-6"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "20210302000006.0000-0600")
        )
        // act
        val expected = "20210302000000.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with minutes`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "minutes",
            "1"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "202103020000-0600")
        )
        // act
        val expected = "20210302000100.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with negative minutes`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "minutes",
            "-1"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "20210302000100.0000-0600")
        )
        // act
        val expected = "20210302000000.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test coalesce mapper`() {
        // arrange
        val mapper = CoalesceMapper()
        val args = listOf("a", "b", "c")
        val element = Element("target")
        var values = listOf(
            ElementAndValue(Element("a"), ""),
            ElementAndValue(Element("b"), ""),
            ElementAndValue(Element("c"), "c")
        )
        // act
        var expected = "c"
        var actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual).isEqualTo(expected)

        values = listOf(
            ElementAndValue(Element("a"), ""),
            ElementAndValue(Element("b"), "b"),
            ElementAndValue(Element("c"), "c")
        )
        expected = "b"
        actual = mapper.apply(element, args, values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test strip formatting mapper`() {
        val mapper = StripPhoneFormattingMapper()
        val args = listOf("patient_phone_number_raw")
        val element = Element("patient_phone_number")
        val values = listOf(
            ElementAndValue(Element("patient_phone_number_raw"), "(850) 999-9999xHOME")
        )
        val expected = "8509999999:1:"
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test strip numeric mapper`() {
        val mapper = StripNumericDataMapper()
        val args = listOf("patient_age_and_units")
        val element = Element("patient_age")
        val values = listOf(
            ElementAndValue(Element("patient_age_and_units"), "99 years")
        )
        val expected = "years"
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test strip non numeric mapper`() {
        val mapper = StripNonNumericDataMapper()
        val args = listOf("patient_age_and_units")
        val element = Element("patient_age")
        val values = listOf(
            ElementAndValue(Element("patient_age_and_units"), "99 years")
        )
        val expected = "99"
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test split mapper`() {
        val mapper = SplitMapper()
        val args = listOf("patient_name", "0")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "John Doe")
        )
        val expected = "John"
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test split mapper with error condition`() {
        val mapper = SplitMapper()
        val args = listOf("patient_name", "1")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "ThereAreNoSpacesHere")
        )
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isNull()
    }

    @Test
    fun `test split by comma mapper`() {
        val mapper = SplitByCommaMapper()
        val args = listOf("patient_name", "2")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "Antley, ARNP, Mona")
        )
        val expected = "Mona"
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test split by comma mapper error condition`() {
        val mapper = SplitByCommaMapper()
        val args = listOf("patient_name", "2")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "I have no commas")
        )
        val actual = mapper.apply(element, args, values)
        assertThat(actual).isNull()
    }

    @Test
    fun `test zip code to county mapper`() {
        val mapper = ZipCodeToCountyMapper()
        val csv = """
            zipcode,county
            32303,Leon
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val lookupElement = metadata.findSchema("test")?.findElement("a") ?: fail("Schema element missing")
        val values = listOf(
            ElementAndValue(Element("patient_zip_code"), "32303-4509")
        )
        val expected = "Leon"
        val actual = mapper.apply(lookupElement, listOf("patient_zip_code"), values)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test HashMapper`() {
        val mapper = HashMapper()
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")

        // Single value conversion
        val arg = listOf("a")
        val value = listOf(ElementAndValue(elementA, "6086edf8e412650032408e96"))
        assertThat(mapper.apply(elementA, arg, value))
            .isEqualTo("47496cafa04e9c489444b60575399f51e9abc061f4fdda40c31d814325bfc223")
        // Multiple values concatenated
        val args = listOf("a", "b", "c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        assertThat(mapper.apply(elementA, args, values))
            .isEqualTo("c8fa773cd54e7a7eb7ca08577d0bd23e6ce3a73e61df176213d9ec90f06cb45f")
        // Unhappy path cases
        assertFails { mapper.apply(elementA, listOf(), listOf()) } // must pass a field name
        assertThat(mapper.apply(elementA, arg, listOf())).isNull() // column not found in the data.
        // column has empty data
        assertThat(mapper.apply(elementA, arg, listOf(ElementAndValue(elementA, "")))).isNull()
    }

    @Test
    fun `test parseMapperField validation - allow mapper tokens to be parsed`() {
        // it should allow mapper tokens to be parsed: i.e. "$index"
        var vals = Mappers.parseMapperField("concat(patient_id, \$index)")
        assertThat(vals.second[1]).isEqualTo("\$index")

        // it should allow mapper tokens to be parsed with semi-colon literal values: i.e. "$dateFormat:some-valid-date-format"
        vals = Mappers.parseMapperField("nullDateValidator(\$dateFormat:some-valid-date-format, test_result_date)")
        assertThat(vals.second[0]).isEqualTo("\$dateFormat:some-valid-date-format")
    }

    @Test
    fun `test NullDateValidator`() {
        val mapper = NullDateValidator()
        val elementA = Element("a")
        val elementB = Element("b")

        // $dateFormat:yyyyMMdd, a (element name)
        // should return the original element's value
        val args = listOf("yyyyMMdd", "a")
        var value = listOf(ElementAndValue(elementA, "yyyyMMdd"), ElementAndValue(elementB, "20211028"))
        assertThat(mapper.apply(elementA, args, value))
            .isEqualTo("20211028")

        // $dateFormat:MM/dd/yyyy, a (element name)
        // should return the original element's value
        val args2 = listOf("MM/dd/yyyy", "a")
        value = listOf(ElementAndValue(elementA, "MM/dd/yyyy"), ElementAndValue(elementB, "10/28/2021"))
        assertThat(mapper.apply(elementA, args2, value))
            .isEqualTo("10/28/2021")

        // mismatched formatting
        // should return an empty string
        value = listOf(ElementAndValue(elementA, "yyyyMMdd"), ElementAndValue(elementB, "a week ago"))
        assertThat(mapper.apply(elementA, args, value))
            .isEqualTo("")

        // empty values
        // should return an empty string
        value = listOf()
        assertThat(mapper.apply(elementA, args, value))
            .isEqualTo("")

        // empty args
        // should return an empty string
        value = listOf(ElementAndValue(elementA, "yyyyMMdd"), ElementAndValue(elementB, "a week ago"))
        assertThat(mapper.apply(elementA, listOf(), value))
            .isEqualTo("")

        // invalid formatting
        // should return an empty string
        value = listOf(ElementAndValue(elementA, "iNvAlId"), ElementAndValue(elementB, "10/28/2021"))
        assertThat(mapper.apply(elementA, args, value))
            .isEqualTo("")

        value = listOf(ElementAndValue(elementA, ""), ElementAndValue(elementB, "10/28/2021"))
        assertThat(mapper.apply(elementA, args, value))
            .isEqualTo("")
    }
}