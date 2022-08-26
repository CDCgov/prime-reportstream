package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.isTrue
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

    /**
     * The fake LIVD data to be used in the table
     */
    private val fakeLivdTableData = listOf(
        listOf(
            LivdTableColumns.MODEL.colName, LivdTableColumns.TEST_PERFORMED_CODE.colName,
            LivdTableColumns.TESTKIT_NAME_ID.colName, LivdTableColumns.EQUIPMENT_UID.colName,
            LivdTableColumns.PROCESSING_MODE_CODE.colName
        ),
        // Rows 1-3: Similar to 1copy COVID-19 qPCR Multi Kit, the only change is the equipment UID
        listOf("model1", "90001-1", "model1kit1", "model1uid1", ""),
        listOf("model1", "90001-1", "model1kit1", "model1uid2", ""),
        listOf("model1", "90001-1", "model1kit1", "model1uid3", ""),

        // Rows 4-6: Similar to TRUPCR SARS-CoV-2 Kit, with different LOINC codes
        listOf("model2", "90001-1", "model2kit1", "model2uid1", ""),
        listOf("model2", "90001-1", "model2kit1", "model2uid2", ""),
        listOf("model2", "90001-2", "model2kit1", "model2uid3", ""),

        // Rows: 7-8: Test devices
        listOf("model3", "90003-1", "model3kit1", "model3uid1", LIVDLookupMapper.testProcessingModeCode),
        listOf("model4", "90003-1", "model4kit1", "model4uid1", LIVDLookupMapper.testProcessingModeCode),

        // Rows 9-11: Some devices with similar data, but different test performed code
        listOf("model5", "90005-1", "model5kit1", "model5uid1", ""),
        listOf("model5", "90005-2", "model5kit1", "model5uid1", ""),
        listOf("model5", "90005-3", "model5kit1", "model5uid1", ""),
    )

    private val livdTable = LookupTable("LIVD", fakeLivdTableData)

    private val modelNameElement = Element(
        ElementNames.EQUIPMENT_MODEL_NAME.elementName,
        tableRef = livdTable,
        tableColumn = LivdTableColumns.MODEL.colName
    )
    private val testKitElement = Element(
        ElementNames.TEST_KIT_NAME_ID.elementName,
        tableRef = livdTable,
        tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName
    )
    private val equipmentUidElement = Element(
        ElementNames.EQUIPMENT_MODEL_ID.elementName,
        tableRef = livdTable,
        tableColumn = LivdTableColumns.EQUIPMENT_UID.colName
    )
    private val deviceIdElement = Element(
        ElementNames.DEVICE_ID.elementName,
        tableRef = livdTable
    )
    private val testPerformedCodeElement = Element(
        ElementNames.TEST_PERFORMED_CODE.elementName,
        tableRef = livdTable,
        tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName
    )
    private val processingModeElement = Element(ElementNames.PROCESSING_MODE_CODE.elementName)

    /**
     * Grab a cell value from the LIVD data based on the [deviceindex] starting at 1 and a given [element].
     * This function uses the column name provided in the element to find the data in the table.
     * @return a string with the cell value
     */
    private fun getDeviceCol(deviceindex: Int, element: Element): String {
        check(deviceindex < fakeLivdTableData.size)
        check(deviceindex > 0)
        check(!element.tableColumn.isNullOrBlank())
        val colIndex = fakeLivdTableData[0].indexOf(element.tableColumn)
        check(colIndex >= 0)
        return fakeLivdTableData[deviceindex][colIndex]
    }

    /**
     * Creates an element value object based on the [element] and [deviceIdElement] starting at 1 to
     * be used as input to a mapper.
     * @return the element value object with the data
     */
    private fun createValue(element: Element, deviceindex: Int): ElementAndValue {
        return ElementAndValue(element, getDeviceCol(deviceindex, element))
    }

    @Test
    fun `test LIVD apply mapper lookup logic`() {
        val mapper = LIVDLookupMapper()

        // Bad element
        assertThat { mapper.apply(Element("noTableElement"), emptyList(), emptyList()) }.isFailure()

        // Simple device ID lookups
        var devIndex = 1
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(ElementAndValue(deviceIdElement, "model1kit1")) // Device ID can contain a testkit name
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))

        // Simple test kit lookups
        devIndex = 9
        assertThat(
            mapper.apply(
                equipmentUidElement, emptyList(),
                listOf(createValue(testKitElement, devIndex))
            ).value
        ).isEqualTo(getDeviceCol(devIndex, equipmentUidElement))

        // Simple model name lookups
        devIndex = 1
        assertThat(
            mapper.apply(
                testKitElement, emptyList(),
                listOf(createValue(modelNameElement, devIndex))
            ).value
        ).isEqualTo(getDeviceCol(devIndex, testKitElement))

        // Simple equipment UID lookups
        devIndex = 1
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(createValue(equipmentUidElement, devIndex))
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))

        // Lookup with test performed code
        devIndex = 4
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    createValue(testPerformedCodeElement, devIndex)
                )
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    createValue(testPerformedCodeElement, 7) // Provide some other code
                )
            ).value
        ).isNullOrEmpty()

        // Lookup for test devices
        devIndex = 1
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    ElementAndValue(processingModeElement, "")
                )
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    ElementAndValue(processingModeElement, "P")
                )
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    ElementAndValue(processingModeElement, LIVDLookupMapper.testProcessingModeCode)
                )
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))
        devIndex = 7
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    ElementAndValue(processingModeElement, LIVDLookupMapper.testProcessingModeCode)
                )
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    createValue(testKitElement, devIndex),
                    ElementAndValue(processingModeElement, "")
                )
            ).value
        ).isNullOrEmpty()

        // Test the loop over the different inputs
        devIndex = 7
        assertThat(
            mapper.apply(
                modelNameElement, emptyList(),
                listOf(
                    ElementAndValue(processingModeElement, LIVDLookupMapper.testProcessingModeCode),
                    ElementAndValue(modelNameElement, ""),
                    ElementAndValue(equipmentUidElement, ""),
                    ElementAndValue(deviceIdElement, ""),
                    createValue(testKitElement, devIndex)
                )
            ).value
        ).isEqualTo(getDeviceCol(devIndex, modelNameElement))
    }

    @Test
    fun `test LIVD mapper apply warnings and errors`() {
        val mapper = LIVDLookupMapper()

        // We get a value, so no warnings or errors.
        val devIndex = 7
        var mapperResult = mapper.apply(
            modelNameElement, emptyList(),
            listOf(
                createValue(testKitElement, devIndex),
                ElementAndValue(processingModeElement, LIVDLookupMapper.testProcessingModeCode)
            )
        )
        assertThat(mapperResult.value.isNullOrEmpty()).isFalse()
        assertThat(mapperResult.warnings).isEmpty()
        assertThat(mapperResult.errors).isEmpty()

        // No value, but elements have no fields, so no error or warnings
        mapperResult = mapper.apply(
            modelNameElement, emptyList(),
            listOf(
                createValue(testKitElement, devIndex),
                ElementAndValue(processingModeElement, "")
            )
        )
        assertThat(mapperResult.value.isNullOrEmpty()).isTrue()
        assertThat(mapperResult.warnings).isEmpty()
        assertThat(mapperResult.errors).isEmpty()

        // Element has fields
        // CSV field
        var newModelNameElement = Element(
            ElementNames.EQUIPMENT_MODEL_NAME.elementName,
            tableRef = livdTable,
            tableColumn = LivdTableColumns.MODEL.colName,
            csvFields = listOf(Element.CsvField("name", null))
        )
        mapperResult = mapper.apply(
            newModelNameElement, emptyList(),
            listOf(
                createValue(testKitElement, devIndex),
                ElementAndValue(processingModeElement, "")
            )
        )
        assertThat(mapperResult.value.isNullOrEmpty()).isTrue()
        assertThat(mapperResult.warnings).isNotEmpty()
        assertThat(mapperResult.errors).isEmpty()

        // HL7 field
        newModelNameElement = Element(
            ElementNames.EQUIPMENT_MODEL_NAME.elementName,
            tableRef = livdTable,
            tableColumn = LivdTableColumns.MODEL.colName,
            hl7Field = "OBX-1"
        )
        mapperResult = mapper.apply(
            newModelNameElement, emptyList(),
            listOf(
                createValue(testKitElement, devIndex),
                ElementAndValue(processingModeElement, "")
            )
        )
        assertThat(mapperResult.value.isNullOrEmpty()).isTrue()
        assertThat(mapperResult.warnings).isNotEmpty()
        assertThat(mapperResult.errors).isEmpty()

        // HL7 output field
        newModelNameElement = Element(
            ElementNames.EQUIPMENT_MODEL_NAME.elementName,
            tableRef = livdTable,
            tableColumn = LivdTableColumns.MODEL.colName,
            hl7OutputFields = listOf("OBX-1", "OBX-2")
        )
        mapperResult = mapper.apply(
            newModelNameElement, emptyList(),
            listOf(
                createValue(testKitElement, devIndex),
                ElementAndValue(processingModeElement, "")
            )
        )
        assertThat(mapperResult.value.isNullOrEmpty()).isTrue()
        assertThat(mapperResult.warnings).isNotEmpty()
        assertThat(mapperResult.errors).isEmpty()

        // HL7 field and output fields
        newModelNameElement = Element(
            ElementNames.EQUIPMENT_MODEL_NAME.elementName,
            tableRef = livdTable,
            tableColumn = LivdTableColumns.MODEL.colName,
            hl7OutputFields = listOf("OBX-1", "OBX-2"),
            hl7Field = "OBX-1"
        )
        mapperResult = mapper.apply(
            newModelNameElement, emptyList(),
            listOf(
                createValue(testKitElement, devIndex),
                ElementAndValue(processingModeElement, "")
            )
        )
        assertThat(mapperResult.value.isNullOrEmpty()).isTrue()
        assertThat(mapperResult.warnings).isNotEmpty()
        assertThat(mapperResult.errors).isEmpty()
    }
}