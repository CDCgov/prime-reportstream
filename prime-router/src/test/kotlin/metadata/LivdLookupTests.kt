package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.metadata.LivdLookup.find
import org.junit.jupiter.api.Test

class LivdLookupTests {
    private val livdPath = "./src/test/resources/metadata/tables/LIVD-SARS-CoV-2-2022-01-12.csv"

    @Test
    fun `test livdLookup with DeviceId`() {
        val lookupTable = LookupTable.read(livdPath)

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = "BinaxNOW COVID-19 Ag Card 2 Home Test_Abbott Diagnostics Scarborough, Inc._EUA",
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94558-4")

        // Test with a truncated device ID
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = "BinaxNOW COVID-19 Ag Card 2 Home Test_Abb#",
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94558-4")

        // Test with an ID NOW device id which is has an FDA number
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = "10811877011269_DII",
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94534-5")

        // With GUDID DI
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = "10811877011269",
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94534-5")
    }

    @Test
    fun `test livdLookup with Equipment Model Name`() {
        val lookupTable = LookupTable.read(livdPath)

        // Test with an EUA
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card",
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94558-4")

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card*",
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94558-4")

        // Test with a ID NOW device id
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "ID NOW",
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94534-5")

        // Test for a device ID that has multiple rows and the same test ordered code.
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "1copy COVID-19 qPCR Multi Kit*",
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94500-6")

        // Test for a device ID that has multiple rows and multiple test ordered codes.
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "Alinity i",
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isNull()

        // Test that the warning is only provided for fields that could be sent by a sender
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "1copy COVID-19 qPCR Multi Kit*",
                tableColumn = LivdTableColumns.TEST_PERFORMED_CODE.colName,
                tableRef = lookupTable
            )
        ).isEqualTo("94500-6")
    }

    @Test
    fun `test livdLookup for Sofia 2`() {
        val lookupTable = LookupTable.read(livdPath)

        assertThat(
            find(
                testPerformedCode = "95209-3",
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "Sofia 2 Flu + SARS Antigen FIA*",
                tableColumn = "Test Performed LOINC Long Name",
                tableRef = lookupTable
            )
        )
            .isEqualTo(
                "SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"
            )
    }

    @Test
    fun `test livdLookup supplemental table by device_id`() {
        val lookupTable = LookupTable.read(livdPath)

        // Test with an FDA device id
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = "10811877011337",
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableColumn = "is_otc",
                tableRef = lookupTable
            )
        )
            .isEqualTo("N")

        // Test with a truncated device ID
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = "BinaxNOW COVID-19 Ag Card 2 Home#",
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableColumn = "is_otc",
                tableRef = lookupTable
            )
        )
            .isEqualTo("Y")
    }

    @Test
    fun `test livdLookup supplemental table by model`() {
        val lookupTable = LookupTable.read(livdPath)

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card Home Test",
                tableColumn = "is_otc",
                tableRef = lookupTable
            )
        )
            .isEqualTo("N")

        // Test with another
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card 2 Home Test",
                tableColumn = "is_otc",
                tableRef = lookupTable
            )
        )
            .isEqualTo("Y")

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "Some bad text",
                tableColumn = "is_otc",
                tableRef = lookupTable
            )
        )
            .isNullOrEmpty()
    }

    @Test
    fun `test supplemental devices for test only`() {
        val lookupTable = LookupTable.read(livdPath)

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = "P",
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = "Test_OTC_Device",
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = lookupTable
            )
        )
            .isNullOrEmpty()

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = "Test_OTC_Device",
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = lookupTable
            )
        )
            .isNullOrEmpty()

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = "T",
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = "Test_OTC_Device",
                equipmentModelName = null,
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = lookupTable
            )
        )
            .isEqualTo("Test_OTC_Device")

        // Test_OTC_Device is a test only device
        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = "P",
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card",
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = lookupTable
            )
        )
            .isEqualTo("10811877011290")

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card",
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = lookupTable
            )
        )
            .isEqualTo("10811877011290")

        assertThat(
            find(
                testPerformedCode = null,
                processingModeCode = "T",
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = "BinaxNOW COVID-19 Ag Card",
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = lookupTable
            )
        )
            .isEqualTo("10811877011290")
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
        listOf("model3", "90003-1", "model3kit1", "model3uid1", LivdLookup.testProcessingModeCode),
        listOf("model4", "90003-1", "model4kit1", "model4uid1", LivdLookup.testProcessingModeCode),

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
        // Simple device ID lookups
        assertThat(
            find(
                deviceId = "model1kit1",
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(1, modelNameElement))

        var ev = createValue(testKitElement, 9)
        // Simple test kit lookups
        assertThat(
            find(
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.EQUIPMENT_UID.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(9, equipmentUidElement))

        // Simple model name lookups
        ev = createValue(modelNameElement, 1)
        assertThat(
            find(
                equipmentModelName = ev.value,
                tableColumn = LivdTableColumns.TESTKIT_NAME_ID.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(1, testKitElement))

        // Simple equipment UID lookups
        ev = createValue(equipmentUidElement, 1)
        assertThat(
            find(
                equipmentModelId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(1, modelNameElement))

        // Lookup with test performed code
        ev = createValue(testKitElement, 4)
        var ev2 = createValue(testPerformedCodeElement, 4)
        assertThat(
            find(
                testPerformedCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(4, modelNameElement))

        ev = createValue(testKitElement, 4)
        ev2 = createValue(testPerformedCodeElement, 7) // Provide some other code
        assertThat(
            find(
                testPerformedCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isNullOrEmpty()

        // Lookup for test devices
        ev = createValue(testKitElement, 1)
        ev2 = ElementAndValue(processingModeElement, "")
        assertThat(
            find(
                processingModeCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(1, modelNameElement))

        ev = createValue(testKitElement, 1)
        ev2 = ElementAndValue(processingModeElement, "P")
        assertThat(
            find(
                processingModeCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(1, modelNameElement))

        ev = createValue(testKitElement, 1)
        ev2 = ElementAndValue(processingModeElement, LivdLookup.testProcessingModeCode)
        assertThat(
            find(
                processingModeCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(1, modelNameElement))

        ev = createValue(testKitElement, 7)
        ev2 = ElementAndValue(processingModeElement, LivdLookup.testProcessingModeCode)
        assertThat(
            find(
                processingModeCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(7, modelNameElement))

        ev = createValue(testKitElement, 7)
        ev2 = ElementAndValue(processingModeElement, "")
        assertThat(
            find(
                processingModeCode = ev2.value,
                testKitNameId = ev.value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isNullOrEmpty()

        // Test the loop over the different inputs
        ev = createValue(testKitElement, 7)
        assertThat(
            find(
                processingModeCode = LivdLookup.testProcessingModeCode,
                deviceId = "",
                equipmentModelId = "",
                testKitNameId = ev.value,
                equipmentModelName = "",
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isEqualTo(getDeviceCol(7, modelNameElement))
    }

    @Test
    fun `test LIVD mapper apply warnings and errors`() {
        // We get a value, so no warnings or errors.
        assertThat(
            find(
                processingModeCode = LivdLookup.testProcessingModeCode,
                testKitNameId = createValue(testKitElement, 7).value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isNotNull()

        // No value, but elements have no fields, so no error or warnings
        assertThat(
            find(
                processingModeCode = "",
                testKitNameId = createValue(testKitElement, 7).value,
                tableColumn = LivdTableColumns.MODEL.colName,
                tableRef = livdTable
            )
        )
            .isNull()
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
        assertThat(
            LivdLookup.lookupByEquipmentModelName(
                element.tableColumn!!,
                testModel,
                lookupTable.FilterBuilder()
            )
        ).isEqualTo(expectedTestOrderedLoinc)

        // Add an * to the end of the model name
        assertThat(
            LivdLookup.lookupByEquipmentModelName(
                element.tableColumn!!, "$testModel*", lookupTable.FilterBuilder()
            )
        ).isEqualTo(expectedTestOrderedLoinc)

        // Add some other character to fail the lookup
        assertThat(
            LivdLookup.lookupByEquipmentModelName(
                element.tableColumn!!, "$testModel^", lookupTable.FilterBuilder()
            )
        ).isNull()

        // Accula SARS-Cov-2 Test does have an * in the table
        testModel = "Accula SARS-Cov-2 Test"
        expectedTestOrderedLoinc = "95409-9"
        assertThat(
            LivdLookup.lookupByEquipmentModelName(
                element.tableColumn!!, testModel, lookupTable.FilterBuilder()
            )
        ).isEqualTo(expectedTestOrderedLoinc)

        // Add an * to the end of the model name
        assertThat(
            LivdLookup.lookupByEquipmentModelName(
                element.tableColumn!!, "$testModel*", lookupTable.FilterBuilder()
            )
        ).isEqualTo(expectedTestOrderedLoinc)
    }
}