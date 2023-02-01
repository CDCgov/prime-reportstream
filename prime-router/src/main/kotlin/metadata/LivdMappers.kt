package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.ElementResult
import gov.cdc.prime.router.InvalidEquipmentMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomFHIRFunctions

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
        sender: Sender?
    ): ElementResult {
        val testPerformedCode = values.firstOrNull {
            it.element.name == ElementNames.TEST_PERFORMED_CODE.elementName
        }?.value
        val processingModeCode = values.firstOrNull {
            it.element.name == ElementNames.PROCESSING_MODE_CODE.elementName
        }?.value

        // carry on as usual
        values.forEach {
            val result = CustomFHIRFunctions.lookupLoincCode(
                testPerformedCode,
                processingModeCode,
                it.element.name,
                it.value,
                element.tableColumn!!
            )
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

    companion object {
        private val standard99ELRTypes = listOf("EUA", "DII", "DIT", "DIM", "MNT", "MNI", "MNM")

        /**
         * The value for the processing mode code for test data.
         */
        internal const val testProcessingModeCode = "T"

        /**
         * Does a lookup in the LIVD table based on the element Id
         * @param element the schema element to use for lookups
         * @param deviceId the ID of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        fun lookupByDeviceId(
            tableColumn: String?,
            elementName: String,
            deviceId: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            /*
             Dev Note:

             From the LIVD implementation notes says that device_id is not well defined:
              "The Device Identifier (DI) may be a Test Kit Name Identifier or the Equipment (IVD) Identifier
               or a combination of the two. "

             This note discusses many of the forms for the device_id
               https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#
               ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification
             */

            if (deviceId.isBlank()) return null

            // Device Id may be 99ELR type
            val suffix = deviceId.substringAfterLast('_', "")
            if (standard99ELRTypes.contains(suffix)) {
                val value = deviceId.substringBeforeLast('_', "")
                return lookup(tableColumn, elementName, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                    ?: lookup(tableColumn, elementName, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
            }

            // truncated 99ELR type
            if (deviceId.endsWith("#")) {
                val value = deviceId.substringBeforeLast('#', "")
                return lookupPrefix(tableColumn, elementName, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                    ?: lookupPrefix(tableColumn, elementName, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
            }

            // May be the DI from a GUDID either test-kit or equipment
            return lookup(tableColumn, elementName, deviceId, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                ?: lookup(tableColumn, elementName, deviceId, LivdTableColumns.EQUIPMENT_UID.colName, filters)
        }

        /**
         * Does a lookup in the LIVD table based on the element unique identifier
         * @param element the schema element to use for lookups
         * @param value the unique ID of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        fun lookupByEquipmentUid(
            tableColumn: String?,
            elementName: String,
            value: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            if (value.isBlank()) return null
            return lookup(tableColumn, elementName, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
        }

        /**
         * Does a lookup in the LIVD table based on the test kit Id
         * @param element the schema element to use for lookups
         * @param value the test kit ID of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        fun lookupByTestkitId(
            tableColumn: String?,
            elementName: String,
            value: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            if (value.isBlank()) return null
            return lookup(tableColumn, elementName, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
        }

        /**
         * Does a lookup in the LIVD table based on the equipment model name
         * @param element the schema element to use for lookups
         * @param value the model name of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        fun lookupByEquipmentModelName(
            tableColumn: String?,
            elementName: String,
            value: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            if (value.isBlank()) return null
            return lookup(
                tableColumn,
                elementName,
                LivdLookupUtilities.getCleanedModelName(value),
                LivdTableColumns.MODEL.colName,
                filters
            )
        }

        /**
         * Does the lookup in the LIVD table based on the lookup type and the values passed in
         * @param element the schema element to use for lookups
         * @param onColumn the name of the index column to do the lookup in
         * @param lookup the value to search the index column for
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        private fun lookup(
            lookupColumn: String?,
            elementName: String,
            lookup: String,
            onColumn: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            lookupColumn ?: error("Schema Error: no tableColumn for element '$elementName'")
            return filters.equalsIgnoreCase(onColumn, lookup).findSingleResult(lookupColumn)
        }

        /**
         * Does the lookup in the LIVD table based on the lookup type and the values passed in,
         * by seeing if any values in the index column starts with the index value
         * @param element the schema element to use for lookups
         * @param onColumn the name of the index column to do the lookup in
         * @param lookup the value to search the index column for
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        private fun lookupPrefix(
            lookupColumn: String?,
            elementName: String,
            lookup: String,
            onColumn: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            lookupColumn ?: error("Schema Error: no tableColumn for element '$elementName'")
            return filters.startsWithIgnoreCase(onColumn, lookup).findSingleResult(lookupColumn)
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