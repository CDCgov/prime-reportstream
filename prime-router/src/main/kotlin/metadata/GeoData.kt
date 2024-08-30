package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.Metadata
import java.util.Random

object GeoData {
    val geoTableName = "geo-data"

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
    fun pickRandomLocationInState(
        state: String,
        column: ColumnNames,
        tableRef: LookupTable? = Metadata.getInstance().findLookupTable(name = geoTableName),
    ): String? {
        var filters = tableRef?.FilterBuilder() ?: error("Could not find table '$tableRef'\"")
        filters = filters.equalsIgnoreCase(ColumnNames.STATE_ABBR.columnName, state)
        val uniqueValues = filters.findAllUnique(column.columnName)
        if (uniqueValues.isEmpty()) {
            // bad data passed, just return default
            return when (column) {
                ColumnNames.STATE_FIPS -> "12345"
                ColumnNames.STATE -> state
                ColumnNames.STATE_ABBR -> state
                ColumnNames.ZIP_CODE -> "98765"
                ColumnNames.COUNTY -> "Multnomah"
                ColumnNames.CITY -> "Portland"
            }
        }
        return uniqueValues[Random().nextInt(uniqueValues.size)]
    }

    /**
     * Column Names
     */
    enum class ColumnNames(val columnName: String) {
        STATE_FIPS("state_fips"),
        STATE("state"),
        STATE_ABBR("state_abbr"),
        ZIP_CODE("zipcode"),
        COUNTY("county"),
        CITY("city"),
    }

    enum class DataTypes(val dataType: String) {
        CITY("city"),
        POSTAL_CODE("postal_code"),
        TESTING_LAB("testing_lab"),
        UUID("sender_identifier"),
        FACILITY_NAME("facility_name"),
        NAME_OF_SCHOOL("name_of_school"),
        REFERENCE_RANGE("reference_range"),
        RESULT_FORMAT("result_format"),
        PATIENT_PREFERRED_LANGUAGE("patient_preferred_language"),
        PATIENT_COUNTRY("patient_country"),
        SITE_OF_CARE("site_of_care"),
        PATIENT_AGE_AND_UNITS("patient_age_and_units"),
        COUNTY("county"),
        EQUIPMENT_MODEL_NAME("equipment_model_name"),
        TEST_PERFORMED_CODE("test_performed_code"),
        OTHER_TEXT("other_text"),
        BLANK("blank"),
        TEXT_OR_BLANK("text_or_blank"),
        NUMBER("number"),
        DATE("date"),
        DATETIME("date_time"),
        SPECIMEN_SOURCE_SITE_CODE("specimen_source_code"),
        TEST_RESULT_STATUS("test_result_status"),
        PROCESSING_MODE_CODE("processing_mode_code"),
        VALUE_TYPE("value_type"),
        TEST_RESULT("test_result"),
        HD("hd"),
        EI("ei"),
        ID("id"),
        ID_CLIA("id_clia"),
        ID_DLN("id_dln"),
        ID_SSN("id_ssn"),
        ID_NPI("id_npi"),
        STREET("street"),
        PERSON_GIVEN_NAME("person_given_name"),
        PERSON_FAMILY_NAME("person_family_name"),
        TELEPHONE("telephone"),
        EMAIL("email"),
        BIRTHDAY("birthday"),
        ID_NUMBER("id_number"),
        STREET_ADDRESS_2("patient_street_address_2"),
        SOURCE_OF_COMMENT("source_of_comment"),
    }
}