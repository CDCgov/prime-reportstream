package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNull
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.common.NPIUtilities
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.fail

class MapperTests {
    @Test
    fun `test MiddleInitialMapper`() {
        val mapper = MiddleInitialMapper()
        val args = listOf("test_element")
        val element = Element("test")
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "Rick"))).value).isEqualTo("R")
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "rick"))).value).isEqualTo("R")
    }

    @Test
    fun `test LookupMapper`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = Topic.COVID_19,
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
                Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val indexElement = metadata.findSchema("test")?.findElement("a") ?: fail("")
        val lookupElement = metadata.findSchema("test")?.findElement("c") ?: fail("")
        val mapper = LookupMapper()
        val args = listOf("a", "Column:a")
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("a", "Column:a"))
        assertThat(mapper.apply(lookupElement, args, listOf(ElementAndValue(indexElement, "3"))).value)
            .isEqualTo("y")
    }

    @Test
    fun `test LookupMapper with two`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = Topic.COVID_19,
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
        val args = listOf("a", "Column:a", "b", "Column:b")
        val elementAndValues = listOf(ElementAndValue(indexElement, "3"), ElementAndValue(index2Element, "4"))
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("a", "Column:a", "b", "Column:b"))
        assertThat(mapper.apply(lookupElement, args, elementAndValues).value).isEqualTo("y")
    }

    @Test
    fun `test decodeArg`() {
        val mapper = IfThenElseMapper()
        val elementsAndValues = listOf(
            ElementAndValue(Element("otc_flag"), "OTC"),
            ElementAndValue(Element("comparison_value"), "Prescription")
        )
        // expected element value
        assertThat(mapper.decodeArg(elementsAndValues, "otc_flag")).isEqualTo("OTC")
        // well-formed non-existent element
        assertThat(mapper.decodeArg(elementsAndValues, "other_element")).isEqualTo("other_element")
        // empty list of elementAndValues
        assertThat(mapper.decodeArg(listOf(), "otc_flag")).isEqualTo("otc_flag")
        // string literal
        assertThat(mapper.decodeArg(elementsAndValues, "George")).isEqualTo("George")
        // numeric string literal
        assertThat(mapper.decodeArg(elementsAndValues, "127")).isEqualTo("127")
    }

    @Test
    fun `test basic element value functionality IfThenElseMapper`() {
        val mapper = IfThenElseMapper()
        val element = Element("test")
        var args = mutableListOf<String>()
        // test for zero to ten args passed - each should fail (skip testing 5)
        while (args.count() < 11) {
            if (args.count() != 5) assertThat { mapper.valueNames(element, args) }.isFailure()
            args.add("arg")
        }
        // test normal call  mapper: ifThenElse(<=, otc_flag comparisonValue, patient_state, ordering_provider_state)
        args = mutableListOf("<=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        val valNames = mapper.valueNames(element, args)
        assertThat(valNames.count()).isEqualTo(5)
        assertThat(valNames[0]).isEqualTo("<=")
        assertThat(valNames[1]).isEqualTo("otc_flag")
        assertThat(valNames[2]).isEqualTo("comparisonValue")
        assertThat(valNames[3]).isEqualTo("patient_state")
        assertThat(valNames[4]).isEqualTo("ordering_provider_state")

        // In all these cases, think of "AL" as the "that's right" and "TN" as the "else case"
        val eAVotc = ElementAndValue(Element(args[1]), "OTC")
        val eAVpresc = ElementAndValue(Element(args[2]), "Prescription")
        val eAValabama = ElementAndValue(Element(args[3]), "AL")
        val eAVtn = ElementAndValue(Element(args[4]), "TN")

        // test 'else value' for ==    ("OTC" != "Prescription")
        args = mutableListOf("==", "otc_flag", "comparisonValue", eAValabama.element.name, "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")

        // bad legal args
        args = mutableListOf(">>", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat { mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)) }.isFailure()

        // test inequality operator
        args = mutableListOf("!=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")
        // It's TRUE, "OTC" != "Prescription" so we return "AL" that's right!

        // "OTC" comes alphabetically before "Prescription", so it's "<="  (technically, "<")
        args = mutableListOf("<=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")
        args = mutableListOf("<", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // finally, test a 'failure' of the assumption that "Prescription" comes before "OTC" in the dictionary
        args = mutableListOf(">=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")
        args = mutableListOf(">", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")

        // make "comparisonValue" equal to "otc_flag"  ("OTC" == "OTC")
        args = mutableListOf("==", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        val eAVotc2 = ElementAndValue(Element(args[2]), "OTC") // set "comparisonValue" element's value to "OTC"
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVotc2, eAValabama, eAVtn)).value).isEqualTo("AL")
        // now that the otc_flag is equal to the comparisonValue, "<=" and ">=" will also return "AL"
        args = mutableListOf("<=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVotc2, eAValabama, eAVtn)).value).isEqualTo("AL")
        args = mutableListOf(">=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVotc2, eAValabama, eAVtn)).value).isEqualTo("AL")

        // test inequality else (now that they are equal, testing of != should return the else value (TN)
        args = mutableListOf("!=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVotc2, eAValabama, eAVtn)).value).isEqualTo("TN")
    }

    @Test
    fun `test string and numeric literals IfThenElseMapper`() {
        val mapper = IfThenElseMapper()
        val element = Element("test")
        var args = listOf("<=", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        val valNames = mapper.valueNames(element, args)
        assertThat(valNames.count()).isEqualTo(5)
        assertThat(valNames[0]).isEqualTo("<=")
        assertThat(valNames[1]).isEqualTo("otc_flag")
        assertThat(valNames[2]).isEqualTo("comparisonValue")
        assertThat(valNames[3]).isEqualTo("patient_state")
        assertThat(valNames[4]).isEqualTo("ordering_provider_state")

        var eAVotc = ElementAndValue(Element(args[1]), "OTC")
        var eAVpresc = ElementAndValue(Element(args[2]), "Prescription")
        val eAValabama = ElementAndValue(Element(args[3]), "AL")
        val eAVtn = ElementAndValue(Element(args[4]), "TN")

        // test normal equality else (Note: "OTC" != "Prescription")
        args = listOf("==", "otc_flag", "comparisonValue", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")

        // now try a string literal ("OTC" is not an element name)
        args = listOf("==", "otc_flag", "OTC", "patient_state", "ordering_provider_state")
        eAVpresc = ElementAndValue(Element("testing"), "nulll") // should work, even after killing EAVpresc
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // now try a numeric literal
        args = listOf("==", "otc_flag", "37", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "22") // element otc_flag has .value "22"
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")

        // same values, different operator; different result
        args = listOf("<=", "otc_flag", "37", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")
        args = listOf("<", "otc_flag", "37", "patient_state", "ordering_provider_state") // <
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // float (this test actually caught a typo for me!)
        args = listOf("==", "otc_flag", "3.1415926536", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "3.1415926536") // pi is pi
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // !=
        args = listOf("!=", "otc_flag", "3.1415", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // ! !=
        args = listOf("!=", "otc_flag", "3.1415926536", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")

        // int not a string
        args = listOf(">=", "otc_flag", "22", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "1700") // element otc_flag has .value "22"
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")
        args = listOf(">", "otc_flag", "22", "patient_state", "ordering_provider_state")
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // string not an int
        args = listOf("<=", "otc_flag", "1700xx", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "22xx") // element otc_flag has .value "22xx"
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("TN")

        // equality of two (numeric looking) strings, returning a passed string literal
        args = listOf("==", "otc_flag", "1700xx", "SweetHomeAlabama", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "1700xx") // element otc_flag has .value "1700xx"
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo(
            "SweetHomeAlabama"
        )

        // int AND float
        args = listOf("==", "otc_flag", "2", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "2.0") // element otc_flag has .value "22"
        assertThat(mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo("AL")

        // int AND string
        args = listOf("==", "otc_flag", "2", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "Butterfly") // this should error, to save hours of debugging
        assertThat { mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)) }.isFailure()

        // string AND int
        args = listOf("==", "otc_flag", "Zebra", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "2") // this should error, to save hours of debugging
        assertThat { mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)) }.isFailure()

        // bad legal args
        args = listOf(">>", "otc_flag", "2", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "2.0") // element otc_flag has .value "2.0"
        assertThat { mapper.apply(element, args, listOf(eAVotc, eAVpresc, eAValabama, eAVtn)) }.isFailure()

        // operator as a value of an element
        args = listOf("op_elm", "otc_flag", "2", "patient_state", "ordering_provider_state")
        eAVotc = ElementAndValue(Element(args[1]), "2.0") // element otc_flag has .value "2.0"
        val eOpElm = ElementAndValue(Element(args[0]), ">=") // element op_elm has .value ">="
        assertThat(mapper.apply(element, args, listOf(eOpElm, eAVotc, eAVpresc, eAValabama, eAVtn)).value).isEqualTo(
            "AL"
        )
    }

    @Test
    fun `test ifPresent`() {
        val element = Element("a")
        val mapper = IfPresentMapper()
        val args = listOf("a", "const")
        assertThat(mapper.valueNames(element, args)).isEqualTo(listOf("a"))
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "3"))).value).isEqualTo("const")
        assertThat(mapper.apply(element, args, emptyList()).value).isNull()
    }

    @Test
    fun `test get cleaned up model name`() {
        val modelName = "some model"
        assertThat(LivdLookupUtilities.getCleanedModelName(modelName)).isEqualTo(modelName)
        assertThat(LivdLookupUtilities.getCleanedModelName("$modelName*")).isEqualTo(modelName)
        assertThat(LivdLookupUtilities.getCleanedModelName("*$modelName**")).isEqualTo("*$modelName*")
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
                .value
        ).isEqualTo("B")
        assertThat(mapper.apply(elementA, args, listOf(ElementAndValue(elementC, "C"))).value).isEqualTo("C")
        assertThat(mapper.apply(elementA, args, emptyList()).value).isNull()
    }

    @Test
    fun `test useSenderSetting`() {
        val sender = CovidSender(
            "senderName",
            "orgName",
            format = Sender.Format.CSV,
            CustomerStatus.ACTIVE,
            "mySchemaName",
            keys = null,
            processingType = Sender.ProcessingType.async
        )
        val elementA = Element("a")
        val mapper = UseSenderSettingMapper()
        var args = listOf("name")
        assertThat(mapper.valueNames(elementA, args)).isEqualTo(emptyList())
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("senderName")
        args = listOf("organizationName")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("orgName")
        args = listOf("fullName")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("orgName.senderName")
        args = listOf("topic")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("COVID_19")
        args = listOf("schemaName")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("mySchemaName")

        // useSenderSetting does a best effort to get string values for other types.
        args = listOf("processingType")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("async")
        args = listOf("format")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("CSV")
        args = listOf("customerStatus")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("ACTIVE")
        args = listOf("processingModeCode")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("P")
        // Error cases:
        // Must have an arg
        args = emptyList()
        assertFails(block = { mapper.apply(elementA, args, emptyList(), sender) })
        // Must have a single arg
        args = listOf("a", "b")
        assertFails(block = { mapper.apply(elementA, args, emptyList(), sender) })
        // Arg must be a valid name of a Sender settings field
        args = listOf("not a valid value")
        val response = mapper.apply(elementA, args, emptyList(), sender)
        assertThat(response.value).isNull()
        assertThat(response.errors.size).isEqualTo(1)
        assertThat(response.errors[0].message).contains("not a valid value")
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
        assertThat(mapper.apply(elementA, args, values).value).isEqualTo("string1, string2, string3")
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
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test lookup with NpiLookupMapper`() {
        /* Schema and table configuration for tests */
        val table = LookupTable.read("./src/test/resources/metadata/tables/npi-lookup.csv")
        val tableName = "npi-lookup"

        /* Hopefully this shuts SonarCloud up about duplicate code */
        val elementInfo: List<Pair<String, String>> = listOf(
            Pair("ordering_provider_id", "ordering_provider_id"),
            Pair("testing_lab_clia", "testing_lab_clia"),
            Pair("sender_id", "sender_id"),
            Pair("ordering_provider_first_name", "first_name"),
            Pair("ordering_provider_last_name", "last_name")
        )
        val elements: List<Element> = elementInfo.map { info ->
            Element(
                name = info.first,
                type = Element.Type.TABLE,
                table = tableName,
                tableColumn = info.second
            )
        }
        val schema = Schema(
            tableName, topic = Topic.TEST,
            elements = elements
        )
        val metadata = Metadata(schema = schema, table = table, tableName = tableName)
        val npiElement = metadata.findSchema(tableName)?.findElement("ordering_provider_id")
            ?: fail("Did not find Ordering_provider_id in test schema")
        val cliaElement = metadata.findSchema(tableName)?.findElement("testing_lab_clia")
            ?: fail("Did not find Testing_lab_clia in test schema")
        val senderIdElement = metadata.findSchema(tableName)?.findElement("sender_id")
            ?: fail("Did not find sender_id in test schema")
        val firstNameElement = metadata.findSchema(tableName)?.findElement("ordering_provider_first_name")
            ?: fail("Did not find Ordering_provider_first_name in test schema")
        val lastNameElement = metadata.findSchema(tableName)?.findElement("ordering_provider_last_name")
            ?: fail("Did not find Ordering_provider_last_name in test schema")
        val mapper = NpiLookupMapper()

        val args = listOf(npiElement.name, cliaElement.name, senderIdElement.name)

        /* Testing value lookup when NPI is present */
        val evNpiPresent = listOf(
            ElementAndValue(npiElement, "1023040318"),
            ElementAndValue(cliaElement, "01D2079572"),
            ElementAndValue(senderIdElement, "cuc-al")
        )

        assertThat(mapper.valueNames(firstNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(firstNameElement, args, evNpiPresent).value).isEqualTo("Paul")

        assertThat(mapper.valueNames(lastNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(lastNameElement, args, evNpiPresent).value).isEqualTo("Fineburg")

        /* Testing value lookup when NPI is NOT present */
        val evNpiNotPresent = listOf(
            ElementAndValue(npiElement, ""),
            ElementAndValue(cliaElement, "01D2079572"),
            ElementAndValue(senderIdElement, "cuc-al")
        )

        assertThat(mapper.valueNames(firstNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(firstNameElement, args, evNpiNotPresent).value).isEqualTo("Paul")

        assertThat(mapper.valueNames(lastNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(lastNameElement, args, evNpiNotPresent).value).isEqualTo("Fineburg")
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
        val expected = "20210302000006-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
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
        val expected = "20210302000000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
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
        val expected = "20210302000100-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
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
        val expected = "20210302000000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isEqualTo(expected)

        values = listOf(
            ElementAndValue(Element("a"), ""),
            ElementAndValue(Element("b"), "b"),
            ElementAndValue(Element("c"), "c")
        )
        expected = "b"
        actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isNull()
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
        assertThat(actual.value).isEqualTo(expected)
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
        assertThat(actual.value).isNull()
    }

    @Test
    fun `test zip code to county mapper`() {
        val mapper = ZipCodeToCountyMapper()
        val csv = """
            zipcode,county
            32303,Leon
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = Topic.COVID_19,
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
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test zip code to state mapper`() {
        val mapper = ZipCodeToStateMapper()
        val csv = """
            zipcode,state_abbr
            32303,FL
            42223,TN
            42223,KY
            12345-1234,LA
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = Topic.COVID_19,
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "state_abbr"),
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val lookupElement = metadata.findSchema("test")?.findElement("a") ?: fail("Schema element missing")

        // Test when value has a hyphen and extension
        val valuesWithHyphen = listOf(
            ElementAndValue(Element("patient_zip_code"), "32303-4509")
        )

        val expectedWithHyphen = "FL"
        val actualWithHyphen = mapper.apply(lookupElement, listOf("patient_zip_code"), valuesWithHyphen)
        assertThat(actualWithHyphen.value).isEqualTo(expectedWithHyphen)

        // Test when multiple values are returned from lookup
        val valuesWithTwoStates = listOf(
            ElementAndValue(Element("patient_zip_code"), "42223")
        )

        val expectedWithTwoStates = null
        val actualWithTwoStates = mapper.apply(lookupElement, listOf("patient_zip_code"), valuesWithTwoStates)
        assertThat(actualWithTwoStates.value).isEqualTo(expectedWithTwoStates)

        // Test when zip code does not exist in table
        val valuesWithFakeZip = listOf(
            ElementAndValue(Element("patient_zip_code"), "fakeZip")
        )
        val expectedWithFakeZip = null
        val actualWithFakeZip = mapper.apply(lookupElement, listOf("patient_zip_code"), valuesWithFakeZip)
        assertThat(actualWithFakeZip.value).isEqualTo(expectedWithFakeZip)

        // Test when zip code is blank
        val valuesWithBlankZip = listOf(
            ElementAndValue(Element("patient_zip_code"), "")
        )
        val expectedWithBlankZip = null
        val actualWithBlankZip = mapper.apply(lookupElement, listOf("patient_zip_code"), valuesWithBlankZip)
        assertThat(actualWithBlankZip.value).isEqualTo(expectedWithBlankZip)
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
        assertThat(mapper.apply(elementA, arg, value).value)
            .isEqualTo("47496cafa04e9c489444b60575399f51e9abc061f4fdda40c31d814325bfc223")
        // Multiple values concatenated
        val args = listOf("a", "b", "c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("c8fa773cd54e7a7eb7ca08577d0bd23e6ce3a73e61df176213d9ec90f06cb45f")
        // Unhappy path cases
        assertFails { mapper.apply(elementA, listOf(), listOf()) } // must pass a field name
        assertThat(mapper.apply(elementA, arg, listOf()).value).isNull() // column not found in the data.
        // column has empty data
        assertThat(mapper.apply(elementA, arg, listOf(ElementAndValue(elementA, ""))).value).isNull()
    }

    @Test
    fun `test parseMapperField validation - allow mapper tokens to be parsed`() {
        // it should allow mapper tokens to be parsed: i.e. "$index"
        val vals = Mappers.parseMapperField("concat(patient_id, \$index)")
        assertThat(vals.second[1]).isEqualTo("\$index")
    }

    @Test
    fun `test IfNotPresent mapper`() {
        val mapper = IfNotPresentMapper()
        val elementA = Element("a")
        val elementB = Element("lookup_field")
        val elementC1 = Element("condition_field_1")
        val elementC2 = Element("condition_field_2")

        // $mode:"literal" tests

        // conditional fields are blank: should return the "$string" value
        var args = listOf(
            "\$mode:literal", "\$string:*** No Address Given ***", "condition_field_1", "condition_field_2"
        )
        var values = listOf(ElementAndValue(elementC1, ""), ElementAndValue(elementC2, ""))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("*** No Address Given ***")

        // any conditional field is non-blank: should return null
        args = listOf("\$mode:literal", "\$string:*** No Address Given ***", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementC1, ""), ElementAndValue(elementC2, "nonBlank"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)

        // conditional fields not present: should return the "$string" value
        args = listOf("\$mode:literal", "\$string:*** No Address Given ***", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementA, ""))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("*** No Address Given ***")

        // $mode:"lookup" tests

        // conditional fields are blank: should return the value of lookup_field (Element B)
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(
            ElementAndValue(elementB, "value of B"),
            ElementAndValue(elementC1, ""),
            ElementAndValue(elementC2, "")
        )
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("value of B")

        // any conditional field is non-blank: should return null
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(
            ElementAndValue(elementB, "value of B"),
            ElementAndValue(elementC1, ""),
            ElementAndValue(elementC2, "nonBlank")
        )
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)

        // conditional fields not present: should return the value of lookup_field (Element B)
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementA, ""), ElementAndValue(elementB, "value of B"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("value of B")

        // single non-blank condition: should return null
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1")
        values = listOf(ElementAndValue(elementB, "value of B"), ElementAndValue(elementC1, "nonBlank"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)

        // invalid $mode should return null
        args = listOf("\$mode:iNvAlId", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementA, ""), ElementAndValue(elementB, "value of B"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)
    }

    @Test
    fun `test ifNPI with valid NPI`() {
        val mapper = IfNPIMapper()
        val elementA = Element("a")
        val args = listOf("a", "NPI", "U")
        val values = listOf(ElementAndValue(elementA, NPIUtilities.VALID_NPI))
        assertThat(mapper.apply(elementA, args, values).value).isEqualTo("NPI")
    }

    @Test
    fun `test ifNPI with invalid NPI`() {
        val mapper = IfNPIMapper()
        val elementA = Element("a")
        val args = listOf("a", "NPI", "U")
        val values = listOf(ElementAndValue(elementA, "xyz"))
        assertThat(mapper.apply(elementA, args, values).value).isEqualTo("U")
    }

    @Test
    fun `test ifNPI with invalid NPI and 2 args`() {
        val mapper = IfNPIMapper()
        val elementA = Element("a")
        val args = listOf("a", "NPI")
        val values = listOf(ElementAndValue(elementA, "xyz"))
        assertThat(mapper.apply(elementA, args, values).value).isNull()
    }

    @Test
    fun `test LookupSenderAutomationValuesetsMapper`() {
        val genderTable = LookupTable
            .read("./src/test/resources/metadata/tables/gender.csv")
        val testResultTable = LookupTable
            .read("./src/test/resources/metadata/tables/test_result.csv")
        val genderSchema = Schema(
            "test-gender",
            topic = Topic.COVID_19,
            elements = listOf(
                Element(
                    "patient_gender",
                    type = Element.Type.TABLE,
                    table = "gender",
                    tableColumn = "code",
                    mapperOverridesValue = true
                )
            )
        )
        val testResultSchema = Schema(
            "test",
            topic = Topic.COVID_19,
            elements = listOf(
                Element(
                    "test_result_element",
                    type = Element.Type.TABLE,
                    table = "test_result",
                    tableColumn = "code",
                    mapperOverridesValue = true
                ),
                Element(
                    "no_table",
                    type = Element.Type.TABLE,
                    tableColumn = "code",
                    mapperOverridesValue = true
                ),
                Element(
                    "no_column",
                    type = Element.Type.TABLE,
                    table = "test_result",
                    mapperOverridesValue = true
                ),
                Element(
                    "wrong_column",
                    type = Element.Type.TABLE,
                    table = "test_result",
                    tableColumn = "wrong",
                    mapperOverridesValue = true
                )
            )
        )
        val genderMetadata = Metadata(
            schema = genderSchema,
            table = genderTable,
            tableName = "gender"
        )
        val testResultMetadata = Metadata(
            schema = testResultSchema,
            table = testResultTable,
            tableName = "test_result"
        )

        // it should look up an element without a version specified
        val lookupElementGender = genderMetadata.findSchema("test-gender")?.findElement("patient_gender") ?: fail("")
        val mapper = LookupSenderAutomationValuesets()
        val argsGender = listOf("patient_gender", "gender")
        val elementAndValuesGender = listOf(ElementAndValue(lookupElementGender, "Female"))
        assertThat(mapper.valueNames(lookupElementGender, argsGender)).isEqualTo(listOf("patient_gender", "gender"))
        assertThat(mapper.apply(lookupElementGender, argsGender, elementAndValuesGender).value).isEqualTo("F")

        // it should look up an element without a version specified but other rows of the same valueset have a version
        val lookupElementTestResult =
            testResultMetadata.findSchema("test")?.findElement("test_result_element") ?: fail("")
        val argsTestResult = listOf("test_result_element", "test_result")
        val elementAndValuesTestResult = listOf(
            ElementAndValue(
                lookupElementTestResult,
                "Specimen unsatisfactory for evaluation"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResult, argsTestResult))
            .isEqualTo(
                listOf(
                    "test_result_element",
                    "test_result"
                )
            )
        assertThat(
            mapper
                .apply(
                    lookupElementTestResult,
                    argsTestResult,
                    elementAndValuesTestResult
                ).value
        )
            .isEqualTo("125154007")

        // it should look up an element with a version specified
        val lookupElementTestResultVersion =
            testResultMetadata.findSchema("test")?.findElement("test_result_element") ?: fail("")
        val argsTestResultVersion = listOf("test_result_element", "test_result", "20200309")
        val elementAndValuesTestResultVersion = listOf(
            ElementAndValue(
                lookupElementTestResultVersion,
                "Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResultVersion, argsTestResultVersion))
            .isEqualTo(listOf("test_result_element", "test_result", "20200309"))

        assertThat(
            mapper
                .apply(
                    lookupElementTestResultVersion,
                    argsTestResultVersion,
                    elementAndValuesTestResultVersion
                ).value
        )
            .isEqualTo("840539006")

        // it should return null and an error in the ElementResult if there is a mismatch of version and display
        val lookupElementTestResultMisVersionDisplay =
            testResultMetadata.findSchema("test")?.findElement("test_result_element") ?: fail("")
        val argsTestResultVersionMisVersionDisplay = listOf("test_result_element", "test_result", "20200309")
        val elementAndValuesTestResultMisVersionDisplay = listOf(
            ElementAndValue(
                lookupElementTestResultMisVersionDisplay,
                "Specimen unsatisfactory for evaluation"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResultMisVersionDisplay, argsTestResultVersionMisVersionDisplay))
            .isEqualTo(listOf("test_result_element", "test_result", "20200309"))

        val elementResultMisVersionDisplay = mapper
            .apply(
                lookupElementTestResultMisVersionDisplay,
                argsTestResultVersionMisVersionDisplay,
                elementAndValuesTestResultMisVersionDisplay
            )

        assertThat(
            elementResultMisVersionDisplay.value
        )
            .isEqualTo(null)

        assertThat(
            elementResultMisVersionDisplay.errors[0].message
        ).contains("no value for element")

        // it should return null and an error in the ElementResult if there is a mismatch of display
        val lookupElementTestResultMisDisplay =
            testResultMetadata.findSchema("test")?.findElement("test_result_element") ?: fail("")
        val argsTestResultVersionMisDisplay = listOf("test_result_element", "test_result")
        val elementAndValuesTestResultMisDisplay = listOf(
            ElementAndValue(
                lookupElementTestResultMisDisplay,
                "This display does not exist"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResultMisDisplay, argsTestResultVersionMisDisplay))
            .isEqualTo(listOf("test_result_element", "test_result"))

        val elementResultMisDisplay = mapper
            .apply(
                lookupElementTestResultMisDisplay,
                argsTestResultVersionMisDisplay,
                elementAndValuesTestResultMisDisplay
            )
        assertThat(
            elementResultMisDisplay.value
        )
            .isEqualTo(null)

        assertThat(
            elementResultMisDisplay.errors[0].message
        ).contains("no value for element")

        // it should return null and an error in the ElementResult if there is no table name in Element
        val lookupElementTestResultWrongTable =
            testResultMetadata.findSchema("test")?.findElement("no_table") ?: fail("")
        val argsTestResultVersionWrongTable = listOf("test_result_element", "no_table", "20200309")
        val elementAndValuesTestResultWrongTable = listOf(
            ElementAndValue(
                lookupElementTestResultWrongTable,
                "It shouldn't even use this value"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResultWrongTable, argsTestResultVersionWrongTable))
            .isEqualTo(listOf("test_result_element", "no_table", "20200309"))

        val elementResultWrongTable = mapper
            .apply(
                lookupElementTestResultWrongTable,
                argsTestResultVersionWrongTable,
                elementAndValuesTestResultWrongTable
            )
        assertThat(
            elementResultWrongTable.value
        )
            .isEqualTo(null)

        assertThat(
            elementResultWrongTable.errors[0].message
        ).contains("Schema Error: could not find table")

        // it should return null and an error in the ElementResult if there is no column name in Element
        val lookupElementTestResultNoColumn =
            testResultMetadata.findSchema("test")?.findElement("no_column") ?: fail("")
        val argsTestResultVersionNoColumn = listOf("test_result_element", "no_column", "20200309")
        val elementAndValuesTestResultNoColumn = listOf(
            ElementAndValue(
                lookupElementTestResultNoColumn,
                "It shouldn't even use this value"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResultNoColumn, argsTestResultVersionNoColumn))
            .isEqualTo(listOf("test_result_element", "no_column", "20200309"))

        val elementResultNoColumn = mapper
            .apply(
                lookupElementTestResultNoColumn,
                argsTestResultVersionNoColumn,
                elementAndValuesTestResultNoColumn
            )
        assertThat(
            elementResultNoColumn.value
        )
            .isEqualTo(null)

        assertThat(
            elementResultNoColumn.errors[0].message
        ).contains("Schema Error: no tableColumn for element")

        // it should return null and an error in the ElementResult if there is a wrong column name in Element
        val lookupElementTestResultWrongColumn =
            testResultMetadata.findSchema("test")?.findElement("wrong_column") ?: fail("")
        val argsTestResultVersionWrongColumn = listOf("test_result_element", "wrong_column", "20200309")
        val elementAndValuesTestResultWrongColumn = listOf(
            ElementAndValue(
                lookupElementTestResultWrongColumn,
                "It shouldn't even use this value"
            )
        )
        assertThat(mapper.valueNames(lookupElementTestResultWrongColumn, argsTestResultVersionWrongColumn))
            .isEqualTo(listOf("test_result_element", "wrong_column", "20200309"))

        val elementResultWrongColumn = mapper
            .apply(
                lookupElementTestResultWrongColumn,
                argsTestResultVersionWrongColumn,
                elementAndValuesTestResultWrongColumn
            )
        assertThat(
            elementResultWrongColumn.value
        )
            .isEqualTo(null)

        assertThat(
            elementResultWrongColumn.errors[0].message
        ).contains("Schema Error: no tableColumn named ${lookupElementTestResultWrongColumn.tableColumn}")
    }

    @Test
    fun `test country mapper`() {
        val mapper = CountryMapper()
        val args = listOf("patient_zip_code")
        val countryElement = Element("patient_country")
        val patientPostalCodeElement = Element("patient_zip_code")
        listOf(ElementAndValue(countryElement, "USA")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("USA")
        }
        listOf(ElementAndValue(countryElement, "MEX")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("MEX")
        }
        listOf(ElementAndValue(countryElement, "CAN")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("CAN")
        }
        listOf(ElementAndValue(countryElement, "USA"), ElementAndValue(patientPostalCodeElement, "90210")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("USA")
        }
        listOf(ElementAndValue(countryElement, "CAN"), ElementAndValue(patientPostalCodeElement, "H0H0H0")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("CAN")
        }
        listOf(ElementAndValue(countryElement, ""), ElementAndValue(patientPostalCodeElement, "90210")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("USA")
        }
        listOf(ElementAndValue(countryElement, ""), ElementAndValue(patientPostalCodeElement, "H0H0H0")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("CAN")
        }
    }

    @Test
    fun `test patient age mapper`() {
        // arrange
        val args = listOf("patient_age", "patient_dob", "specimen_collection_date_time")
        val patientAgeElement = Element("patient_age")
        val patientDobElement = Element("patient_dob")
        val specimenCollectionDateElement = Element("specimen_collection_date_time")
        // build our test cases
        val testCases = mapOf(
            // test case 1 - age pass through
            listOf(
                ElementAndValue(patientAgeElement, "18"),
                ElementAndValue(patientDobElement, ""),
                ElementAndValue(specimenCollectionDateElement, "")
            ) to "18",
            // test case 2 - age pass through but with values. we're ignoring the values and using the
            // age passed in instead because that's the more meaningful value
            listOf(
                ElementAndValue(patientAgeElement, "18"),
                ElementAndValue(patientDobElement, "6/1/1900"),
                ElementAndValue(specimenCollectionDateElement, "6/2/2018")
            ) to "18",
            // test case 3 - age is missing. calculate based on the dob and specimen collection date time
            listOf(
                ElementAndValue(patientAgeElement, ""),
                ElementAndValue(patientDobElement, "6/1/2000"),
                ElementAndValue(specimenCollectionDateElement, "6/2/2018")
            ) to "18",
            // test case 4 - age is missing. calculate based on the dob and specimen collection date time
            // but the DOB is AFTER the specimen collection date, so we should return null
            listOf(
                ElementAndValue(patientAgeElement, ""),
                ElementAndValue(patientDobElement, "6/1/2018"),
                ElementAndValue(specimenCollectionDateElement, "6/2/2017")
            ) to null,
            // test case 5 - everything is null or empty, return the same
            listOf(
                ElementAndValue(patientAgeElement, ""),
                ElementAndValue(patientDobElement, ""),
                ElementAndValue(specimenCollectionDateElement, "")
            ) to null,
            // test case 6 - DOB is one minute before specimen collection. age is zero
            listOf(
                ElementAndValue(patientAgeElement, ""),
                ElementAndValue(patientDobElement, "2022-04-01T16:00:00Z"),
                ElementAndValue(specimenCollectionDateElement, "2022-04-01T16:01:00Z")
            ) to "0",
            // test case 7 - DOB is assumed to be 00:00:00 UTC so it should return zero
            listOf(
                ElementAndValue(patientAgeElement, ""),
                ElementAndValue(patientDobElement, "2022-04-01"),
                ElementAndValue(specimenCollectionDateElement, "2022-04-01T16:01:00Z")
            ) to "0",
            // test case 8 - we aren't worried about future date checking. this is very permissive
            // if someone passes us a specimen collection date in the future we will still calculate
            // this is also verifying that our naïve math around dividing number of days by 365
            // and calculating the floor won't return a bad result
            listOf(
                ElementAndValue(patientAgeElement, ""),
                ElementAndValue(patientDobElement, "2000-05-13"),
                ElementAndValue(specimenCollectionDateElement, "2100-05-13")
            ) to "100",
        )
        // run all our test cases
        PatientAgeMapper().run {
            testCases.forEach {
                val elementsAndValues = it.key
                assertThat(this.apply(patientAgeElement, args, elementsAndValues).value).isEqualTo(it.value)
            }
        }
    }
}