package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.ElementResult
import gov.cdc.prime.router.InvalidEquipmentMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.metadata.LivdLookup.find

/**
 * Column names in the LIVD table.
 */
enum class LivdTableColumns(val colName: String) {
    TESTKIT_NAME_ID("Testkit Name ID"),
    EQUIPMENT_UID("Equipment UID"),
    MODEL("Model"),
    TEST_PERFORMED_CODE("Test Performed LOINC Code"),
    PROCESSING_MODE_CODE("processing_mode_code"),
    MANUFACTURER("Manufacturer")
}

/**
 * Element names
 */
enum class ElementNames(val elementName: String) {
    DEVICE_ID("device_id"),
    EQUIPMENT_MODEL_ID("equipment_model_id"),
    EQUIPMENT_MODEL_NAME("equipment_model_name"),
    TEST_KIT_NAME_ID("test_kit_name_id"),
    TEST_PERFORMED_CODE("test_performed_code"),
    PROCESSING_MODE_CODE("processing_mode_code"),
}

object LivdLookupUtilities {
    /**
     * Clean up a [modelName].
     * @return cleaned up model name
     */
    fun getCleanedModelName(modelName: String): String {
        // Remove a single * from the end of the name
        return if (modelName.endsWith("*")) modelName.dropLast(1) else modelName
    }
}
/**
 * This is a lookup mapper specialized for the LIVD table. The LIVD table has multiple columns
 * which could be used for lookup. Different senders send different information, so this mapper
 * incorporates business logic to do this lookup based on the available information.
 *
 * This function uses covid-19 schema elements in the following order:
 * - device_id - From OBX-17.1 and OBX-17.3, may be a FDA GUDID or a textual description
 * - equipment_model_id - From OBX-18.1, matches column 0
 * - test_kit_name_id - matches column M
 * - equipment_model_name - From STRAC, SimpleReport, and many CSVs, matches on column B
 *
 * Example Usage
 *
 *   - name: test_performed_system_version
 *     type: TABLE
 *     table: LIVD-SARS-CoV-2-2021-01-20        # Specific version of the LIVD table to use
 *     tableColumn: LOINC Version ID            # Column in the table to map
 *     mapper: livdLookup()
 *
 */
class LIVDLookupMapper : Mapper {
    override val name = "livdLookup"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isNotEmpty())
            error("Schema Error: livdLookup mapper does not expect args")
        // EQUIPMENT_MODEL_NAME is the more stable id so it goes first. Device_id will change as devices change from
        // emergency use to fully authorized status in the LIVD table
        return listOf(
            ElementNames.EQUIPMENT_MODEL_NAME.elementName, ElementNames.DEVICE_ID.elementName,
            ElementNames.EQUIPMENT_MODEL_ID.elementName, ElementNames.TEST_KIT_NAME_ID.elementName,
            ElementNames.TEST_PERFORMED_CODE.elementName, ElementNames.PROCESSING_MODE_CODE.elementName
        )
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?,
    ): ElementResult {
        val testPerformedCode = values.firstOrNull {
            it.element.name == ElementNames.TEST_PERFORMED_CODE.elementName
        }?.value
        val processingModeCode = values.firstOrNull {
            it.element.name == ElementNames.PROCESSING_MODE_CODE.elementName
        }?.value

        // carry on as usual
        values.forEach {
            val result = when (it.element.name) {
                ElementNames.EQUIPMENT_MODEL_NAME.elementName -> find(
                    testPerformedCode = testPerformedCode,
                    processingModeCode = processingModeCode,
                    equipmentModelName = it.value,
                    tableColumn = element.tableColumn!!,
                    tableRef = element.tableRef
                )
                ElementNames.DEVICE_ID.elementName -> find(
                    testPerformedCode = testPerformedCode,
                    processingModeCode = processingModeCode,
                    deviceId = it.value,
                    tableColumn = element.tableColumn!!,
                    tableRef = element.tableRef
                )
                ElementNames.EQUIPMENT_MODEL_ID.elementName -> find(
                    testPerformedCode = testPerformedCode,
                    processingModeCode = processingModeCode,
                    equipmentModelId = it.value,
                    tableColumn = element.tableColumn!!,
                    tableRef = element.tableRef
                )
                ElementNames.TEST_KIT_NAME_ID.elementName -> find(
                    testPerformedCode = testPerformedCode,
                    processingModeCode = processingModeCode,
                    testKitNameId = it.value,
                    tableColumn = element.tableColumn!!,
                    tableRef = element.tableRef
                )
                else -> null
            }

            if (result != null) return ElementResult(result)
        }

        return ElementResult(null).also {
            // Hide any warnings to fields the user does not send to us
            if (!element.csvFields.isNullOrEmpty() || !element.hl7Field.isNullOrBlank() ||
                !element.hl7OutputFields.isNullOrEmpty()
            )
                it.warning(InvalidEquipmentMessage(element.fieldMapping))
        }
    }
}

/**
 * The obx17 mapper is specific to the LIVD table and the DeviceID field. Do not use in other places.
 *
 * @See <a href=https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification>HHS Submission Guidance</a>Do not use it for other fields and tables.
 */
class Obx17Mapper : Mapper {
    override val name = "obx17"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isNotEmpty())
            error("Schema Error: obx17 mapper does not expect args")
        return listOf("equipment_model_name")
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.isEmpty()) {
                null
            } else {
                val (indexElement, indexValue) = values.first()
                val lookupTable = element.tableRef
                    ?: error("Schema Error: could not find table '${element.table}'")
                val indexColumn = indexElement.tableColumn
                    ?: error("Schema Error: no tableColumn for element '${indexElement.name}'")

                // Remove any * coming in the model value
                val sanitizedValue = if (indexElement.name == ElementNames.EQUIPMENT_MODEL_NAME.elementName)
                    LivdLookupUtilities.getCleanedModelName(indexValue) else indexValue

                val testKitNameId = lookupTable.FilterBuilder().equalsIgnoreCase(indexColumn, sanitizedValue)
                    .findSingleResult("Testkit Name ID")
                val testKitNameIdType = lookupTable.FilterBuilder().equalsIgnoreCase(indexColumn, sanitizedValue)
                    .findSingleResult("Testkit Name ID Type")
                if (testKitNameId != null && testKitNameIdType != null) {
                    "${testKitNameId}_$testKitNameIdType"
                } else {
                    null
                }
            }
        )
    }
}

/**
 * The obx17Type mapper is specific to the LIVD table and the DeviceID field. Do not use in other places.
 *
 * @See <a href=https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification>HHS Submission Guidance</a>Do not use it for other fields and tables.
 */
class Obx17TypeMapper : Mapper {
    override val name = "obx17Type"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isNotEmpty())
            error("Schema Error: obx17Type mapper does not expect args")
        return listOf(ElementNames.EQUIPMENT_MODEL_NAME.elementName)
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.isEmpty()) {
                null
            } else {
                val (indexElement, indexValue) = values.first()
                val lookupTable = element.tableRef
                    ?: error("Schema Error: could not find table '${element.table}'")
                val indexColumn = indexElement.tableColumn
                    ?: error("Schema Error: no tableColumn for element '${indexElement.name}'")
                if (lookupTable.FilterBuilder().equalsIgnoreCase(
                        indexColumn,
                        LivdLookupUtilities.getCleanedModelName(indexValue)
                    )
                    .findSingleResult("Testkit Name ID") != null
                ) "99ELR" else null
            }
        )
    }
}