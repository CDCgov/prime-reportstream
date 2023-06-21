package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.Metadata

object LivdLookup {
    val livdTableName = "LIVD-SARS-CoV-2"
    /**
     * Will find a value in the LIVD table based on the test kit name id, equipment model name, equipment model id, or
     * the device id
     * @param testPerformedCode used for additional filtering of the test info in case we are dealing with tests that
     * check for more than on type of disease
     * @param processingModeCode will flag the data as test data which will make it ignore any test devices
     * @param deviceId the ID of the test device to lookup LIVD information by
     * @param equipmentModelId the ID of the equipment model to lookup LIVD information by
     * @param testKitNameId the ID of the test kit to lookup LIVD information by
     * @param equipmentModelName the name of the equipment model to lookup LIVD information by
     * @param tableColumn the name of the table column to lookup LIVD information on
     * @param tableRef allows us to pass in the reference to the table, just used for testing purposes
     * @return a possible String? value based on the lookup
     */
    fun find(
        testPerformedCode: String? = null,
        processingModeCode: String? = null,
        deviceId: String? = null,
        equipmentModelId: String? = null,
        testKitNameId: String? = null,
        equipmentModelName: String? = null,
        tableColumn: String,
        tableRef: LookupTable? = Metadata.getInstance().findLookupTable(name = livdTableName)
    ): String? {
        val filters = tableRef?.FilterBuilder() ?: error("Could not find table '$tableRef'\"")
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
     * @param tableColumn the column to lookup the value in
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
     * @param tableColumn the column to lookup the value in
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
     * @param tableColumn the column to lookup the value in
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
     * @param tableColumn the column to lookup the value in
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
     * @param lookupColumn the column to filter the results on
     * @param lookup the value to search the index column for
     * @param onColumn the name of the index column to do the lookup in
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
     * @param lookupColumn the column to filter the results on
     * @param lookup the value to search the index column for
     * @param onColumn the name of the index column to do the lookup in
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