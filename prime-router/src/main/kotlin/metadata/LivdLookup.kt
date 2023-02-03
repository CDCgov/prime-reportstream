package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.Metadata

// this will be used by the LivdMappers and the CustomFHIRFunction
// /need to move most of LivdMappers in here
// will need to move the tests to look at this as well
// keep the library the way it was then use the context to use the library to pass in custom functions
object LivdLookup {
    fun find(
        testPerformedCode: String?,
        processingModeCode: String?,
        deviceId: String?,
        equipmentModelId: String?,
        testKitNameId: String?,
        equipmentModelName: String?,
        tableColumn: String,
        tableRef: LookupTable? = Metadata.getInstance().findLookupTable(name = "LIVD-SARS-CoV-2")
    ): String? {
        if (tableRef == null) {
            return null
        }

        val filters = tableRef.FilterBuilder()
        // get the test performed code for additional filtering of the test information in case we are
        // dealing with tests that check for more than one type of disease, for example COVID + influenza
        if (testPerformedCode != null) {
            filters.equalsIgnoreCase(LivdTableColumns.TEST_PERFORMED_CODE.colName, testPerformedCode)
        }

        // If the data is NOT flagged as test data then ignore any test devices in the LIVD table
        if (processingModeCode == null ||
            processingModeCode.uppercase() != testProcessingModeCode
        ) {
            filters.notEqualsIgnoreCase(
                LivdTableColumns.PROCESSING_MODE_CODE.colName,
                testProcessingModeCode
            )
        }

        // carry on as usual
        val filtersCopy = filters.copy() // Filters are not reusable
        return when {
            !deviceId.isNullOrEmpty() -> lookupByDeviceId(
                tableColumn,
                deviceId,
                filtersCopy
            )
            !equipmentModelId.isNullOrEmpty() -> lookupByEquipmentUid(
                tableColumn,
                equipmentModelId,
                filtersCopy
            )
            !testKitNameId.isNullOrEmpty() -> lookupByTestkitId(
                tableColumn,
                testKitNameId,
                filtersCopy
            )
            !equipmentModelName.isNullOrEmpty() -> lookupByEquipmentModelName(
                tableColumn, equipmentModelName, filtersCopy
            )

            else -> null
        }
    }

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
    private fun lookupByDeviceId(
        tableColumn: String,
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
            return lookup(tableColumn, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                ?: lookup(tableColumn, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
        }

        // truncated 99ELR type
        if (deviceId.endsWith("#")) {
            val value = deviceId.substringBeforeLast('#', "")
            return lookupPrefix(tableColumn, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
                ?: lookupPrefix(tableColumn, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
        }

        // May be the DI from a GUDID either test-kit or equipment
        return lookup(tableColumn, deviceId, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
            ?: lookup(tableColumn, deviceId, LivdTableColumns.EQUIPMENT_UID.colName, filters)
    }

    /**
     * Does a lookup in the LIVD table based on the element unique identifier
     * @param element the schema element to use for lookups
     * @param value the unique ID of the test device to lookup LIVD information by
     * @param filters an optional list of additional filters to limit our search by
     * @return a possible String? value based on the lookup
     */
    private fun lookupByEquipmentUid(
        tableColumn: String,
        value: String,
        filters: LookupTable.FilterBuilder
    ): String? {
        return lookup(tableColumn, value, LivdTableColumns.EQUIPMENT_UID.colName, filters)
    }

    /**
     * Does a lookup in the LIVD table based on the test kit Id
     * @param element the schema element to use for lookups
     * @param value the test kit ID of the test device to lookup LIVD information by
     * @param filters an optional list of additional filters to limit our search by
     * @return a possible String? value based on the lookup
     */
    private fun lookupByTestkitId(
        tableColumn: String,
        value: String,
        filters: LookupTable.FilterBuilder
    ): String? {
        if (value.isBlank()) return null
        return lookup(tableColumn, value, LivdTableColumns.TESTKIT_NAME_ID.colName, filters)
    }

    /**
     * Does a lookup in the LIVD table based on the equipment model name
     * @param element the schema element to use for lookups
     * @param value the model name of the test device to lookup LIVD information by
     * @param filters an optional list of additional filters to limit our search by
     * @return a possible String? value based on the lookup
     */
    internal fun lookupByEquipmentModelName(
        tableColumn: String,
        value: String,
        filters: LookupTable.FilterBuilder
    ): String? {
        if (value.isBlank()) return null
        return lookup(
            tableColumn,
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
        lookupColumn: String,
        lookup: String,
        onColumn: String,
        filters: LookupTable.FilterBuilder
    ): String? {
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
        lookupColumn: String,
        lookup: String,
        onColumn: String,
        filters: LookupTable.FilterBuilder
    ): String? {
        return filters.startsWithIgnoreCase(onColumn, lookup).findSingleResult(lookupColumn)
    }
}