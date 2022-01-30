package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.ElementResult
import gov.cdc.prime.router.InvalidEquipmentMessage
import gov.cdc.prime.router.Sender

/**
 * Column names in the LIVD table.
 */
enum class LivdTableColumns(val colName: String) {
    TESTKIT_NAME_ID("Testkit Name ID"),
    EQUIPMENT_UID("Equipment UID"),
    MODEL("Model"),
    TEST_PERFORMED_CODE("Test Performed LOINC Code"),
    PROCESSING_MODE_CODE("processing_mode_code"),
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
        val lookupTable = element.tableRef
            ?: error("Schema Error: could not find table '${element.table}'")
        val filters = lookupTable.FilterBuilder()
        // get the test performed code for additional filtering of the test information in case we are
        // dealing with tests that check for more than one type of disease, for example COVID + influenza
        values.firstOrNull { it.element.name == ElementNames.TEST_PERFORMED_CODE.elementName }?.value?.also {
            filters.notEqualsIgnoreCase(LivdTableColumns.TEST_PERFORMED_CODE.colName, testProcessingModeCode)
        }

        // If the data is NOT flagged as test data then ignore any test devices in the LIVD table
        val processingModeCode = values.firstOrNull {
            it.element.name == ElementNames.PROCESSING_MODE_CODE.elementName
        }?.value
        if (processingModeCode == null || processingModeCode.uppercase() != testProcessingModeCode) {
            filters.notEqualsIgnoreCase(LivdTableColumns.PROCESSING_MODE_CODE.colName, testProcessingModeCode)
        }

        val testPerformedCode = values.firstOrNull {
            it.element.name == ElementNames.TEST_PERFORMED_CODE.elementName
        }?.value

        // carry on as usual
        values.forEach {
            val filtersCopy = filters.copy() // Filters are not reusable
            val result = when (it.element.name) {
                ElementNames.DEVICE_ID.elementName -> lookupByDeviceId(element, it.value, filtersCopy)
                ElementNames.EQUIPMENT_MODEL_ID.elementName -> lookupByEquipmentUid(element, it.value, filtersCopy)
                ElementNames.TEST_KIT_NAME_ID.elementName -> lookupByTestkitId(element, it.value, filtersCopy)
                ElementNames.EQUIPMENT_MODEL_NAME.elementName -> {
                    // from time to time we will encounter tests that are multiplex, which means they can be
                    // used to test for more than one disease. Unfortunately for us, the LIVD table includes
                    // not just the COVID results, but also all the other diagnostic LOINC codes for the other
                    // diseases the test is designed for. For example, the Sofia 2 test can check for influenza
                    // A, B, *AND* COVID. This means that when we filter by the equipment model name, we can
                    // end up with more than one row returned from the LIVD table. When that happens, the lookup
                    // method below will only return a value if there's only a single row returned by the filtering,
                    // and therefore returns null for any multiplex test. *****THIS IS NOT OKAY*****
                    // I have added logic here to check and see if we have a test performed code as well,
                    // and if we do, we use that to winnow down the list further. We can't depend on this for all
                    // senders because not all senders give us the test performed code. We also **CANNOT** just use
                    // the test performed code to get the values that we want from the LIVD table. For example,
                    // test 94500-6 matches 188 tests from a multitude of different manufacturers right now, which
                    // means we'd never be able to fill out other parts of the HL7 message as we need to.
                    // THEREFORE...
                    // I am pulling the test ordered code from the list of values we get, and if it is present
                    // then we can use that to filter down to a single matching row as the lookup logic expects
                    // and get back the test results we expect
                    if (testPerformedCode != null) {
                        // copy the filter and add a new one to also search on test performed code
                        val dualFilterCopy = filtersCopy.equalsIgnoreCase(
                            LivdTableColumns.TEST_PERFORMED_CODE.colName,
                            testPerformedCode
                        )
                        lookupByEquipmentModelName(
                            element, it.value,
                            dualFilterCopy
                        )
                    } else {
                        // the default state, just search by model name
                        lookupByEquipmentModelName(
                            element, it.value,
                            filtersCopy
                        )
                    }
                }
                else -> null
            }
            if (result != null) return ElementResult(result)
        }
        return ElementResult(null).also {
            // Hide any warnings to fields the user does not send to us
            if (!element.csvFields.isNullOrEmpty() || !element.hl7Field.isNullOrBlank() ||
                !element.hl7OutputFields.isNullOrEmpty()
            )
                it.warning(InvalidEquipmentMessage.new(element))
        }
    }

    companion object {
        private val standard99ELRTypes = listOf("EUA", "DII", "DIT", "DIM", "MNT", "MNI", "MNM")

        private const val testProcessingModeCode = "T"

        /**
         * Does a lookup in the LIVD table based on the element Id
         * @param element the schema element to use for lookups
         * @param deviceId the ID of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        private fun lookupByDeviceId(
            element: Element,
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
                return lookup(element, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                    ?: lookup(element, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
            }

            // truncated 99ELR type
            if (deviceId.endsWith("#")) {
                val value = deviceId.substringBeforeLast('#', "")
                return lookupPrefix(element, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                    ?: lookupPrefix(element, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
            }

            // May be the DI from a GUDID either test-kit or equipment
            return lookup(element, deviceId, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                ?: lookup(element, deviceId, LivdTableColumns.EQUIPMENT_UID.colName, filters)
        }

        /**
         * Does a lookup in the LIVD table based on the element unique identifier
         * @param element the schema element to use for lookups
         * @param value the unique ID of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        private fun lookupByEquipmentUid(
            element: Element,
            value: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            if (value.isBlank()) return null
            return lookup(element, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
        }

        /**
         * Does a lookup in the LIVD table based on the test kit Id
         * @param element the schema element to use for lookups
         * @param value the test kit ID of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        private fun lookupByTestkitId(
            element: Element,
            value: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            if (value.isBlank()) return null
            return lookup(element, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
        }

        /**
         * Does a lookup in the LIVD table based on the equipment model name
         * @param element the schema element to use for lookups
         * @param value the model name of the test device to lookup LIVD information by
         * @param filters an optional list of additional filters to limit our search by
         * @return a possible String? value based on the lookup
         */
        internal fun lookupByEquipmentModelName(
            element: Element,
            value: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            if (value.isBlank()) return null
            return lookup(
                element, LivdLookupUtilities.getCleanedModelName(value), LivdTableColumns.MODEL.colName,
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
            element: Element,
            lookup: String,
            onColumn: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            val lookupColumn = element.tableColumn
                ?: error("Schema Error: no tableColumn for element '${element.name}'")
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
            element: Element,
            lookup: String,
            onColumn: String,
            filters: LookupTable.FilterBuilder
        ): String? {
            val lookupColumn = element.tableColumn
                ?: error("Schema Error: no tableColumn for element '${element.name}'")
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