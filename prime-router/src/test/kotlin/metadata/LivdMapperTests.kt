package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import gov.cdc.prime.router.Element
import kotlin.test.Test

class LivdMapperTests {
    private val livdPath = "./src/test/resources/metadata/tables/LIVD-SARS-CoV-2-2022-01-12.csv"

    @Test
    fun `test livdLookup with DeviceId`() {
        val lookupTable = LookupTable.read(livdPath)
        val codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName
        )
        val deviceElement = Element(ElementNames.DEVICE_ID.elementName)
        val mapper = LIVDLookupMapper()

        // Test with a EUA
        val ev1 = ElementAndValue(
            deviceElement,
            "BinaxNOW COVID-19 Ag Card 2 Home Test_Abbott Diagnostics Scarborough, Inc._EUA"
        )
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1)).value).isEqualTo("94558-4")

        // Test with a truncated device ID
        val ev1a = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card 2 Home Test_Abb#")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1a)).value).isEqualTo("94558-4")

        // Test with an ID NOW device id which is has an FDA number
        val ev2 = ElementAndValue(deviceElement, "10811877011269_DII")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev2)).value).isEqualTo("94534-5")

        // With GUDID DI
        val ev3 = ElementAndValue(deviceElement, "10811877011269")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev3)).value).isEqualTo("94534-5")
    }

    @Test
    fun `test livdLookup with Equipment Model Name`() {
        val lookupTable = LookupTable.read(livdPath)
        var codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName
        )
        val modelElement = Element(ElementNames.EQUIPMENT_MODEL_NAME.elementName)
        val mapper = LIVDLookupMapper()

        // Test with an EUA
        var ev1 = ElementAndValue(modelElement, "BinaxNOW COVID-19 Ag Card")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1)).value).isEqualTo("94558-4")
        ev1 = ElementAndValue(modelElement, "BinaxNOW COVID-19 Ag Card*")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1)).value).isEqualTo("94558-4")

        // Test with a ID NOW device id
        val ev2 = ElementAndValue(modelElement, "ID NOW")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev2)).value).isEqualTo("94534-5")

        // Test for a device ID that has multiple rows and the same test ordered code.
        val ev3 = ElementAndValue(modelElement, "1copy COVID-19 qPCR Multi Kit*")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev3)).value).isEqualTo("94500-6")

        // Test for a device ID that has multiple rows and multiple test ordered codes.
        val ev4 = ElementAndValue(modelElement, "Alinity i")
        mapper.apply(codeElement, emptyList(), listOf(ev4)).also {
            assertThat(it.value).isNull()
            assertThat(it.warnings).isEmpty()
        }

        // Test that the warning is only provided for fields that could be sent by a sender
        codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
            hl7Field = "OBX-1"
        )
        mapper.apply(codeElement, emptyList(), listOf(ev4)).also {
            assertThat(it.value).isNull()
            assertThat(it.warnings).isNotEmpty()
        }
        codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
            hl7OutputFields = listOf("OBX-1")
        )
        mapper.apply(codeElement, emptyList(), listOf(ev4)).also {
            assertThat(it.value).isNull()
            assertThat(it.warnings).isNotEmpty()
        }
        codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
            csvFields = listOf(Element.CsvField("somefield", null))
        )
        mapper.apply(codeElement, emptyList(), listOf(ev4)).also {
            assertThat(it.value).isNull()
            assertThat(it.warnings).isNotEmpty()
        }

        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev4)).value).isNull()
    }

    @Test
    fun `test livdLookup for Sofia 2`() {
        val lookupTable = LookupTable.read(livdPath)
        val codeElement = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = "Test Performed LOINC Long Name"
        )
        val modelElement = Element(ElementNames.EQUIPMENT_MODEL_NAME.elementName)
        val testPerformedElement = Element(ElementNames.TEST_PERFORMED_CODE.elementName)
        val mapper = LIVDLookupMapper()

        mapper.apply(
            codeElement,
            emptyList(),
            listOf(
                ElementAndValue(modelElement, "Sofia 2 Flu + SARS Antigen FIA*"),
                ElementAndValue(testPerformedElement, "95209-3")
            )
        ).let {
            assertThat(it.value)
                .equals("SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay")
        }
    }

    @Test
    fun `test livdLookup supplemental table by device_id`() {
        val lookupTable = LookupTable.read(livdPath)
        val codeElement = Element(
            "test_authorized_for_otc",
            tableRef = lookupTable,
            tableColumn = "is_otc"
        )
        val deviceElement = Element(ElementNames.DEVICE_ID.elementName)
        val mapper = LIVDLookupMapper()

        // Test with an FDA device id
        val ev1 = ElementAndValue(deviceElement, "10811877011337")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1)).value).isEqualTo("N")

        // Test with a truncated device ID
        val ev1a = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card 2 Home#")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1a)).value).isEqualTo("Y")
    }

    @Test
    fun `test livdLookup supplemental table by model`() {
        val lookupTable = LookupTable.read(livdPath)
        val codeElement = Element(
            "test_authorized_for_otc",
            tableRef = lookupTable,
            tableColumn = "is_otc",
            hl7Field = "OBX-1"
        )
        val deviceElement = Element(ElementNames.EQUIPMENT_MODEL_NAME.elementName)
        val mapper = LIVDLookupMapper()

        // Test with an FDA device id
        val ev1 = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card Home Test")
        var result = mapper.apply(codeElement, emptyList(), listOf(ev1))
        assertThat(result.value).isEqualTo("N")
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings).isEmpty()

        // Test with another
        val ev1a = ElementAndValue(deviceElement, "BinaxNOW COVID-19 Ag Card 2 Home Test")
        assertThat(mapper.apply(codeElement, emptyList(), listOf(ev1a)).value).isEqualTo("Y")

        val ev2 = ElementAndValue(deviceElement, "Some bad text")
        result = mapper.apply(codeElement, emptyList(), listOf(ev2))
        assertThat(result.value).isNullOrEmpty()
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings).isNotEmpty()
    }

    @Test
    fun `test livdLookup model variation lookup`() {
        val lookupTable = LookupTable.read(livdPath)
        val element = Element(
            "ordered_test_code",
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName
        )

        // Cue COVID-19 Test does not have an * in the table
        var testModel = "Cue COVID-19 Test"
        var expectedTestOrderedLoinc = "95409-9"
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, testModel, lookupTable.FilterBuilder()))
            .isEqualTo(expectedTestOrderedLoinc)

        // Add an * to the end of the model name
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, "$testModel*", lookupTable.FilterBuilder()))
            .isEqualTo(expectedTestOrderedLoinc)

        // Add some other character to fail the lookup
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, "$testModel^", lookupTable.FilterBuilder()))
            .isNull()

        // Accula SARS-Cov-2 Test does have an * in the table
        testModel = "Accula SARS-Cov-2 Test"
        expectedTestOrderedLoinc = "95409-9"
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, testModel, lookupTable.FilterBuilder()))
            .isEqualTo(expectedTestOrderedLoinc)

        // Add an * to the end of the model name
        assertThat(LIVDLookupMapper.lookupByEquipmentModelName(element, "$testModel*", lookupTable.FilterBuilder()))
            .isEqualTo(expectedTestOrderedLoinc)
    }

    @Test
    fun `test supplemental devices for test only`() {
        val lookupTable = LookupTable.read(livdPath)
        val modelElement = Element(
            ElementNames.EQUIPMENT_MODEL_NAME.elementName,
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.MODEL.colName
        )
        val testKitElement = Element(
            ElementNames.TEST_KIT_NAME_ID.elementName,
            tableRef = lookupTable,
            tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName
        )
        val processingCodeElement = Element(
            ElementNames.PROCESSING_MODE_CODE.elementName
        )

        val mapper = LIVDLookupMapper()
        val modeP = ElementAndValue(processingCodeElement, "P")
        val modeT = ElementAndValue(processingCodeElement, "T")

        // Test_OTC_Device is a test only device
        val ev1 = ElementAndValue(modelElement, "Test_OTC_Device")
        assertThat(mapper.apply(testKitElement, emptyList(), listOf(ev1, modeP)).value).isNullOrEmpty()
        assertThat(mapper.apply(testKitElement, emptyList(), listOf(ev1)).value).isNullOrEmpty()
        assertThat(mapper.apply(testKitElement, emptyList(), listOf(ev1, modeT)).value).isEqualTo(ev1.value)

        // Test_OTC_Device is a test only device
        val ev2 = ElementAndValue(modelElement, "BinaxNOW COVID-19 Ag Card")
        val testKitIdBinax = "10811877011290"
        assertThat(mapper.apply(testKitElement, emptyList(), listOf(ev2, modeP)).value).isEqualTo(testKitIdBinax)
        assertThat(mapper.apply(testKitElement, emptyList(), listOf(ev2)).value).isEqualTo(testKitIdBinax)
        assertThat(mapper.apply(testKitElement, emptyList(), listOf(ev2, modeT)).value).isEqualTo(testKitIdBinax)
    }
}